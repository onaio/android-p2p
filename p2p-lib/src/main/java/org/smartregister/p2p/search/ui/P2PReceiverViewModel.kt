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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.IReceiverSyncLifecycleCallback
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.BytePayload
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
  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private var sendingDeviceAppLifetimeKey: String = ""

  fun processSenderDeviceDetails() {

    dataSharingStrategy.receive(
      dataSharingStrategy.getCurrentDevice(),
      object: DataSharingStrategy.PayloadReceiptListener {
        override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
         // Timber.e("Payload received : ${(payload as StringPayload).string}")

          var map: MutableMap<String, String?> = HashMap()
          val deviceDetails = Gson().fromJson((payload as StringPayload).string, map.javaClass)

          // TODO: Fix possible crash here due to NPE
          checkIfDeviceKeyHasChanged(
            deviceDetails[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY]!!
          )

          GlobalScope.launch {
            withContext(Dispatchers.Main) {
              context.showTransferProgressDialog()
            }
          }
        }
      },
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {}

        override fun onFailure(device: DeviceInfo?, ex: Exception) {}
      }
    )
  }

  fun processSyncParamsRequest() {

    dataSharingStrategy.receive(
      context.getCurrentConnectedDevice(),
      object : DataSharingStrategy.PayloadReceiptListener {
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

      sendingDeviceAppLifetimeKey = appLifetimeKey
      if (receivedHistory != null && !receivedHistory.isEmpty()) {
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

    syncReceiverHandler = SyncReceiverHandler(this)

    dataSharingStrategy.send(
      device = dataSharingStrategy.getCurrentDevice(),
      syncPayload =
        StringPayload(
          Gson().toJson(receivedHistory),
        ),
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.e("Successfully sent the last received records")
          // Listen for incoming manifest
          val receivedManifest = dataSharingStrategy.receiveManifest(device = dataSharingStrategy.getCurrentDevice()!!,
            object: DataSharingStrategy.OperationListener{
              override fun onSuccess(device: DeviceInfo?) {
                Timber.e("Manifest successfully received")

              }

              override fun onFailure(device: DeviceInfo?, ex: Exception) {
                Timber.e("Failed to receive manifest")
              }

            }
          )

          // Handle successfully received manifest
          if (receivedManifest != null) {
            Timber.e("Manifest with data successfully received")
            syncReceiverHandler.processManifest(receivedManifest)
          }
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e("Failed to send the last received records")
        }
      }
    )
  }

  fun processChunkData() {
    Timber.e("Listen for incoming chunk data")
    // Listen for incoming chunk data
    dataSharingStrategy.receive(dataSharingStrategy.getCurrentDevice(), object: DataSharingStrategy.PayloadReceiptListener{
      override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
        Timber.e("Successfully received chunk data")
        // Process chunk data
        val jsonString = String((payload as BytePayload).getData())
        Timber.e("Received data!!!")
        Timber.e(jsonString)
        val chunkData = JSONArray(jsonString)
        syncReceiverHandler.processData(chunkData)
      }

    }, object: DataSharingStrategy.OperationListener{
      override fun onSuccess(device: DeviceInfo?) {
        //TODO Handle successful receipt of chunk data
      }

      override fun onFailure(device: DeviceInfo?, ex: Exception) {
        //TODO handle failure to receive chunk data
      }

    })

  }

  fun processIncomingManifest() {
    val incomingManifest = listenForIncomingManifest()

    // Handle successfully received manifest
    if (incomingManifest != null && incomingManifest.dataType.name == Constants.SYNC_COMPLETE) {
      handleDataTransferCompleteManifest()
    } else {
      Timber.e("Subsequent manifest with data successfully received")
      syncReceiverHandler.processManifest(incomingManifest!!)
    }
  }

  fun handleDataTransferCompleteManifest() {
    Timber.e("Data transfer complete")
    GlobalScope.launch {
      withContext(Dispatchers.Main) {
        context.showTransferCompleteDialog()
      }
      dataSharingStrategy.disconnect(dataSharingStrategy.getCurrentDevice()!!,
        object: DataSharingStrategy.OperationListener{
          override fun onSuccess(device: DeviceInfo?) {
            Timber.e("Diconnection successful")
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e("Diconnection failed")
          }

        })
    }
  }

  fun listenForIncomingManifest(): Manifest? {
    Timber.e("Listening for subsequent manifests")
    // Listen for incoming manifest
    val receivedManifest = dataSharingStrategy.receiveManifest(device = dataSharingStrategy.getCurrentDevice()!!,
      object: DataSharingStrategy.OperationListener{
        override fun onSuccess(device: DeviceInfo?) {
          Timber.e("Manifest successfully received")

        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e("Failed to receive manifest")
        }

      }
    )
    return receivedManifest
  }

  override fun getSendingDeviceAppLifetimeKey(): String {
    return sendingDeviceAppLifetimeKey
  }

  override fun upDateProgress(msg: String, recordSize: Int) {
    //TODO Update UI with record size
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
