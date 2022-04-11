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
import androidx.lifecycle.ViewModel
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.P2pReceivedHistoryDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.search.ui.P2PReceiverViewModel

class SyncReceiverHandler
constructor(
    @NonNull val p2PReceiverViewModel: P2PReceiverViewModel
) : ViewModel() {

    private val awaitingManifestReceipt = true
    private val awaitingPayloadManifests: HashMap<Long, Manifest> =
        HashMap<Long, Manifest>()

    fun processManifest(manifest: Manifest) {
        // update UI with number of records to expect
    }

    fun updateLastRecord(@NonNull entityType: String , lastUpdatedAt: Long ) {
        // Retrieve sending device details
        val sendingDeviceId = p2PReceiverViewModel.getSendingDeviceId()

        if(sendingDeviceId.isNotBlank()) {
            val p2pReceivedHistoryDao: P2pReceivedHistoryDao? = P2PLibrary.getInstance()!!.getDb()
                ?.p2pReceivedHistoryDao()

            var receivedHistory: P2PReceivedHistory? = p2pReceivedHistoryDao
                ?.getHistory(sendingDeviceId, entityType)

            if (receivedHistory == null) {
                receivedHistory = P2PReceivedHistory()
                receivedHistory.lastUpdatedAt = lastUpdatedAt
                receivedHistory.entityType = entityType
                receivedHistory.appLifetimeKey = sendingDeviceId

            } else {
                receivedHistory.lastUpdatedAt = lastUpdatedAt
                p2pReceivedHistoryDao?.updateReceivedHistory(receivedHistory)
            }
        }

    }

}
