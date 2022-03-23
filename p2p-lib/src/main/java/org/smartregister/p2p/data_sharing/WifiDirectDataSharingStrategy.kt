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
import android.content.BroadcastReceiver
import android.content.Context
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.wifi.p2p.WifiP2pConfig
import android.net.wifi.p2p.WifiP2pDevice
import android.net.wifi.p2p.WifiP2pDeviceList
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import org.smartregister.p2p.SyncPayload
import org.smartregister.p2p.WifiP2pBroadcastReceiver
import org.smartregister.p2p.search.contract.P2PManagerListener
import timber.log.Timber

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022. */
class WifiDirectDataSharingStrategy() : DataSharingStrategy, P2PManagerListener {

  lateinit var context: Activity
  private val wifiP2pManager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
    context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
  }
  private val accessFineLocationPermissionRequestInt: Int = 12345
  private var wifiP2pChannel: WifiP2pManager.Channel? = null
  private var wifiP2pReceiver: BroadcastReceiver? = null

  override fun setActivity(context: Activity) {
    this.context = context
  }

  override fun searchDevices(onDeviceFound: OnDeviceFound) {
    // Wifi P2p
    wifiP2pChannel = wifiP2pManager.initialize(context, context.mainLooper, null)
    wifiP2pChannel?.also { channel ->
      wifiP2pReceiver = WifiP2pBroadcastReceiver(wifiP2pManager, channel, this, context)
    }

    // renameWifiDirectName();

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestAccessFineLocationIfNotGranted()
    }

    listenForWifiP2pIntents()
    initiatePeerDiscovery()
  }

  private fun listenForWifiP2pIntents() {
    wifiP2pReceiver?.also {
      context.registerReceiver(
        it,
        IntentFilter().apply {
          addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
          addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
        }
      )
    }
  }

  private fun initiatePeerDiscoveryOnceAccessFineLocationGranted() {
    if (ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        requestAccessFineLocationIfNotGranted()
      } else {
        handleMinimumSDKVersionNotMet(Build.VERSION_CODES.M)
      }
    } else {
      initiatePeerDiscovery()
    }
  }

  @RequiresApi(Build.VERSION_CODES.M)
  private fun requestAccessFineLocationIfNotGranted() {
    when (ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      )
    ) {
      PackageManager.PERMISSION_GRANTED -> logDebug("Wifi P2P: Access fine location granted")
      else -> {
        logDebug("Wifi P2P: Requesting access fine location permission")
        return context.requestPermissions(
          arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION),
          accessFineLocationPermissionRequestInt
        )
      }
    }
  }

  private fun initiatePeerDiscovery() {
    if (ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return handleAccessFineLocationNotGranted()
    }

    wifiP2pManager.discoverPeers(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          logDebug("Discovering peers successful")
          // handleP2pDiscoverySuccess()
        }

        override fun onFailure(reason: Int) {
          // handleP2pDiscoveryFailure(reason)
          onSearchingFailed(java.lang.Exception(""))
        }
      }
    )
    Timber.d("Peer discovery initiated")
  }

  override fun connect(
    device: DeviceInfo,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    val wifiDirectDevice = device.strategySpecificDevice as WifiP2pDevice

    Timber.d("Wifi P2P: Initiating connection to device: ${wifiDirectDevice.deviceName}")
    val wifiP2pConfig = WifiP2pConfig().apply { deviceAddress = wifiDirectDevice.deviceAddress }
    wifiP2pChannel?.also { wifiP2pChannel ->
      if (ActivityCompat.checkSelfPermission(
          context,
          android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return handleAccessFineLocationNotGranted()
      }
      wifiP2pManager.connect(
        wifiP2pChannel,
        wifiP2pConfig,
        object : WifiP2pManager.ActionListener {
          override fun onSuccess() {
            onConnectionSucceeded(device)
          }

          override fun onFailure(reason: Int) {
            onConnectionFailed(device, Exception("Error #$reason: ${getWifiP2pReason(reason)}"))
          }
        }
      )
    }
  }

  override fun disconnect(
    device: DeviceInfo,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    wifiP2pManager.cancelConnect(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          onDisconnectSucceeded(device)
        }

        override fun onFailure(reason: Int) {
          onDisconnectFailed(device, Exception())
        }
      }
    )
  }

  override fun send(
    device: DeviceInfo,
    syncPayload: SyncPayload,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for sending
    // Check if this is the sender/receiver
  } /*

        fun makeSocketConnections() {
            if (groupOwner) {
                // Start a server to accept connections.
                acceptedConnectionsToServerSocket(sender)
            } else {
                // Connect to the server running on the group owner device.
                connectToServerSocket(groupOwnerAddress, sender)
            }
        }

        private fun acceptedConnectionsToServerSocket(sender: Boolean): Result = try {
            ServerSocket(PORT).use { server ->
                server.accept().use { socket ->
                    transmit(sender, socket)
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("Wifi P2P: ${this::class.simpleName}", e.message, e)
            Result.failure()
        }

        private fun connectToServerSocket(
            groupOwnerAddress: String,
            sender: Boolean
        ): Result = try {
            Socket().use { socket ->
                socket.bind(null)
                socket.connect(InetSocketAddress(groupOwnerAddress, PORT), SOCKET_TIMEOUT)
                transmit(sender, socket)
            }
            Result.success()
        } catch (e: Exception) {
            Log.e("Wifi P2P: ${this::class.simpleName}", e.message, e)
            Result.failure()
        }
    */
  override fun sendManifest(
    device: DeviceInfo,
    manifest: Manifest,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for sending
    // Check if this is the sender/receiver
  }

  override fun receive(
    device: DeviceInfo,
    syncPayload: SyncPayload,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for listening
    // Check if this is the receiver/sender
  }

  override fun receiveManifest(
    device: DeviceInfo,
    manifest: Manifest,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for listening
    // Check if this is the receiver/sender
  }

  override fun onErrorOccurred(ex: Exception) {
    TODO("Not yet implemented")
  }

  override fun onConnectionFailed(device: DeviceInfo, ex: Exception) {
    TODO("Not yet implemented")
  }

  override fun onConnectionSucceeded(device: DeviceInfo) {
    TODO("Not yet implemented")
  }

  override fun onDisconnectFailed(device: DeviceInfo, ex: Exception) {
    TODO("Not yet implemented")
  }

  override fun onDisconnectSucceeded(device: DeviceInfo) {
    TODO("Not yet implemented")
  }

  override fun onPairingFailed(ex: Exception) {
    TODO("Not yet implemented")
  }

  override fun onSendingFailed(ex: Exception) {
    TODO("Not yet implemented")
  }

  override fun onSearchingFailed(ex: Exception) {
    TODO("Not yet implemented")
  }

  private fun logDebug(message: String) {
    // Snackbar.make(rootView, message, Snackbar.LENGTH_SHORT).show()
    Timber.d(message)
  }

  override fun handleWifiP2pDisabled() {
    TODO("Not yet implemented")
  }

  override fun handleWifiP2pEnabled() {
    TODO("Not yet implemented")
  }

  override fun handleUnexpectedWifiP2pState(wifiState: Int) {
    TODO("Not yet implemented")
  }

  override fun handleWifiP2pDevice(device: WifiP2pDevice) {
    TODO("Not yet implemented")
  }

  override fun handleP2pDiscoveryStarted() {
    TODO("Not yet implemented")
  }

  override fun handleP2pDiscoveryStopped() {
    TODO("Not yet implemented")
  }

  override fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
    TODO("Not yet implemented")
  }

  override fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
    TODO("Not yet implemented")
  }

  override fun handleAccessFineLocationNotGranted() {
    TODO("Not yet implemented")
  }

  override fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int) {
    TODO("Not yet implemented")
  }

  override fun onConnectionInfoAvailable(info: WifiP2pInfo?) {
    /*val message =
        "Connection info available: groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
    Timber.d(message)
    if (info.groupFormed && !isSender) {
        // Start syncing given the ip addresses
        showReceiverDialog()
    }*/
  }

  private fun getWifiP2pReason(reasonInt: Int): String =
    when (reasonInt) {
      0 -> "Error"
      1 -> "Unsupported"
      2 -> "Busy"
      else -> "Unknown"
    }
}
