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

import android.app.Activity
import org.smartregister.p2p.payload.PayloadContract

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022. */
interface DataSharingStrategy<DeviceObjectForStrategy> {

  fun setActivity(context: Activity)

  fun searchDevices(onDeviceFound: OnDeviceFound<DeviceObjectForStrategy>)

  fun connect(device: DeviceInfo<DeviceObjectForStrategy>, operationListener: OperationListener<DeviceObjectForStrategy>)

  fun disconnect(device: DeviceInfo<DeviceObjectForStrategy>, operationListener: OperationListener<DeviceObjectForStrategy>)

  fun send(
    device: DeviceInfo<DeviceObjectForStrategy>,
    syncPayload: PayloadContract<out Any>,
    operationListener: OperationListener<DeviceObjectForStrategy>
  )

  fun sendManifest(device: DeviceInfo<DeviceObjectForStrategy>, manifest: Manifest, operationListener: OperationListener<DeviceObjectForStrategy>)

  fun receive(device: DeviceInfo<DeviceObjectForStrategy>, operationListener: OperationListener<DeviceObjectForStrategy>): PayloadContract<out Any>?

  fun receiveManifest(device: DeviceInfo<DeviceObjectForStrategy>, operationListener: OperationListener<DeviceObjectForStrategy>): Manifest?

  fun onErrorOccurred(ex: Exception)

  fun onConnectionFailed(device: DeviceInfo<DeviceObjectForStrategy>, ex: Exception)

  fun onConnectionSucceeded(device: DeviceInfo<DeviceObjectForStrategy>)

  fun onDisconnectFailed(device: DeviceInfo<DeviceObjectForStrategy>, ex: Exception)

  fun onDisconnectSucceeded(device: DeviceInfo<DeviceObjectForStrategy>)

  fun onPairingFailed(ex: Exception)

  fun onSendingFailed(ex: Exception)

  fun onSearchingFailed(ex: Exception)

  interface OperationListener<DeviceObjectForStrategy> {

    fun onSuccess(device: DeviceInfo<DeviceObjectForStrategy>)

    fun onFailure(device: DeviceInfo<DeviceObjectForStrategy>, ex: Exception)
  }
}
