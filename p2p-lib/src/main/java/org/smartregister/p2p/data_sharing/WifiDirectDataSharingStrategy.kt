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
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.ActivityCompat
import com.google.gson.Gson
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.net.Socket
import java.net.SocketException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.smartregister.p2p.WifiP2pBroadcastReceiver
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.payload.SyncPayloadType
import org.smartregister.p2p.search.contract.P2PManagerListener
import org.smartregister.p2p.utils.DefaultDispatcherProvider
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 21-03-2022. */
class WifiDirectDataSharingStrategy : DataSharingStrategy, P2PManagerListener {

  lateinit var context: Activity
  private val wifiP2pManager: WifiP2pManager by lazy(LazyThreadSafetyMode.NONE) {
    context.getSystemService(Context.WIFI_P2P_SERVICE) as WifiP2pManager
  }
  private val accessFineLocationPermissionRequestInt: Int = 12345
  private var wifiP2pChannel: WifiP2pManager.Channel? = null
  private var wifiP2pReceiver: BroadcastReceiver? = null

  private var wifiP2pInfo: WifiP2pInfo? = null
  private var onConnectionInfo: (() -> Unit)? = null
  private var wifiP2pGroup: WifiP2pGroup? = null
  private var currentDevice: WifiP2pDevice? = null

  val PORT = 8988
  val SOCKET_TIMEOUT = 5_000

  private var socket: Socket? = null
  private var dataInputStream: DataInputStream? = null
  private var dataOutputStream: DataOutputStream? = null

  private var requestedDisconnection = false
  private var isSearchingDevices = false
  private var paired = false
  private var dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()

  private lateinit var coroutineScope: CoroutineScope

  private val MANIFEST = "MANIFEST"

  override fun setDispatcherProvider(dispatcherProvider: DispatcherProvider) {
    this.dispatcherProvider = dispatcherProvider
  }

  override fun setActivity(context: Activity) {
    this.context = context
  }

  override fun setCoroutineScope(coroutineScope: CoroutineScope) {
    this.coroutineScope = coroutineScope
  }

