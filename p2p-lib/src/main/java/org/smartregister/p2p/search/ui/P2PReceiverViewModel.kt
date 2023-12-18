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
package org.smartregister.p2p.search.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import java.net.SocketException
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.TransferProgress
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
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : BaseViewModel(dataSharingStrategy), P2pModeSelectContract.ReceiverViewModel {

  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private var sendingDeviceAppLifetimeKey: String = ""

  fun processSenderDeviceDetails() {

    dataSharingStrategy.receive(
      device = dataSharingStrategy.getCurrentDevice(),
      payloadReceiptListener =
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
            } else {
              // Show error msg
              updateP2PState(P2PState.RECEIVE_BASIC_DEVICE_DETAILS_FAILED)
              Timber.e(Exception("An error occurred and the APP-LIFETIME-KEY was not sent"))
            }
          }
        },
      operationListener =
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {}

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex, "An exception occured when trying to create the socket")

            if (ex is SocketException) {
              handleSocketException()
            } else {
              disconnect()
            }
          }
        }
    )
  }

  fun checkIfDeviceKeyHasChanged(appLifetimeKey: String) {
    Timber.d("In check if device key has changed with app lifetime key $appLifetimeKey")
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

                  if (ex is SocketException) {
                    handleSocketException()
                  } else {
                    disconnect()
                  }
                }
              }
            )

          // Handle successfully received manifest
          if (receivedManifest != null) {
            Timber.i("Manifest with data successfully received")
            // notify UI data transfer is starting
            postUIAction(UIAction.NOTIFY_DATA_TRANSFER_STARTING, DeviceRole.RECEIVER)
            syncReceiverHandler.processManifest(receivedManifest)
          }
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e(ex, "Failed to send the last received records")

          if (ex is SocketException) {
            handleSocketException()
          } else {
            disconnect()
          }
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

          // Reset and restart the page
          if (ex is SocketException) {
            handleSocketException()
          } else {
            disconnect()
          }
        }
      }
    )
  }

  fun processIncomingManifest() {
    val incomingManifest = listenForIncomingManifest()

    // Handle successfully received manifest
    if (incomingManifest != null) {
      if (incomingManifest.dataType.name == Constants.SYNC_COMPLETE) {
        handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
      } else {
        syncReceiverHandler.processManifest(incomingManifest!!)
      }
    } else {
      showToast(R.string.an_error_occurred_restarting_the_p2p_screen)
      restartActivity()
    }
  }

  fun handleDataTransferCompleteManifest(p2PState: P2PState) {
    Timber.e("Data transfer complete")
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) {
        postUIAction(UIAction.SHOW_TRANSFER_COMPLETE_DIALOG, p2PState)
      }
      dataSharingStrategy.disconnect(
        dataSharingStrategy.getCurrentDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.i("Disconnection successful")
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

            if (ex is SocketException) {
              handleSocketException()
            } else {
              disconnect()
            }
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
        postUIAction(
          UIAction.UPDATE_TRANSFER_PROGRESS,
          TransferProgress(
            totalRecordCount = totalRecords,
            transferredRecordCount = totalReceivedRecords,
            percentageTransferred = percentageReceived
          )
        )
      }
    }
  }

  fun showCancelTransferDialog() {
    viewModelScope.launch(dispatcherProvider.main()) {
      postUIAction(UIAction.SHOW_CANCEL_TRANSFER_DIALOG)
    }
  }

  class Factory(
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PReceiverViewModel(dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
