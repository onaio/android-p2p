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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.P2pReceivedHistoryDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.utils.Constants
import timber.log.Timber

class SyncReceiverHandler constructor(@NonNull val p2PReceiverViewModel: P2PReceiverViewModel) {

  private lateinit var currentManifest: Manifest

  fun processManifest(manifest: Manifest) {
    currentManifest = manifest
    // update UI with number of records to expect
    p2PReceiverViewModel.upDateProgress("Transferring %,d records", manifest.recordsSize)
    if (manifest.dataType.name == Constants.SYNC_COMPLETE) {
      p2PReceiverViewModel.handleDataTransferCompleteManifest()
    } else {
      p2PReceiverViewModel.processChunkData()
    }
  }

  suspend fun processData(data: JSONArray) {
    Timber.i("Processing chunk data")

    var lastUpdatedAt =
      P2PLibrary.getInstance().getReceiverTransferDao().receiveJson(currentManifest.dataType, data)

    addOrUpdateLastRecord(currentManifest.dataType.name, lastUpdatedAt = lastUpdatedAt)

    p2PReceiverViewModel.processIncomingManifest()
  }

  suspend fun addOrUpdateLastRecord(@NonNull entityType: String, lastUpdatedAt: Long) {
    // Retrieve sending device details
    val sendingDeviceAppLifetimeKey = p2PReceiverViewModel.getSendingDeviceAppLifetimeKey()

    withContext(Dispatchers.IO) {
      if (sendingDeviceAppLifetimeKey.isNotBlank()) {
        val p2pReceivedHistoryDao: P2pReceivedHistoryDao? = getP2pReceivedHistoryDao()

        var receivedHistory: P2PReceivedHistory? =
          p2pReceivedHistoryDao?.getHistory(sendingDeviceAppLifetimeKey, entityType)

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

  private fun getP2pReceivedHistoryDao(): P2pReceivedHistoryDao? {
    return P2PLibrary.getInstance()!!.getDb()?.p2pReceivedHistoryDao()
  }
}
