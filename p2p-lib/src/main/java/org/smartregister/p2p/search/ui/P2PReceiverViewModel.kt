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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.R
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.DefaultDispatcherProvider
import org.smartregister.p2p.utils.DispatcherProvider
import org.smartregister.p2p.utils.divideToPercent
import timber.log.Timber

class P2PReceiverViewModel(
  private val view: P2PDeviceSearchActivity,
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : ViewModel(), P2pModeSelectContract.ReceiverViewModel {

  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private var sendingDeviceAppLifetimeKey: String = ""

  fun processSenderDeviceDetails() {

    dataSharingStrategy.receive(
      dataSharingStrategy.getCurrentDevice(),
      object : DataSharingStrategy.PayloadReceiptListener {
        override fun onPayloadReceived(payload: PayloadContract<out Any>?) {

          var map: MutableMap<String, String?> = HashMap()
          val deviceDetails = Gson().fromJson((payload as StringPayload).string, map.javaClass)

          if (deviceDetails != null &&
              deviceDetails.containsKey(Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY)
          ) {
            checkIfDeviceKeyHasChanged(
              deviceDetails[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY]!!
            )

            viewModelScope.launch {
              withContext(dispatcherProvider.main()) { view.showTransferProgressDialog() }
            }
          } else {
            Timber.e("An error occurred and the APP-LIFETIME-KEY was not sent")
          }
        }
      },
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {}

        override fun onFailure(device: DeviceInfo?, ex: Exception) {}
      }
    )
  }

  fun checkIfDeviceKeyHasChanged(appLifetimeKey: String) {
    Timber.e("In check if device key has changed with app lifetime key $appLifetimeKey")
    viewModelScope.launch(dispatcherProvider.io()) {
      val receivedHistory = getReceivedHistory(appLifetimeKey = appLifetimeKey)

      sendingDeviceAppLifetimeKey = appLifetimeKey
      if (receivedHistory != null && receivedHistory.isNotEmpty()) {
        sendLastReceivedRecords(receivedHistory)
      } else {
        sendLastReceivedRecords(emptyList())
      }
    }
  }

  fun getReceivedHistory(appLifetimeKey: String): List<P2PReceivedHistory?>? {
    return P2PLibrary.getInstance()
      .getDb()
      .p2pReceivedHistoryDao()
      .getDeviceReceivedHistory(appLifetimeKey)
  }

  override fun sendLastReceivedRecords(receivedHistory: List<P2PReceivedHistory?>?) {

    val deviceInfo: MutableMap<String, String?> = HashMap()
    deviceInfo[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY] =
      P2PLibrary.getInstance().getHashKey()
    deviceInfo[Constants.BasicDeviceDetails.KEY_DEVICE_ID] =
      P2PLibrary.getInstance().getDeviceUniqueIdentifier()

    syncReceiverHandler = SyncReceiverHandler(this, DefaultDispatcherProvider())

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
          val receivedManifest =
            dataSharingStrategy.receiveManifest(
              device = dataSharingStrategy.getCurrentDevice()!!,
              object : DataSharingStrategy.OperationListener {
                override fun onSuccess(device: DeviceInfo?) {
                  Timber.i("Manifest successfully received")
                }

                override fun onFailure(device: DeviceInfo?, ex: Exception) {
                  Timber.e(ex, "Failed to receive manifest")
                }
              }
            )

          // Handle successfully received manifest
          if (receivedManifest != null) {
            Timber.i("Manifest with data successfully received")
            syncReceiverHandler.processManifest(receivedManifest)
          }
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e(ex, "Failed to send the last received records")
        }
      }
    )
  }

  fun processChunkData() {
    Timber.e("Listen for incoming chunk data")
    // Listen for incoming chunk data
    dataSharingStrategy.receive(
      dataSharingStrategy.getCurrentDevice(),
      object : DataSharingStrategy.PayloadReceiptListener {
        override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
          // Process chunk data
          val jsonString = String((payload as BytePayload).getData())
          Timber.e(jsonString)
          val chunkData = JSONArray(jsonString)
          viewModelScope.launch(dispatcherProvider.io()) {
            syncReceiverHandler.processData(chunkData)
          }
        }
      },
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.i("Successfully received chunk data")
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e(ex, "Failed to receive chunk data")
        }
      }
    )
  }

  fun processIncomingManifest() {
    val incomingManifest = listenForIncomingManifest()

    // Handle successfully received manifest
    if (incomingManifest != null && incomingManifest.dataType.name == Constants.SYNC_COMPLETE) {
      handleDataTransferCompleteManifest()
    } else {
      syncReceiverHandler.processManifest(incomingManifest!!)
    }
  }

  fun handleDataTransferCompleteManifest() {
    Timber.e("Data transfer complete")
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) { view.showTransferCompleteDialog() }
      dataSharingStrategy.disconnect(
        dataSharingStrategy.getCurrentDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.i("Diconnection successful")
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex, "P2P diconnection failed")
          }
        }
      )
    }
  }

  fun listenForIncomingManifest(): Manifest? {
    // Listen for incoming manifest
    val receivedManifest =
      dataSharingStrategy.receiveManifest(
        device = dataSharingStrategy.getCurrentDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.i("Manifest successfully received")
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex, "Failed to receive manifest")
          }
        }
      )
    return receivedManifest
  }

  override fun getSendingDeviceAppLifetimeKey(): String {
    return sendingDeviceAppLifetimeKey
  }

  override fun updateTransferProgress(totalReceivedRecords: Long, totalRecords: Long) {
    var percentageReceived = totalReceivedRecords.divideToPercent(totalRecords)
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) {
        view.updateTransferProgress(
          resStringId = R.string.receiving_x_records,
          percentageTransferred = percentageReceived,
          totalRecords = totalRecords
        )
      }
    }
  }

  class Factory(
    private val context: P2PDeviceSearchActivity,
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PReceiverViewModel(context, dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
