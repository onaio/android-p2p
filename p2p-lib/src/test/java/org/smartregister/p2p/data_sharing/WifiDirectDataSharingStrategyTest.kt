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
import android.net.wifi.p2p.WifiP2pGroup
import android.net.wifi.p2p.WifiP2pInfo
import android.net.wifi.p2p.WifiP2pManager
import com.google.gson.Gson
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.SyncPayloadType
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.sync.DataType

class WifiDirectDataSharingStrategyTest : RobolectricTest() {

  private lateinit var wifiDirectDataSharingStrategy: WifiDirectDataSharingStrategy
  private lateinit var context: Activity
  private lateinit var wifiP2pManager: WifiP2pManager
  private lateinit var wifiP2pChannel: WifiP2pManager.Channel
  private lateinit var wifiP2pReceiver: BroadcastReceiver

  private lateinit var onDeviceFound: OnDeviceFound
  private lateinit var pairingListener: DataSharingStrategy.PairingListener
  private lateinit var operationListener: DataSharingStrategy.OperationListener
  private lateinit var coroutineScope: CoroutineScope

  private lateinit var wifiP2pInfo: WifiP2pInfo
  private lateinit var onConnectionInfo: (() -> Unit)
  private lateinit var wifiP2pGroup: WifiP2pGroup
  private lateinit var currentDevice: WifiP2pDevice

  private lateinit var socket: Socket
  private lateinit var dataInputStream: DataInputStream
  private lateinit var dataOutputStream: DataOutputStream
  private lateinit var device: DeviceInfo
  private lateinit var wifiP2pDevice: WifiP2pDevice
  private lateinit var syncPayload: PayloadContract<out Any>
  private val groupOwnerAddress: String = "00:00:5e:00:53:af"
  private lateinit var onSocketConnectionMade: (socket: Socket?) -> Unit
  private lateinit var expectedManifest: Manifest

  @Before
  fun setUp() {
    wifiP2pManager = mockk()
    wifiP2pChannel = mockk()
    wifiP2pReceiver = mockk()
    wifiP2pInfo = mockk()
    context = spyk(Activity())
    onDeviceFound = mockk()
    pairingListener = mockk()
    operationListener = mockk()
    device = mockk()
    syncPayload = mockk()
    coroutineScope = CoroutineScope(Dispatchers.IO)
    onSocketConnectionMade = mockk()
    expectedManifest = populateManifest()
    dataOutputStream = mockk()
    wifiDirectDataSharingStrategy = spyk(recordPrivateCalls = true)
    wifiDirectDataSharingStrategy.setActivity(context)
    wifiDirectDataSharingStrategy.setCoroutineScope(coroutineScope = coroutineScope)
    every { context.getSystemService(Context.WIFI_P2P_SERVICE) } returns wifiP2pManager

    wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }

