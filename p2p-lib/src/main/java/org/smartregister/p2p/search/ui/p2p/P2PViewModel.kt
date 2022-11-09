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
package org.smartregister.p2p.search.ui.p2p

import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.OnDeviceFound
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

class P2PViewModel(
  private val view: P2PDeviceSearchActivity,
  private val dataSharingStrategy: DataSharingStrategy,
  private val dispatcherProvider: DispatcherProvider
) : ViewModel() {
  val p2PUiState = mutableStateOf(P2PUiState())
  private var currentConnectedDevice: DeviceInfo? = null

  fun setP2PUiState() {
    // Set UI state
    p2PUiState.value = P2PUiState()
  }

  fun onEvent(event: P2PEvent) {
    when (event) {
      is P2PEvent.StartScanning -> {
        // initiate scanning
        startScanning(dataSharingStrategy = dataSharingStrategy)
      }
      is P2PEvent.PairDevicesFound -> {
        // display list of pairing devices
      }
    }
  }

  private fun startScanning(dataSharingStrategy: DataSharingStrategy) {
    // keepScreenOn(true)
    dataSharingStrategy.searchDevices(
      object : OnDeviceFound {
        override fun deviceFound(devices: List<DeviceInfo>) {
          // showDevicesList(devices)
          Timber.e("Devices searching succeeded. Found ${devices.size} devices")
        }

        override fun failed(ex: Exception) {
          // keepScreenOn(false)
          Timber.e("Devices searching failed")
          Timber.e(ex)
          // removeScanningDialog()

          /* Toast.makeText(
            this@P2PDeviceSearchActivity,
            R.string.device_searching_failed,
            Toast.LENGTH_LONG
          )
            .show()*/
        }
      },
      object : DataSharingStrategy.PairingListener {

        override fun onSuccess(device: DeviceInfo?) {

          if (currentConnectedDevice == null) {
            Timber.e("Devices paired with another: DeviceInfo is null")
          }

          currentConnectedDevice = device
          val displayName = device?.getDisplayName() ?: "Unknown"
          // showP2PSelectPage(getDeviceRole(), displayName)
        }

        override fun onFailure(device: DeviceInfo?, ex: Exception) {
          // keepScreenOn(false)
          Timber.e("Devices pairing failed")
          Timber.e(ex)
          // removeScanningDialog()
        }

        override fun onDisconnected() {
          /*if (!requestDisconnection) {
            removeScanningDialog()
            showToast("Connection was disconnected")

            keepScreenOn(false)

            if (isSenderSyncComplete) {
              showTransferCompleteDialog()
            }

            Timber.e("Successful on disconnect")
            Timber.e("isSenderSyncComplete $isSenderSyncComplete")
            // But use a flag to determine if sync was completed
          }*/
        }
      }
    )

    // showScanningDialog()
  }

  class Factory(
    private val context: P2PDeviceSearchActivity,
    private val dataSharingStrategy: DataSharingStrategy,
    private val dispatcherProvider: DispatcherProvider
  ) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
      return P2PViewModel(context, dataSharingStrategy, dispatcherProvider).apply {
        dataSharingStrategy.setCoroutineScope(viewModelScope)
      } as
        T
    }
  }
}