  override fun searchDevices(
    onDeviceFound: OnDeviceFound,
    onConnected: DataSharingStrategy.PairingListener
  ) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      requestAccessFineLocationIfNotGranted()
    }

    // Check if already connected and disconnect
    requestDeviceInfo2()

    initChannel(onDeviceFound = onDeviceFound, onConnected = onConnected)

    listenForWifiP2pEventsIntents()
    initiatePeerDiscovery(onDeviceFound)
  }

  private fun requestConnectionInfo() {
    wifiP2pManager.requestConnectionInfo(wifiP2pChannel) { onConnectionInfoAvailable(it, null) }
  }

  private fun listenForWifiP2pEventsIntents() {
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
      initiatePeerDiscovery(null)
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

  private fun initiatePeerDiscovery(onDeviceFound: OnDeviceFound?) {
    if (ActivityCompat.checkSelfPermission(
        context,
        android.Manifest.permission.ACCESS_FINE_LOCATION
      ) != PackageManager.PERMISSION_GRANTED
    ) {
      return handleAccessFineLocationNotGranted()
    }

    isSearchingDevices = true
    wifiP2pManager.discoverPeers(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          logDebug("Discovering peers successful")
        }

        override fun onFailure(reason: Int) {
          val exception = Exception("$reason: ${getWifiP2pReason(reason)}")
          onDeviceFound?.failed(exception)
          onSearchingFailed(exception)
        }
      }
    )
    Timber.d("Peer discovery initiated")
  }
  private fun requestDeviceInfo() {
    wifiP2pChannel?.also { wifiP2pChannel ->
      if (ActivityCompat.checkSelfPermission(
          context,
          android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return handleAccessFineLocationNotGranted()
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        wifiP2pManager.requestDeviceInfo(wifiP2pChannel) {
          if (it != null) {
            handleWifiP2pDevice(it)
          }
        }
      } else {
        /** This has been handled by [WifiDirectDataSharingStrategy.onDeviceInfoChanged] */
      }
    }
  }

  private fun requestDeviceInfo2() {
    wifiP2pChannel?.also { wifiP2pChannel ->
      if (ActivityCompat.checkSelfPermission(
          context,
          android.Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED
      ) {
        return handleAccessFineLocationNotGranted()
      }

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        wifiP2pManager.requestDeviceInfo(wifiP2pChannel) {
          if (it != null && it.status == WifiP2pDevice.CONNECTED) {
            disconnect(WifiDirectDevice(it), object: DataSharingStrategy.OperationListener {
              override fun onSuccess(device: DeviceInfo?) {
                Timber.e("Successfully connected from Wifi-Direct")
              }

              override fun onFailure(device: DeviceInfo?, ex: Exception) {
                Timber.e(ex, "Successfully disconnect from Wifi-Direct")
              }
            })
          }
        }
      } else {
        wifiP2pManager.requestConnectionInfo(wifiP2pChannel) {
          if (it != null && it.groupFormed) {
            wifiP2pManager.removeGroup(
              wifiP2pChannel,
              object : WifiP2pManager.ActionListener {
                override fun onSuccess() {
                  Timber.e("Successfully connected from Wifi-Direct")
                }

                override fun onFailure(reason: Int) {
                  Timber.e(Exception(getWifiP2pReason(reason)), "Successfully disconnect from Wifi-Direct")
                }
              })
          }
        }
      }
    }
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
            currentDevice = wifiDirectDevice
            paired = true

            onConnectionSucceeded(device)
            operationListener.onSuccess(device)
          }

          override fun onFailure(reason: Int) {
            val exception = Exception("Error #$reason: ${getWifiP2pReason(reason)}")
            onConnectionFailed(device, exception)
            operationListener.onFailure(device, exception)
          }
        }
      )
    }
  }

  override fun disconnect(
    device: DeviceInfo,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    requestedDisconnection = true
    wifiP2pManager.removeGroup(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          paired = false
          onDisconnectSucceeded(device)
          operationListener.onSuccess(device)
        }

        override fun onFailure(reason: Int) {
          val exception = Exception("Error #$reason: ${getWifiP2pReason(reason)}")
          onDisconnectFailed(device, exception)
          operationListener.onFailure(device, exception)
        }
      }
    )
  }

  override fun send(
    device: DeviceInfo?,
    syncPayload: PayloadContract<out Any>,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for sending
    // Check if this is the sender/receiver
    if (wifiP2pInfo == null) {
      val p2pDevice = (device?.strategySpecificDevice as WifiP2pDevice)
      val errorMsg = "WifiP2PInfo is not available"
      logError(errorMsg)
      operationListener.onFailure(
        device,
        Exception("Error sending to ${p2pDevice.deviceName}(${p2pDevice.deviceAddress}): $errorMsg")
      )
      return
    }

    coroutineScope.launch(dispatcherProvider.io()) {
      makeSocketConnections(getGroupOwnerAddress()) { socket ->
        if (socket != null) {

          try {

          when (syncPayload.getDataType()) {
            SyncPayloadType.STRING -> {
              writeStringPayload(syncPayload, operationListener, device)
            }

            SyncPayloadType.BYTES -> {
              writeBytePayload(syncPayload, operationListener, device)
            }
          }
            } catch(ex: SocketException) {
              operationListener.onFailure(device, ex)
            }
        } else {
          onConnectionInfo =
            fun() {
              send(device, syncPayload, operationListener)
            }

          operationListener.onFailure(device, Exception("An exception occurred and the socket is null"))
        }
      }
    }
  }

  private fun writeBytePayload(
      syncPayload: PayloadContract<out Any>,
      operationListener: DataSharingStrategy.OperationListener,
      device: DeviceInfo?
  ) {
    (dataOutputStream?.apply {
      val byteArray = syncPayload.getData() as ByteArray

      writeUTF(SyncPayloadType.BYTES.name)
      writeLong(byteArray.size.toLong())

      val len = byteArray.size
      var offset = 0
      var chunkSize = 1024

      while (offset < len) {
        if (chunkSize > len) {
          chunkSize = len
        }
        write(byteArray, offset, chunkSize)

        offset += chunkSize
        if ((len - offset) < chunkSize) {
          chunkSize = len - offset
        }
      }

      operationListener.onSuccess(device)
    }
      ?: run {
        operationListener.onFailure(device, Exception("DataOutputStream is null"))
      })
  }

  private fun writeStringPayload(
      syncPayload: PayloadContract<out Any>,
      operationListener: DataSharingStrategy.OperationListener,
      device: DeviceInfo?
  ) {
    if (dataOutputStream != null) {
      dataOutputStream?.apply {
        writeUTF(SyncPayloadType.STRING.name)
        flush()

        writeUTF(syncPayload.getData() as String)
        flush()

        operationListener.onSuccess(device)
      }
    } else {
      operationListener.onFailure(device, Exception("DataOutputStream is null"))
    }
  }

  private fun getGroupOwnerAddress(): String {
    // This also causes a crash on the app
    return wifiP2pInfo!!.groupOwnerAddress.hostAddress
  }

  suspend fun makeSocketConnections(
    groupOwnerAddress: String,
    onSocketConnectionMade: (socket: Socket?) -> Unit
  ) {
    val socketResult: Socket?
    if (socket != null) {
      socketResult = socket
    } else if (wifiP2pInfo == null) {
      // Request connections
      requestConnectionInfo()

      socketResult = null
      socket = socketResult
    } else if (wifiP2pInfo?.isGroupOwner == true) {
      // Start a server to accept connections.
      Timber.e("Accepting connections")
      socketResult = acceptConnectionsToServerSocket()
      socket = socketResult
    } else {
      // Connect to the server running on the group owner device.
      socketResult = connectToServerSocket(groupOwnerAddress)
      Timber.e("Making connection to server")
      socket = socketResult
    }

    onSocketConnectionMade.invoke(socketResult)
  }

  private suspend fun acceptConnectionsToServerSocket(): Socket? =
    withContext(dispatcherProvider.io()) {
      try {
        val serverSocket = ServerSocket(PORT)
        serverSocket.accept().apply { constructStreamsFromSocket(this) }
      } catch (e: Exception) {
        Timber.e(e)
        null
      }
    }

  private fun constructStreamsFromSocket(socket: Socket) {
    dataInputStream = DataInputStream(socket.getInputStream())
    dataOutputStream = DataOutputStream(socket.getOutputStream())
  }

  private suspend fun connectToServerSocket(groupOwnerAddress: String): Socket? =
    withContext(dispatcherProvider.io()) {
      var retryCount = 3
      val retryDuration = 5000L
      var clientSocket : Socket? = null

      while (retryCount > 0) {
        try {
          clientSocket = Socket().apply {
            bind(null)
            connect(InetSocketAddress(groupOwnerAddress, PORT), SOCKET_TIMEOUT)
            constructStreamsFromSocket(this)
          }
          break
        } catch (e: Exception) {
          Timber.e(e)
          clientSocket = null
          delay(retryDuration)
        }

        retryCount--
      }

      clientSocket
    }

  override fun sendManifest(
    device: DeviceInfo?,
    manifest: Manifest,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for sending
    // Check if this is the sender/receiver

    try {
    dataOutputStream?.apply {
      val manifestString = Gson().toJson(manifest)
      try {
        writeUTF(MANIFEST)
        writeUTF(manifestString)
      } catch (e: Exception) {
        operationListener.onFailure(device = device, ex = e)
      }
      flush()
      operationListener.onSuccess(device = device)
    }
      } catch(ex: SocketException) {
        Timber.e("Sending the manifest failed", ex)
      operationListener.onFailure(device, ex)
      }
  }

  override fun receive(
    device: DeviceInfo?,
    payloadReceiptListener: DataSharingStrategy.PayloadReceiptListener,
    operationListener: DataSharingStrategy.OperationListener
  ) {
    // Check if the socket is setup for listening
    // Check if this is the receiver/sender

    if (wifiP2pInfo == null) {
      val p2pDevice = (device?.strategySpecificDevice as WifiP2pDevice)
      val errorMsg = "WifiP2PInfo is not available"
      logError(errorMsg)
      operationListener.onFailure(
        device,
        Exception(
          "Error receiving from ${p2pDevice.deviceName}(${p2pDevice.deviceAddress}): $errorMsg"
        )
      )
    }

    coroutineScope.launch(dispatcherProvider.io()) {
      makeSocketConnections(getGroupOwnerAddress()) { socket ->
        if (socket != null) {

          dataInputStream?.run {
            try {
            val dataType = readUTF()

            if (dataType == SyncPayloadType.STRING.name) {
              val stringPayload = readUTF()
              payloadReceiptListener.onPayloadReceived(StringPayload(stringPayload))
            } else if (dataType == SyncPayloadType.BYTES.name) {
              var payloadLen = readLong()
              val payloadByteArray = ByteArray(payloadLen.toInt())
              var currentBufferPos = 0
              var n = 0

              while (payloadLen > 0 &&
                read(payloadByteArray, currentBufferPos, Math.min(1024, payloadLen).toInt()).also {
                  n = it
                } != -1) {

                currentBufferPos += n
                payloadLen -= n.toLong()
                logDebug("file size $payloadLen")
              }
              payloadReceiptListener.onPayloadReceived(BytePayload(payloadByteArray))
            } else {
              operationListener.onFailure(
                getCurrentDevice(),
                Exception("Unknown datatype: $dataType")
              )
            }
            } catch(ex: SocketException) {
              operationListener.onFailure(device, ex)
            }
          }
        } else {
          operationListener.onFailure(device, Exception("Socket is null"))
        }
      }
    }
  }

  override fun receiveManifest(
    device: DeviceInfo,
    operationListener: DataSharingStrategy.OperationListener
  ): Manifest? {
    // Check if the socket is setup for listening
    // Check if this is the receiver/sender

    return dataInputStream?.run {

      try {
      val dataType = readUTF()

      if (dataType == MANIFEST) {
        val manifestString = readUTF()
        Gson().fromJson(manifestString, Manifest::class.java)
      } else {
        null
      }
        } catch(ex: SocketException) {
          Timber.e("Receiving manfiest failed", ex)
        operationListener.onFailure(device, ex)
        null
      }
    }
  }

  override fun onErrorOccurred(ex: Exception) {
    // TODO: Show random error occurred
    closeSocketAndStreams()
  }

  override fun onConnectionFailed(device: DeviceInfo, ex: Exception) {
    // TODO: Return this to the device
    closeSocketAndStreams()
  }

  override fun onConnectionSucceeded(device: DeviceInfo) {
    // TODO: Return this to the device
  }

  override fun onDisconnectFailed(device: DeviceInfo, ex: Exception) {
    // TODO: Return this to the device
  }

  override fun onDisconnectSucceeded(device: DeviceInfo) {
    // TODO: Return this to the device

    closeSocketAndStreams()
  }

  override fun onPairingFailed(ex: Exception) {
    // TODO: Return this to the device
  }

  override fun onSendingFailed(ex: Exception) {
    // TODO: Return this to the device
    // Also show an error on the UI
  }

  override fun onSearchingFailed(ex: Exception) {
    // TODO: Return this to the device
  }

  override fun getCurrentDevice(): DeviceInfo? {
    if (currentDevice != null) {
      return WifiDirectDevice(currentDevice!!)
    } else {
      return null
    }
  }

  override fun onResume(isScanning: Boolean) {
    if (isScanning) {
      listenForWifiP2pEventsIntents()
      initiatePeerDiscoveryOnceAccessFineLocationGranted()
      requestConnectionInfo()
    }
  }

  override fun onPause() {
    runCatching { wifiP2pReceiver?.also { context.unregisterReceiver(it) } }.onFailure {
      Timber.e(it)
    }
  }

  override fun initChannel(
    onDeviceFound: OnDeviceFound,
    onConnected: DataSharingStrategy.PairingListener
  ) {
    wifiP2pChannel = wifiP2pManager.initialize(context, context.mainLooper, null)
    wifiP2pChannel?.also { channel ->
      wifiP2pReceiver =
        WifiP2pBroadcastReceiver(
          wifiP2pManager,
          channel,
          object : P2PManagerListener {
            override fun handleWifiP2pDisabled() {
              this@WifiDirectDataSharingStrategy.handleWifiP2pDisabled()
            }

            override fun handleWifiP2pEnabled() {
              this@WifiDirectDataSharingStrategy.handleWifiP2pEnabled()
            }

            override fun handleUnexpectedWifiP2pState(wifiState: Int) {
              this@WifiDirectDataSharingStrategy.handleUnexpectedWifiP2pState(wifiState)
            }

            override fun handleWifiP2pDevice(device: WifiP2pDevice) {
              onDeviceFound.deviceFound(listOf(WifiDirectDevice(device)))

              this@WifiDirectDataSharingStrategy.handleWifiP2pDevice(device)
            }

            override fun handleP2pDiscoveryStarted() {
              this@WifiDirectDataSharingStrategy.handleP2pDiscoveryStarted()
            }

            override fun handleP2pDiscoveryStopped() {
              this@WifiDirectDataSharingStrategy.handleP2pDiscoveryStopped()
            }

            override fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
              this@WifiDirectDataSharingStrategy.handleUnexpectedWifiP2pDiscoveryState(
                discoveryState
              )
            }

            override fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
              val devicesList = peerDeviceList.deviceList.map { WifiDirectDevice(it) }
              onDeviceFound.deviceFound(devicesList)

              this@WifiDirectDataSharingStrategy.handleP2pPeersChanged(peerDeviceList)
            }

            override fun handleAccessFineLocationNotGranted() {
              this@WifiDirectDataSharingStrategy.handleAccessFineLocationNotGranted()
            }

            override fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int) {
              this@WifiDirectDataSharingStrategy.handleMinimumSDKVersionNotMet(minimumSdkVersion)
            }

            override fun onConnectionInfoAvailable(info: WifiP2pInfo, wifiP2pGroup: WifiP2pGroup?) {
              this@WifiDirectDataSharingStrategy.onConnectionInfoAvailable(info, wifiP2pGroup)

              if (info.groupFormed) {
                paired = true
                onConnected.onSuccess(null)
              } else {

                if (paired) {
                  closeSocketAndStreams()
                  if (!requestedDisconnection) {
                    onConnected.onDisconnected()
                  }

                  paired = false
                }
                requestedDisconnection = false
              }
            }

            override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
              this@WifiDirectDataSharingStrategy.onConnectionInfoAvailable(info, null)
            }
          },
          context
        )
    }
  }

  private fun logDebug(message: String) {
    Timber.d(message)
  }

  private fun logError(message: String) {
    Timber.e(message)
  }

  override fun handleWifiP2pDisabled() {
    // TODO: Handle the issue here
  }

  override fun handleWifiP2pEnabled() {
    // TODO: Handle the issue here
  }

  override fun handleUnexpectedWifiP2pState(wifiState: Int) {
    // TODO: Handle the issue here
    // Also show an error on the UI
  }

  override fun handleWifiP2pDevice(device: WifiP2pDevice) {
    if (device != null) {
      currentDevice = device
    }
  }

  override fun handleP2pDiscoveryStarted() {
    // TODO: Handle the issue here
  }

  override fun handleP2pDiscoveryStopped() {
    // TODO: Handle the issue here
  }

  override fun handleUnexpectedWifiP2pDiscoveryState(discoveryState: Int) {
    // TODO: Handle the issue here
  }

  override fun handleP2pPeersChanged(peerDeviceList: WifiP2pDeviceList) {
    // TODO: Handle the issue here
  }

  override fun handleAccessFineLocationNotGranted() {
    // TODO: Handle the issue here
  }

  override fun handleMinimumSDKVersionNotMet(minimumSdkVersion: Int) {
    // TODO: Handle the issue here
  }

  override fun onConnectionInfoAvailable(info: WifiP2pInfo, wifiP2pGroup: WifiP2pGroup?) {
    if (info == null) {
      logError("Connection info provided is NULL")
      return
    }

    val message =
      "Connection info available: groupFormed = ${info.groupFormed}, isGroupOwner = ${info.isGroupOwner}"
    logDebug(message)
    wifiP2pInfo = info

    if (info.groupFormed && wifiP2pGroup != null) {
      this.wifiP2pGroup = wifiP2pGroup

      if (info.isGroupOwner) {
        val isGroupOwner = info.isGroupOwner
        currentDevice = wifiP2pGroup.clientList.firstOrNull { it.isGroupOwner != isGroupOwner }
      }
    }

    if (onConnectionInfo != null) {
      onConnectionInfo?.invoke()
      onConnectionInfo = null
    }
  }

  override fun onConnectionInfoAvailable(info: WifiP2pInfo) {
    this.onConnectionInfoAvailable(info, null)
  }

  override fun stopSearchingDevices(operationListener: DataSharingStrategy.OperationListener?) {
    if (isSearchingDevices) {
      wifiP2pManager.stopPeerDiscovery(
        wifiP2pChannel,
        object : WifiP2pManager.ActionListener {
          override fun onSuccess() {
            logDebug("Successfully stopped peer discovery")
            operationListener?.onSuccess(null)
          }

          override fun onFailure(reason: Int) {
            val ex =
              Exception("Error occurred trying to stop peer discovery ${getWifiP2pReason(reason)}")
            Timber.e(ex)
            operationListener?.onFailure(null, ex)
          }
        }
      )
      isSearchingDevices = false
    }
  }

  fun closeSocketAndStreams() {
    stopSearchingDevices(null)

    dataInputStream?.run { close() }

    dataOutputStream?.run {
      flush()
      close()
    }

    if (socket != null) {
      try {
        socket!!.close()
      } catch (e: IOException) {
        Timber.e(e)
      }
      socket = null
    }
  }

  fun getWifiP2pReason(reasonInt: Int): String =
    when (reasonInt) {
      0 -> "Error"
      1 -> "Unsupported"
      2 -> "Busy"
      else -> "Unknown"
    }

  class WifiDirectDevice(var wifiP2pDevice: WifiP2pDevice) : DeviceInfo {

    override var strategySpecificDevice: Any
      get() = wifiP2pDevice
      set(value) {
        wifiP2pDevice = value as WifiP2pDevice
      }

    override fun getDisplayName(): String =
      "${wifiP2pDevice.deviceName} (${wifiP2pDevice.deviceAddress})"

    override fun name(): String {
      return wifiP2pDevice.deviceName
    }

    override fun address(): String {
      return wifiP2pDevice.deviceAddress
    }
  }

  override fun onStop() {
    closeSocketAndStreams()

    requestedDisconnection = true
    wifiP2pManager.removeGroup(
      wifiP2pChannel,
      object : WifiP2pManager.ActionListener {
        override fun onSuccess() {
          Timber.i("Device successfully disconnected")
          paired = false
        }

        override fun onFailure(reason: Int) {
          val exception = Exception("Error #$reason: ${getWifiP2pReason(reason)}")
          Timber.e(exception)
        }
      }
    )
  }
}
