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

import androidx.annotation.NonNull
import androidx.annotation.Nullable
import java.util.TreeSet
import kotlin.collections.HashMap
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.search.ui.P2PSenderViewModel
import org.smartregister.p2p.sync.DataType

class SyncSenderHandler
constructor(
  @NonNull val p2PSenderViewModel: P2PSenderViewModel,
  @NonNull val dataSyncOrder: TreeSet<DataType>,
  @Nullable val receivedHistory: List<P2PReceivedHistory>
) {
  private val remainingLastRecordIds = HashMap<String, Long>()
  private val batchSize = 0
  private var awaitingDataTypeName: String? = null
  private var awaitingDataTypeHighestId: Long = 0
  private var awaitingDataTypeRecordsBatchSize = 0

  private var awaitingManifestTransfer = false

  fun startSyncProcess() {
    generateRecordsToSend()
    sendNextManifest()
  }

  private fun generateRecordsToSend() {
    for (dataType in dataSyncOrder) {
      remainingLastRecordIds[dataType.name] = 0L
    }

    if (receivedHistory != null && receivedHistory.size > 0) {
      for (dataTypeHistory in receivedHistory) {
        remainingLastRecordIds[dataTypeHistory.entityType!!] = dataTypeHistory.lastUpdatedAt
      }
    }
  }

  fun sendNextManifest() {
    if (!dataSyncOrder.isEmpty()) {
      sendJsonDataManifest(dataSyncOrder.first())
    } else {
      // sendSyncComplete()
    }
  }

  fun sendJsonDataManifest(@NonNull dataType: DataType) {
    val nullableRecordId = remainingLastRecordIds[dataType.name]
    val lastRecordId = nullableRecordId ?: 0L
    // TODO run this is background
    val jsonData = JsonData()
      P2PLibrary.getInstance().getSenderTransferDao()
        ?.getJsonData(dataType, lastRecordId, batchSize)!!

    // send actual manifest

    if (jsonData != null) {
      val recordsArray = jsonData.getJsonArray()

      // TODO: Check if I should remove this
      remainingLastRecordIds[dataType.name] = jsonData.getHighestRecordId()

      val recordsJsonString = recordsArray.toString()
      awaitingDataTypeName = dataType.name
      awaitingDataTypeHighestId = jsonData.getHighestRecordId()
      awaitingDataTypeRecordsBatchSize = recordsArray!!.length()

      if (recordsJsonString.isNotBlank()) {

        val manifest =
          Manifest(
            dataType = dataType,
            recordsSize = awaitingDataTypeRecordsBatchSize,
            payloadSize = recordsJsonString.length
          )

        awaitingManifestTransfer = true

        p2PSenderViewModel.sendManifest(manifest = manifest)
      } else {
        dataSyncOrder.remove(dataType)
        sendNextManifest()
      }
    }
  }
}
