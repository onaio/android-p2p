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

import androidx.annotation.VisibleForTesting
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.net.SocketException
import java.util.TreeSet
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.authentication.model.DeviceRole
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
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : BaseViewModel(dataSharingStrategy), P2pModeSelectContract.SenderViewModel {

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

              override fun onFailure(device: DeviceInfo?, ex: Exception) {
                Timber.e("An error occured trying to receive last history", ex)
                if (ex is SocketException) {
                  handleSocketException()
                } else {
                  disconnect()
                }
              }
            }
          )
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e(ex, "An error occurred trying to setup the socket")

          if (ex is SocketException) {
            handleSocketException()
          } else {
            disconnect()
          }
        }
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

          if (ex is SocketException) {
            handleSocketException()
          } else {
            disconnect()
          }
        }
      }
    )
  }

  override fun sendSyncComplete() {
    Timber.i("P2P sync complete")
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) {
        postUIAction(UIAction.SHOW_TRANSFER_COMPLETE_DIALOG)
      }
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
          Timber.i("Progress update: Chunk data sent successfully")
          syncSenderHandler.updateTotalSentRecordCount()
          viewModelScope.launch(dispatcherProvider.io()) { syncSenderHandler.sendNextManifest() }
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.e("Failed to send chunk data", ex)

          if (ex is SocketException) {
            handleSocketException()
          } else {
            disconnect()
          }
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

            if (ex is SocketException) {
              handleSocketException()
            } else {
              disconnect()
            }
          }
        }
      )
    }
  }

  override fun getCurrentConnectedDevice(): DeviceInfo? {
    return dataSharingStrategy.getCurrentDevice()
  }

  override fun processReceivedHistory(syncPayload: StringPayload) {
    connectionLevel = Constants.ConnectionLevel.RECEIPT_OF_RECEIVED_HISTORY

    // Handle non empty payload
    val receivedHistoryListType = object : TypeToken<List<P2PReceivedHistory?>?>() {}.type
    val receivedHistory: List<P2PReceivedHistory> =
      Gson().fromJson(syncPayload.string, receivedHistoryListType)

    val dataTypes = P2PLibrary.getInstance().getSenderTransferDao().getP2PDataTypes()
    syncSenderHandler = createSyncSenderHandler(dataTypes, receivedHistory)

    if (!dataTypes.isEmpty()) {
      Timber.i("Process received history json data not null")
      viewModelScope.launch(dispatcherProvider.io()) { syncSenderHandler.startSyncProcess() }
    } else {
      Timber.i("Process received history json data null")
      disconnect()
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
      withContext(dispatcherProvider.main()) {
        postUIAction(UIAction.SENDER_SYNC_COMPLETE, senderSyncComplete)
      }
    }
  }

  override fun updateTransferProgress(totalSentRecords: Long, totalRecords: Long) {
    val percentageSent = totalSentRecords.divideToPercent(totalRecords)
    viewModelScope.launch {
      withContext(dispatcherProvider.main()) {
        postUIAction(
          UIAction.UPDATE_TRANSFER_PROGRESS,
          TransferProgress(
            totalRecordCount = totalRecords,
            transferredRecordCount = totalSentRecords,
            percentageTransferred = percentageSent
          )
        )
      }
    }
  }

  fun notifyDataTransferStarting() {
    postUIAction(UIAction.NOTIFY_DATA_TRANSFER_STARTING, DeviceRole.SENDER)
  }

  class Factory(
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PSenderViewModel(dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
