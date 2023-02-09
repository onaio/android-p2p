/*
 * Copyright 2022-2023 Ona Systems, Inc
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

import android.app.Activity
import kotlinx.coroutines.CoroutineScope
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.utils.DispatcherProvider

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022. */
interface DataSharingStrategy {

  fun setDispatcherProvider(dispatcherProvider: DispatcherProvider)

  fun setActivity(context: Activity)

  fun setCoroutineScope(coroutineScope: CoroutineScope)

  fun searchDevices(onDeviceFound: OnDeviceFound, onConnected: PairingListener)

  fun stopSearchingDevices(operationListener: OperationListener?)

  fun connect(device: DeviceInfo, operationListener: OperationListener)

  fun disconnect(device: DeviceInfo, operationListener: OperationListener)

  fun send(
    device: DeviceInfo?,
    syncPayload: PayloadContract<out Any>,
    operationListener: OperationListener
  )

  fun sendManifest(device: DeviceInfo?, manifest: Manifest, operationListener: OperationListener)

  fun receive(
    device: DeviceInfo?,
    payloadReceiptListener: PayloadReceiptListener,
    operationListener: OperationListener
  )

  fun receiveManifest(device: DeviceInfo, operationListener: OperationListener): Manifest?

  fun onErrorOccurred(ex: Exception)

  fun onConnectionFailed(device: DeviceInfo, ex: Exception)

  fun onConnectionSucceeded(device: DeviceInfo)

  fun onDisconnectFailed(device: DeviceInfo, ex: Exception)

  fun onDisconnectSucceeded(device: DeviceInfo)

  fun onPairingFailed(ex: Exception)

  fun onSendingFailed(ex: Exception)

  fun onSearchingFailed(ex: Exception)

  fun getCurrentDevice(): DeviceInfo?

  interface OperationListener {

    fun onSuccess(device: DeviceInfo?)

    fun onFailure(device: DeviceInfo?, ex: Exception)
  }

  interface PairingListener {

    fun onSuccess(device: DeviceInfo?)

    fun onFailure(device: DeviceInfo?, ex: Exception)

    fun onDisconnected()
  }

  interface PayloadReceiptListener {

    fun onPayloadReceived(payload: PayloadContract<out Any>?)
  }

  fun onResume(isScanning: Boolean = false)

  fun onPause()

  fun initChannel(onDeviceFound: OnDeviceFound, onConnected: DataSharingStrategy.PairingListener)
}
