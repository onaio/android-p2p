/*
 * License text copyright (c) 2020 MariaDB Corporation Ab, All Rights Reserved.
 * “Business Source License” is a trademark of MariaDB Corporation Ab.
 *
 * Parameters
 *
 * Licensor:             Ona Systems, Inc.
 * Licensed Work:        android-p2p. The Licensed Work is (c) 2023 Ona Systems, Inc.
 * Additional Use Grant: You may make production use of the Licensed Work,
 *                       provided such use does not include offering the Licensed Work
 *                       to third parties on a hosted or embedded basis which is
 *                       competitive with Ona Systems' products.
 * Change Date:          Four years from the date the Licensed Work is published.
 * Change License:       MPL 2.0
 *
 * For information about alternative licensing arrangements for the Licensed Work,
 * please contact licensing@ona.io.
 *
 * Notice
 *
 * Business Source License 1.1
 *
 * Terms
 *
 * The Licensor hereby grants you the right to copy, modify, create derivative
 * works, redistribute, and make non-production use of the Licensed Work. The
 * Licensor may make an Additional Use Grant, above, permitting limited production use.
 *
 * Effective on the Change Date, or the fourth anniversary of the first publicly
 * available distribution of a specific version of the Licensed Work under this
 * License, whichever comes first, the Licensor hereby grants you rights under
 * the terms of the Change License, and the rights granted in the paragraph
 * above terminate.
 *
 * If your use of the Licensed Work does not comply with the requirements
 * currently in effect as described in this License, you must purchase a
 * commercial license from the Licensor, its affiliated entities, or authorized
 * resellers, or you must refrain from using the Licensed Work.
 *
 * All copies of the original and modified Licensed Work, and derivative works
 * of the Licensed Work, are subject to this License. This License applies
 * separately for each version of the Licensed Work and the Change Date may vary
 * for each version of the Licensed Work released by Licensor.
 *
 * You must conspicuously display this License on each original or modified copy
 * of the Licensed Work. If you receive the Licensed Work in original or
 * modified form from a third party, the terms and conditions set forth in this
 * License apply to your use of that work.
 *
 * Any use of the Licensed Work in violation of this License will automatically
 * terminate your rights under this License for the current and all other
 * versions of the Licensed Work.
 *
 * This License does not grant you any right in any trademark or logo of
 * Licensor or its affiliates (provided that you may use a trademark or logo of
 * Licensor as expressly required by this License).
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE LICENSED WORK IS PROVIDED ON
 * AN “AS IS” BASIS. LICENSOR HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS,
 * EXPRESS OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND
 * TITLE.
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
