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
package org.smartregister.p2p.search.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.IReceiverSyncLifecycleCallback
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.utils.Constants
import timber.log.Timber

class P2PReceiverViewModel(
  private val context: P2PDeviceSearchActivity,
  private val dataSharingStrategy: DataSharingStrategy
) : ViewModel(), IReceiverSyncLifecycleCallback, P2pModeSelectContract.ReceiverViewModel {

  private val connectionLevel: Constants.ConnectionLevel? = null


  fun processSenderDeviceDetails() {

    dataSharingStrategy.receive(
      context.getCurrentConnectedDevice(),
      object: DataSharingStrategy.PayloadReceiptListener {
        override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
          Timber.e("Payload received : ${(payload as StringPayload).string}")

          var map : MutableMap<String, String?> = HashMap()
          val deviceDetails = Gson().fromJson((payload as StringPayload).string, map.javaClass)

          // TODO: Fix possible crash here due to NPE
          checkIfDeviceKeyHasChanged(deviceDetails[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY]!!)
        }
      },
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {

        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {}
      }
    )
  }

  fun processSyncParamsRequest() {

      dataSharingStrategy.receive(
        context.getCurrentConnectedDevice(),
        object: DataSharingStrategy.PayloadReceiptListener {
          override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
            Timber.i("Payload received ")
            //
          }
        },
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            checkIfDeviceKeyHasChanged((device?.strategySpecificDevice as WifiP2pDevice).deviceName)
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {}
        }
      )
  }

  fun checkIfDeviceKeyHasChanged(appLifetimeKey: String) {
    Timber.e("In check if device key has changed with app lifetime key $appLifetimeKey")
    viewModelScope.launch(Dispatchers.IO) {
      val receivedHistory =
        P2PLibrary.getInstance()
          .getDb()
          ?.p2pReceivedHistoryDao()
          ?.getDeviceReceivedHistory(appLifetimeKey)

      if (receivedHistory != null) {
        sendLastReceivedRecords(receivedHistory)
        Timber.e("Sent received history")
      } else {
        sendLastReceivedRecords(listOf(P2PReceivedHistory()))
        Timber.e("Sent empty received history")
      }
    }
  }

  override fun sendLastReceivedRecords(receivedHistory: List<P2PReceivedHistory?>?) {

    val deviceInfo: MutableMap<String, String?> = HashMap()
    deviceInfo[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY] =
      P2PLibrary.getInstance().getHashKey()
    deviceInfo[Constants.BasicDeviceDetails.KEY_DEVICE_ID] =
      P2PLibrary.getInstance().getDeviceUniqueIdentifier()

    dataSharingStrategy.send(
      device = dataSharingStrategy.getCurrentDevice(),
      syncPayload =
        StringPayload(
          Gson().toJson(receivedHistory),
        ),
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.e("Successfully sent the last received records")
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e("Failed to send the last received records")
        }
      }
    )
  }

  override fun getSendingDeviceId(): String {
    // TODO implement this
    return ""
  }

  class Factory(
    private val context: P2PDeviceSearchActivity,
    private val dataSharingStrategy: DataSharingStrategy
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
      return P2PReceiverViewModel(context, dataSharingStrategy) as T
    }
  }
}
