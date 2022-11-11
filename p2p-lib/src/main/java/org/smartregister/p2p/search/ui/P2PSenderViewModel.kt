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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.util.TreeSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncSenderHandler
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.DefaultDispatcherProvider
import org.smartregister.p2p.utils.DispatcherProvider
import org.smartregister.p2p.utils.divideToPercent
import timber.log.Timber

class P2PSenderViewModel(
  private val view: P2pModeSelectContract.View,
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : ViewModel(), P2pModeSelectContract.SenderViewModel {

  private var connectionLevel: Constants.ConnectionLevel? = null

  private lateinit var syncSenderHandler: SyncSenderHandler

  fun sendDeviceDetails(deviceInfo: DeviceInfo?) {
    // write a message to the socket requesting the receiver for acceptable data types
    // and their last update times which can be sent using a simple string command,
    // 'SEND_SYNC_PARAMS', and the **app_lifetime_key**

    val deviceDetailsMap: MutableMap<String, String?> = HashMap()
    deviceDetailsMap[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY] =
      P2PLibrary.getInstance().getHashKey()
    deviceDetailsMap[Constants.BasicDeviceDetails.KEY_DEVICE_ID] =
      P2PLibrary.getInstance().getDeviceUniqueIdentifier()

    dataSharingStrategy.send(
      device = dataSharingStrategy.getCurrentDevice(),
      syncPayload =
        StringPayload(
          Gson().toJson(deviceDetailsMap),
        ),
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.i("Successfully sent the device details map")

          dataSharingStrategy.receive(
            device,
            object : DataSharingStrategy.PayloadReceiptListener {

              override fun onPayloadReceived(payload: PayloadContract<out Any>?) {
                // WE are receiving the history

                Timber.i("I have received last history : ${(payload as StringPayload).string}")

                processReceivedHistory(payload)
              }
            },
            object : DataSharingStrategy.OperationListener {
              override fun onSuccess(device: DeviceInfo?) {}

              override fun onFailure(device: DeviceInfo?, ex: Exception) {}
            }
          )
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {}
      }
    )
  }

  override fun requestSyncParams(deviceInfo: DeviceInfo?) {
    // write a message to the socket requesting the receiver for acceptable data types
    // and their last update times which can be sent using a simple string command,
    // 'SEND_SYNC_PARAMS', and the **app_lifetime_key**

    dataSharingStrategy.send(
      device = dataSharingStrategy.getCurrentDevice()
      /** Find out how to get this */
      ,
      syncPayload =
        StringPayload(
          Gson().toJson(Constants.SEND_SYNC_PARAMS),
        ),
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.i("Send sync params request sent successfully")
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e(ex)
        }
      }
    )
  }

  override fun sendSyncComplete() {
    Timber.i("P2P sync complete")
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) { view.showTransferCompleteDialog() }
      dataSharingStrategy.disconnect(
        getCurrentConnectedDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.i("Diconnection successful")
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.i("Diconnection failed")
            Timber.e(ex.message)
          }
        }
      )
    }
  }

  override fun sendChunkData(awaitingPayload: PayloadContract<out Any>) {
    Timber.i("Send chunk data")
    dataSharingStrategy.send(
      device = getCurrentConnectedDevice(),
      syncPayload = awaitingPayload,
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          Timber.i("Chunk data sent successfully")
          syncSenderHandler.updateTotalSentRecordCount()
          viewModelScope.launch(dispatcherProvider.io()) { syncSenderHandler.sendNextManifest() }
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.i("Failed to send chunk data")
        }
      }
    )
  }

  override fun sendManifest(manifest: Manifest) {
    if (getCurrentConnectedDevice() != null) {
      dataSharingStrategy.sendManifest(
        device = getCurrentConnectedDevice(),
        manifest = manifest,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.i(
              "Manifest sent successfully  ${manifest.dataType.name },  ${manifest.dataType.type}, ${manifest.payloadSize}, ${manifest.recordsSize}"
            )
            // Start sending the actual data
            syncSenderHandler.processManifestSent()
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex, "manifest failed to send")
          }
        }
      )
    }
  }

  override fun getCurrentConnectedDevice(): DeviceInfo? {
    return view.getCurrentConnectedDevice()
  }

  override fun processReceivedHistory(syncPayload: StringPayload) {
    connectionLevel = Constants.ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY

    // Handle non empty payload
    val receivedHistoryListType = object : TypeToken<List<P2PReceivedHistory?>?>() {}.type
    val receivedHistory: List<P2PReceivedHistory> =
      Gson().fromJson(syncPayload.string, receivedHistoryListType)

    var dataTypes = P2PLibrary.getInstance().getSenderTransferDao().getP2PDataTypes()
    syncSenderHandler = createSyncSenderHandler(dataTypes, receivedHistory)

    if (!dataTypes.isEmpty()) {
      Timber.i("Process received history json data not null")
      viewModelScope.launch(dispatcherProvider.io()) { syncSenderHandler.startSyncProcess() }
    } else {
      Timber.i("Process received history json data null")
      sendSyncComplete()
    }
  }

  @VisibleForTesting()
  internal fun createSyncSenderHandler(
    dataTypes: TreeSet<DataType>,
    receivedHistory: List<P2PReceivedHistory>
  ) =
    SyncSenderHandler(
      p2PSenderViewModel = this,
      dataSyncOrder = dataTypes,
      receivedHistory = receivedHistory,
      dispatcherProvider = DefaultDispatcherProvider()
    )

  fun updateSenderSyncComplete(senderSyncComplete: Boolean) {
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) { view.senderSyncComplete(senderSyncComplete) }
    }
  }

  override fun updateTransferProgress(totalSentRecords: Long, totalRecords: Long) {
    var percentageSent = totalSentRecords.divideToPercent(totalRecords)
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) {
        view.updateTransferProgress(
          TransferProgress(
            totalRecordCount = totalRecords,
            transferredRecordCount = totalSentRecords,
            percentageTransferred = percentageSent.toFloat()
          )
        )
      }
    }
  }

  class Factory(
    private val context: P2pModeSelectContract.View,
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PSenderViewModel(context, dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
