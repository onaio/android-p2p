package org.smartregister.p2p.data_sharing

import android.app.Activity
import org.smartregister.p2p.SyncPayload

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022.
 */
interface DataSharingStrategy {

    fun setActivity(context: Activity)

    fun searchDevices(onDeviceFound: OnDeviceFound)

    fun connect(device: DeviceInfo, operationListener: OperationListener)

    fun disconnect(device: DeviceInfo, operationListener: OperationListener)

    fun send(device: DeviceInfo, syncPayload: SyncPayload, operationListener: OperationListener)

    fun sendManifest(device: DeviceInfo, manifest: Manifest, operationListener: OperationListener)

    fun receive(device: DeviceInfo, syncPayload: SyncPayload, operationListener: OperationListener)

    fun receiveManifest(device: DeviceInfo, manifest: Manifest, operationListener: OperationListener)

    fun onErrorOccurred(ex: Exception)

    fun onConnectionFailed(device: DeviceInfo, ex: Exception)

    fun onConnectionSucceeded(device: DeviceInfo)

    fun onDisconnectFailed(device: DeviceInfo, ex: Exception)

    fun onDisconnectSucceeded(device: DeviceInfo)

    fun onPairingFailed(ex: Exception)

    fun onSendingFailed(ex: Exception)

    fun onSearchingFailed(ex: Exception)

    interface OperationListener {

        fun onSuccess(device: DeviceInfo)

        fun onFailure(device: DeviceInfo, ex: Exception)

    }

}