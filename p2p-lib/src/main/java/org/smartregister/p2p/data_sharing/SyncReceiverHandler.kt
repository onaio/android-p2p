/*
 * Copyright 2022-2023 Ona Systems, Inc
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.smartregister.p2p.data_sharing

import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.P2pReceivedHistoryDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

class SyncReceiverHandler
constructor(
  val p2PReceiverViewModel: P2PReceiverViewModel,
  private val dispatcherProvider: DispatcherProvider
) {

  private lateinit var currentManifest: Manifest
  private var totalRecordCount: Long = 0
  private var totalReceivedRecordCount: Long = 0
  private val receivedResourceCountMap: HashMap<String, Long> = HashMap()

  fun processManifest(manifest: Manifest) {
    currentManifest = manifest
    totalRecordCount =
      if (manifest.totalRecordCount > 0) manifest.totalRecordCount else totalRecordCount

    when (manifest.dataType.name) {
      Constants.SYNC_COMPLETE -> {
        p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
      }
      Constants.DATA_UP_TO_DATE -> {
        p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.DATA_UP_TO_DATE)
      }
      else -> {
        p2PReceiverViewModel.processChunkData()
      }
    }
  }

  suspend fun processData(data: JSONArray) {
    Timber.i("Processing chunk data")

    var lastUpdatedAt =
      P2PLibrary.getInstance().getReceiverTransferDao().receiveJson(currentManifest.dataType, data)

    totalReceivedRecordCount += data!!.length()
    Timber.e(
      "Progress update: Updating received data ${currentManifest.dataType.name} x ${currentManifest.recordsSize} | UPTO $lastUpdatedAt"
    )
    Timber.e(
      "Progress update: Record count vs JSONArray size | ${currentManifest.recordsSize} - ${data.length()}"
    )
    p2PReceiverViewModel.updateTransferProgress(
      totalReceivedRecords = totalReceivedRecordCount,
      totalRecords = totalRecordCount
    )

    /**
     * When all records have been received increment lastUpdatedAt by 1 This ensures that when a new
     * transfer is initiated the already transferred items with the highest lastUpdated value are
     * not sent again Fixes issue documented here
     * https://github.com/opensrp/fhircore/issues/2390#issuecomment-1726305575
     */
    if (transferCompletedForCurrentDataType(data)) {
      lastUpdatedAt += 1
      Timber.i(
        "Last updatedAt incremented by 1 to $lastUpdatedAt for ${currentManifest.dataType.name}"
      )
    } else lastUpdatedAt
    addOrUpdateLastRecord(currentManifest.dataType.name, lastUpdatedAt = lastUpdatedAt)

    p2PReceiverViewModel.processIncomingManifest()
  }

  suspend fun addOrUpdateLastRecord(entityType: String, lastUpdatedAt: Long) {
    // Retrieve sending device details
    val sendingDeviceAppLifetimeKey = p2PReceiverViewModel.getSendingDeviceAppLifetimeKey()

    withContext(dispatcherProvider.io()) {
      if (sendingDeviceAppLifetimeKey.isNotBlank()) {
        val p2pReceivedHistoryDao = getP2pReceivedHistoryDao()

        var receivedHistory: P2PReceivedHistory? =
          p2pReceivedHistoryDao.getHistory(sendingDeviceAppLifetimeKey, entityType)

        if (receivedHistory == null) {
          receivedHistory = P2PReceivedHistory()
          receivedHistory.lastUpdatedAt = lastUpdatedAt
          receivedHistory.entityType = entityType
          receivedHistory.appLifetimeKey = sendingDeviceAppLifetimeKey
          p2pReceivedHistoryDao?.addReceivedHistory(receivedHistory)
        } else {
          receivedHistory.lastUpdatedAt = lastUpdatedAt
          p2pReceivedHistoryDao?.updateReceivedHistory(receivedHistory)
        }
      }
    }
  }

  private fun getP2pReceivedHistoryDao(): P2pReceivedHistoryDao {
    return P2PLibrary.getInstance().getDb().p2pReceivedHistoryDao()
  }

  /**
   * This method determines whether all records of the current data type being processed have been
   * transferred
   * @param data [JSONArray] contains the batch of records being transferred
   * @return Boolean indicating whether all records for a particular data type have been transferred
   */
  private fun transferCompletedForCurrentDataType(data: JSONArray): Boolean {
    val currentDataTypeTotalRecordCount =
      currentManifest.recordCount.dataTypeTotalCountMap[currentManifest.dataType.name]

    receivedResourceCountMap[currentManifest.dataType.name] =
      if (receivedResourceCountMap[currentManifest.dataType.name] != null)
        receivedResourceCountMap[currentManifest.dataType.name]?.plus(data!!.length())!!
      else data!!.length().toLong()

    Timber.i(
      "totalReceivedRecordCount is ${receivedResourceCountMap[currentManifest.dataType.name]} and totalRecordCount is  $currentDataTypeTotalRecordCount"
    )

    return receivedResourceCountMap[currentManifest.dataType.name] ==
      currentDataTypeTotalRecordCount
  }
}
