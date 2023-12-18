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
package org.smartregister.p2p.search.ui.p2p

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import org.smartregister.p2p.R
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.OnDeviceFound
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.search.ui.BaseViewModel
import org.smartregister.p2p.search.ui.UIAction
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

class P2PViewModel(
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : BaseViewModel(dataSharingStrategy) {
  val p2PUiState = mutableStateOf(P2PUiState())
  private val _deviceList = MutableLiveData<List<DeviceInfo>>()
  val deviceList: LiveData<List<DeviceInfo>>
    get() = _deviceList

  private val _p2PState = MutableLiveData<P2PState>()
  val p2PState: LiveData<P2PState>
    get() = _p2PState

  var deviceRole: DeviceRole = DeviceRole.SENDER
  private var currentConnectedDevice: DeviceInfo? = null
  private var requestDisconnection = false
  private var isSenderSyncComplete = false

  fun onEvent(event: P2PEvent) {
    when (event) {
      is P2PEvent.StartScanning -> {
        // check whether location services are enabled
        postUIAction(UIAction.REQUEST_LOCATION_PERMISSIONS_ENABLE_LOCATION)
      }
      is P2PEvent.PairWithDevice -> {
        // initiate pairing with device
        connectToDevice(event.device)
      }
      is P2PEvent.CancelDataTransfer -> {
        // show cancel transfer dialog
        showCancelTransferDialog()
      }
      P2PEvent.ConnectionBreakConfirmed -> {
        // cancel data transfer
        cancelTransfer(P2PState.INITIATE_DATA_TRANSFER)
        setRequestDisconnection(true)
      }
      P2PEvent.DismissConnectionBreakDialog -> {
        p2PUiState.value = p2PUiState.value.copy(showP2PDialog = false)
      }
      P2PEvent.DataTransferCompleteConfirmed -> {
        updateP2PState(P2PState.PROMPT_NEXT_TRANSFER)
      }
      P2PEvent.BottomSheetClosed -> {
        cancelTransfer(P2PState.PROMPT_NEXT_TRANSFER)
      }
    }
  }

  fun startScanning() {
    postUIAction(UIAction.KEEP_SCREEN_ON, true)
    dataSharingStrategy.searchDevices(onDeviceFound, pairingListener)
  }

  val onDeviceFound by lazy {
    object : OnDeviceFound {
      override fun deviceFound(devices: List<DeviceInfo>) {
        _deviceList.postValue(devices)
        if (deviceRole == DeviceRole.SENDER &&
            (_p2PState.value == null || _p2PState.value == P2PState.INITIATE_DATA_TRANSFER)
        ) {
          updateP2PState(P2PState.PAIR_DEVICES_FOUND)
        }

        Timber.i("onDeviceFound: State is ${p2PState.value?.name}")
        Timber.i("Devices searching succeeded. Found ${devices.size} devices")
      }

      override fun failed(ex: Exception) {
        postUIAction(UIAction.KEEP_SCREEN_ON, false)

        Timber.d("Devices searching failed")
        Timber.e(ex)
        updateP2PState(P2PState.PAIR_DEVICES_SEARCH_FAILED)
      }
    }
  }

  val pairingListener: DataSharingStrategy.PairingListener by lazy {
    object : DataSharingStrategy.PairingListener {

      override fun onSuccess(device: DeviceInfo?) {

        if (!dataSharingStrategy.isPairingInitiated() && deviceRole == DeviceRole.SENDER) {
          Timber.i("pairingListener#onSuccess() -> pairingInitiated = false ${deviceRole.name}")
          return
        }
        currentConnectedDevice = device
        Timber.i("Device role is ${deviceRole.name}")

        when (deviceRole) {
          DeviceRole.RECEIVER -> {
            updateP2PState(P2PState.WAITING_TO_RECEIVE_DATA)
            postUIAction(UIAction.PROCESS_SENDER_DEVICE_DETAILS)
          }
          DeviceRole.SENDER -> {
            postUIAction(UIAction.SEND_DEVICE_DETAILS)
          }
        }
      }

      override fun onFailure(device: DeviceInfo?, ex: Exception) {
        // view.keepScreenOn(false)
        postUIAction(UIAction.KEEP_SCREEN_ON, false)
        Timber.i("Devices pairing failed")
        Timber.e(ex)
        updateP2PState(P2PState.PROMPT_NEXT_TRANSFER)
      }

      override fun onDisconnected() {
        Timber.d("onDisconnected()")
        if (!requestDisconnection) {
          showToast(R.string.connection_was_disconnected)

          postUIAction(UIAction.KEEP_SCREEN_ON, false)

          Timber.i("Successful on disconnect")
          Timber.i("isSenderSyncComplete $isSenderSyncComplete")
          // But use a flag to determine if sync was completed
          updateP2PState(P2PState.DEVICE_DISCONNECTED)
        }
        if (isSenderSyncComplete) {
          updateP2PState(P2PState.TRANSFER_COMPLETE)

          // TODO: Remove this, this should have fixed the socket bind address exception
          dataSharingStrategy.cleanup()
        }
      }
    }
  }

  fun initChannel() {
    dataSharingStrategy.initChannel(onDeviceFound, pairingListener)
  }

  fun connectToDevice(device: DeviceInfo) {
    dataSharingStrategy.connect(
      device,
      object : DataSharingStrategy.OperationListener {
        override fun onSuccess(device: DeviceInfo?) {
          currentConnectedDevice = device
          Timber.d("Connecting to device %s success", device?.getDisplayName() ?: "Unknown")
          updateP2PState(P2PState.PREPARING_TO_SEND_DATA)
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          Timber.d("Connecting to device %s failure", device?.getDisplayName() ?: "Unknown")
          Timber.e(ex)
          updateP2PState(P2PState.CONNECT_TO_DEVICE_FAILED)
        }
      }
    )
  }

  fun showTransferCompleteDialog(p2PState: P2PState) {
    updateP2PState(p2PState)
  }

  fun cancelTransfer(p2PState: P2PState = P2PState.TRANSFER_CANCELLED) {
    if (dataSharingStrategy.getCurrentDevice() == null) {
      updateP2PState(p2PState)
      return
    }
    viewModelScope.launch {
      dataSharingStrategy.disconnect(
        dataSharingStrategy.getCurrentDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.d("Disconnection successful")
            updateP2PState(p2PState)
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            Timber.e(ex, "P2P diconnection failed")
            updateP2PState(p2PState)
          }
        }
      )
    }
  }

  fun updateTransferProgress(transferProgress: TransferProgress) {
    p2PUiState.value = p2PUiState.value.copy(transferProgress = transferProgress)
  }

  fun getCurrentConnectedDevice(): DeviceInfo? {
    return currentConnectedDevice
  }

  override fun updateP2PState(state: P2PState) {
    Timber.i("P2P state updated to ${state.name}")
    _p2PState.postValue(state)
  }

  fun closeP2PScreen() {
    if (dataSharingStrategy.getCurrentDevice() == null) {
      postUIAction(UIAction.FINISH)
      return
    }

    viewModelScope.launch {
      dataSharingStrategy.disconnect(
        dataSharingStrategy.getCurrentDevice()!!,
        object : DataSharingStrategy.OperationListener {
          override fun onSuccess(device: DeviceInfo?) {
            Timber.d("Disconnection successful")
            postUIAction(UIAction.FINISH)
          }

          override fun onFailure(device: DeviceInfo?, ex: Exception) {
            postUIAction(UIAction.FINISH)
            Timber.e(ex, "P2P diconnection failed")
          }
        }
      )
    }
  }

  fun setCurrentConnectedDevice(device: DeviceInfo?) {
    currentConnectedDevice = device
  }

  fun setRequestDisconnection(requestDisconnection: Boolean) {
    this.requestDisconnection = requestDisconnection
  }
  fun getRequestDisconnection(): Boolean {
    return requestDisconnection
  }

  fun showCancelTransferDialog() {
    // show cancel transfer dialog
    p2PUiState.value = p2PUiState.value.copy(showP2PDialog = true)
  }

  fun updateSenderSyncComplete(complete: Boolean = false) {
    this.isSenderSyncComplete = complete
  }

  override fun onCleared() {
    Timber.d("P2PViewModel onCleared")
    super.onCleared()
  }

  class Factory(
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PViewModel(dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
