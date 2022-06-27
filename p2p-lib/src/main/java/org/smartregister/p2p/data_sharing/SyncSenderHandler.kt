/*
 * Copyright 2022 Ona Systems, Inc
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

import java.util.TreeSet
import kotlinx.coroutines.withContext
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.search.ui.P2PSenderViewModel
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

class SyncSenderHandler
constructor(
  val p2PSenderViewModel: P2PSenderViewModel,
  val dataSyncOrder: TreeSet<DataType>,
  val receivedHistory: List<P2PReceivedHistory>,
  private val dispatcherProvider: DispatcherProvider
) {
  private val remainingLastRecordIds = HashMap<String, Long>()
  private val batchSize = 25
  private var awaitingDataTypeRecordsBatchSize = 0
  private var totalRecordCount:Long = 0
  private var totalSentRecordCount:Long = 0

  private lateinit var awaitingPayload: PayloadContract<out Any>
  private var sendingSyncCompleteManifest = false

  suspend fun startSyncProcess() {
    Timber.i("Start sync process")
    generateRecordsToSend()
    sendNextManifest()
  }

  fun generateRecordsToSend() {
    for (dataType in dataSyncOrder) {
      remainingLastRecordIds[dataType.name] = 0L
    }

    if (receivedHistory.isNotEmpty()) {
      for (dataTypeHistory in receivedHistory) {
        if (dataTypeHistory.lastUpdatedAt == 0L) {
          continue
        }
        remainingLastRecordIds[dataTypeHistory.entityType!!] = dataTypeHistory.lastUpdatedAt
      }
    }
  }

  suspend fun sendNextManifest() {
    Timber.i("in send next manifest")
    if (!dataSyncOrder.isEmpty()) {
      sendJsonDataManifest(dataSyncOrder.first())
    } else {
      val manifest =
        Manifest(
          dataType = DataType(Constants.SYNC_COMPLETE, DataType.Filetype.JSON, 0),
          recordsSize = 0,
          payloadSize = 0
        )

      sendingSyncCompleteManifest = true
      p2PSenderViewModel.sendManifest(manifest = manifest)
      p2PSenderViewModel.updateSenderSyncComplete(senderSyncComplete = true)
    }
  }

  suspend fun sendJsonDataManifest(dataType: DataType) {
    Timber.i("Sending json manifest")
    val nullableRecordId = remainingLastRecordIds[dataType.name]
    val lastRecordId = nullableRecordId ?: 0L

    withContext(dispatcherProvider.io()) {
      val jsonData =
        P2PLibrary.getInstance()
          .getSenderTransferDao()
          .getJsonData(dataType, lastRecordId, batchSize)!!

      // send actual manifest

      if (jsonData != null && (jsonData.getJsonArray()?.length()!! > 0)) {
        Timber.i("Json data is has content")
        val recordsArray = jsonData.getJsonArray()

        remainingLastRecordIds[dataType.name] = jsonData.getHighestRecordId()
        Timber.i("remaining records last updated is ${remainingLastRecordIds[dataType.name]}")

        val recordsJsonString = recordsArray.toString()
        awaitingDataTypeRecordsBatchSize = recordsArray!!.length()
        awaitingPayload =
          BytePayload(
            recordsArray.toString().toByteArray(),
          )

        if (recordsJsonString.isNotBlank()) {

          val manifest =
            Manifest(
              dataType = dataType,
              recordsSize = awaitingDataTypeRecordsBatchSize,
              payloadSize = recordsJsonString.length,
              totalRecordCount = totalRecordCount
            )

          p2PSenderViewModel.sendManifest(manifest = manifest)
        }
      } else {
        // signifies all data has been sent
        Timber.i("Json data is null")
        dataSyncOrder.remove(dataType)
        sendNextManifest()
      }
    }
  }

  fun processManifestSent() {
    if (sendingSyncCompleteManifest) {
      sendingSyncCompleteManifest = false
    } else {
      p2PSenderViewModel.sendChunkData(awaitingPayload)
    }
  }

  open fun setTotalRecordCount(totalRecordCount: Long) {
    this.totalRecordCount = totalRecordCount
  }

  open fun updateTotalSentRecordCount() {
    this.totalSentRecordCount = totalSentRecordCount + awaitingDataTypeRecordsBatchSize
    p2PSenderViewModel.updateTransferProgress(recordsSent = totalSentRecordCount, totalRecords = this.totalRecordCount )
  }
}