    every { device.strategySpecificDevice } returns wifiP2pDevice
    every { wifiDirectDataSharingStrategy getProperty "wifiP2pManager" } returns wifiP2pManager
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pReceiver", wifiP2pReceiver)
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pChannel", wifiP2pChannel)
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pInfo", wifiP2pInfo)
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "dataOutputStream", dataOutputStream)
  }

  @Test
  fun `searchDevices() calls wifiP2pManager#initialize()`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.initialize(context, context.mainLooper, null) } returns wifiP2pChannel
    every { context.registerReceiver(any(), any()) } returns null
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    wifiDirectDataSharingStrategy.searchDevices(onDeviceFound, pairingListener)

    verify { wifiP2pManager.initialize(context, context.mainLooper, null) }
  }

  @Test
  fun `requestConnectionInfo() calls onConnectionInfoAvailable()`() {
    every { wifiP2pManager.requestConnectionInfo(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "requestConnectionInfo"
    )
    verify { wifiP2pManager.requestConnectionInfo(wifiP2pChannel, any()) }
  }

  @Test
  fun `listenForWifiP2pIntents() calls context#registerReceiver() with correct intent filter actions`() {
    every { context.registerReceiver(any(), any()) } returns null
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "listenForWifiP2pIntents"
    )

    val broadcastReceiverSlot = slot<BroadcastReceiver>()
    val intentFilterSlot = slot<IntentFilter>()
    verify { context.registerReceiver(capture(broadcastReceiverSlot), capture(intentFilterSlot)) }
    Assert.assertTrue(
      intentFilterSlot.captured.hasAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION)
    )
    Assert.assertTrue(
      intentFilterSlot.captured.hasAction(WifiP2pManager.WIFI_P2P_DISCOVERY_CHANGED_ACTION)
    )
    Assert.assertTrue(
      intentFilterSlot.captured.hasAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION)
    )
    Assert.assertTrue(
      intentFilterSlot.captured.hasAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION)
    )
    Assert.assertTrue(
      intentFilterSlot.captured.hasAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION)
    )
  }

  @Test
  fun `initiatePeerDiscovery() calls wifiP2pManager#discoverPeers()`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscovery",
      ReflectionHelpers.ClassParameter.from(OnDeviceFound::class.java, onDeviceFound)
    )

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.discoverPeers(wifiP2pChannel, capture(actionListenerSlot)) }
  }

  @Test
  fun `initiatePeerDiscovery() calls onSearchingFailed() and onDeviceFound#failed() when WifiP2pManager#ActionListener() returns failure`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    every { onDeviceFound.failed(any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscovery",
      ReflectionHelpers.ClassParameter.from(OnDeviceFound::class.java, onDeviceFound)
    )

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.discoverPeers(wifiP2pChannel, capture(actionListenerSlot)) }

    val exceptionSlot = slot<Exception>()
    actionListenerSlot.captured.onFailure(0)
    verify { onDeviceFound.failed(capture(exceptionSlot)) }
    Assert.assertEquals("0: Error", exceptionSlot.captured.message)
    verify { wifiDirectDataSharingStrategy.onSearchingFailed(capture(exceptionSlot)) }
    Assert.assertEquals("0: Error", exceptionSlot.captured.message)
  }

  @Test
  fun `initiatePeerDiscovery() calls logDebug() when WifiP2pManager#ActionListener() returns success`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscovery",
      ReflectionHelpers.ClassParameter.from(OnDeviceFound::class.java, onDeviceFound)
    )

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.discoverPeers(wifiP2pChannel, capture(actionListenerSlot)) }

    actionListenerSlot.captured.onSuccess()
    verify {
      wifiDirectDataSharingStrategy invoke
        "logDebug" withArguments
        listOf("Discovering peers successful")
    }
  }

  @Test
  fun `requestDeviceInfo() calls handleAccessFineLocationNotGranted() when ACCESS_FINE_LOCATION permission is denied`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED

    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "requestDeviceInfo"
    )

    verify { wifiDirectDataSharingStrategy invokeNoArgs "handleAccessFineLocationNotGranted" }
  }

  @Test
  fun `connect() calls handleAccessFineLocationNotGranted() when ACCESS_FINE_LOCATION permission is denied`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_DENIED

    wifiDirectDataSharingStrategy.connect(device = device, operationListener = operationListener)

    verify { wifiDirectDataSharingStrategy invokeNoArgs "handleAccessFineLocationNotGranted" }
  }

  @Test
  fun `connect() calls wifiP2pManager#connect() when ACCESS_FINE_LOCATION permission is granted`() {
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.connect(any(), any(), any()) } just runs
    every { operationListener.onSuccess(any()) } just runs

    Assert.assertFalse(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "paired"))
    Assert.assertNull(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "currentDevice"))

    wifiDirectDataSharingStrategy.connect(device = device, operationListener = operationListener)

    verify(exactly = 0) {
      wifiDirectDataSharingStrategy invokeNoArgs "handleAccessFineLocationNotGranted"
    }

    val wifiP2pConfigSlot = slot<WifiP2pConfig>()
    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify {
      wifiP2pManager.connect(
        wifiP2pChannel,
        capture(wifiP2pConfigSlot),
        capture(actionListenerSlot)
      )
    }

    actionListenerSlot.captured.onSuccess()

    Assert.assertTrue(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "paired"))
    val actualCurrentDevice =
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "currentDevice") as WifiP2pDevice
    Assert.assertEquals(wifiP2pDevice.deviceName, actualCurrentDevice.deviceName)
    Assert.assertEquals(wifiP2pDevice.deviceAddress, actualCurrentDevice.deviceAddress)
    verify {
      wifiDirectDataSharingStrategy invoke "onConnectionSucceeded" withArguments listOf(device)
    }
    verify { operationListener.onSuccess(device) }
  }

  @Test
  fun `disconnect() calls wifiP2pManager#removeGroup()`() {
    every { wifiP2pManager.removeGroup(any(), any()) } just runs
    every { operationListener.onSuccess(any()) } just runs
    every { operationListener.onFailure(any(), any()) } just runs
    every { wifiDirectDataSharingStrategy invokeNoArgs "closeSocketAndStreams" } returns null

    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "paired", true)
    Assert.assertTrue(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "paired"))

    wifiDirectDataSharingStrategy.disconnect(device = device, operationListener = operationListener)

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    Assert.assertTrue(
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "requestedDisconnection")
    )
    verify { wifiP2pManager.removeGroup(wifiP2pChannel, capture(actionListenerSlot)) }

    actionListenerSlot.captured.onSuccess()
    Assert.assertFalse(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "paired"))
    verify { operationListener.onSuccess(device) }
    verify {
      wifiDirectDataSharingStrategy invoke "onDisconnectSucceeded" withArguments listOf(device)
    }

    actionListenerSlot.captured.onFailure(0)
    val exceptionSlot = slot<Exception>()
    verify {
      wifiDirectDataSharingStrategy invoke
        "onDisconnectFailed" withArguments
        listOf(device, capture(exceptionSlot))
    }
    Assert.assertEquals("Error #0: Error", exceptionSlot.captured.message)
    verify { operationListener.onFailure(device, capture(exceptionSlot)) }
    Assert.assertEquals("Error #0: Error", exceptionSlot.captured.message)
  }

  @Test
  fun `send() calls operationListener#onFailure() when wifiP2pInfo() is null`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pInfo", null)
    every { operationListener.onFailure(any(), any()) } just runs
    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )

    val exceptionSlot = slot<Exception>()
    verify { operationListener.onFailure(device, capture(exceptionSlot)) }
    Assert.assertEquals(
      "Error sending to Google Pixel(00:00:5e:00:53:af): WifiP2PInfo is not available",
      exceptionSlot.captured.message
    )
  }

  @Ignore
  @Test
  fun `send() calls makeSocketConnections() when wifiP2pInfo() is not  null`() {
    coEvery { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) } just runs
    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )

    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
  }

  @Test
  fun `makeSocketConnections() calls requestConnectionInfo() when wifiP2pInfo is null`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pInfo", null)
    every { onSocketConnectionMade.invoke(any()) } just runs
    every { wifiDirectDataSharingStrategy invokeNoArgs "requestConnectionInfo" } returns null

    runBlocking {
      wifiDirectDataSharingStrategy.makeSocketConnections(groupOwnerAddress, onSocketConnectionMade)
    }

    verify { wifiDirectDataSharingStrategy invokeNoArgs "requestConnectionInfo" }

    verify { onSocketConnectionMade.invoke(any()) }
  }

  @Test
  fun `makeSocketConnections() calls acceptConnectionsToServerSocket() when socket is null and wifiP2pInfo#isGroupOwner is true`() {
    wifiP2pInfo.isGroupOwner = true
    every { onSocketConnectionMade.invoke(any()) } just runs
    every { wifiDirectDataSharingStrategy invokeNoArgs "acceptConnectionsToServerSocket" } returns
      null

    runBlocking {
      wifiDirectDataSharingStrategy.makeSocketConnections(groupOwnerAddress, onSocketConnectionMade)
    }

    verify { wifiDirectDataSharingStrategy invokeNoArgs "acceptConnectionsToServerSocket" }

    verify { onSocketConnectionMade.invoke(any()) }
  }

  @Test
  fun `makeSocketConnections() calls connectToServerSocket() when socket is null, wifiP2pInfo is not null and  wifiP2pInfo#isGroupOwner is false`() {
    wifiP2pInfo.isGroupOwner = false
    every { onSocketConnectionMade.invoke(any()) } just runs
    every {
      wifiDirectDataSharingStrategy invoke
        "connectToServerSocket" withArguments
        listOf(groupOwnerAddress)
    } returns null

    runBlocking {
      wifiDirectDataSharingStrategy.makeSocketConnections(groupOwnerAddress, onSocketConnectionMade)
    }

    verify {
      wifiDirectDataSharingStrategy invoke
        "connectToServerSocket" withArguments
        listOf(groupOwnerAddress)
    }

    verify { onSocketConnectionMade.invoke(any()) }
  }

  @Test
  fun `sendManifest() call dataOutputStream#writeUTF() and dataOutputStream#flush`() {
    val manifestString = Gson().toJson(expectedManifest)
    every { dataOutputStream.writeUTF(SyncPayloadType.MANIFEST.name) } just runs
    every { dataOutputStream.writeUTF(manifestString) } just runs
    every { dataOutputStream.flush() } just runs
    every { operationListener.onSuccess(device) } just runs

    wifiDirectDataSharingStrategy.sendManifest(device, expectedManifest, operationListener)

    verify { dataOutputStream.writeUTF(SyncPayloadType.MANIFEST.name) }
    verify { dataOutputStream.writeUTF(manifestString) }
    verify { dataOutputStream.flush() }
    verify { operationListener.onSuccess(device) }
  }

  private fun populateManifest(): Manifest {
    val dataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1)
    return Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
  }
}
