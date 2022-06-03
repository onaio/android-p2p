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
import android.os.Build
import com.google.gson.Gson
import io.mockk.CapturingSlotMatcher
import io.mockk.EqMatcher
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import io.mockk.verifySequence
import java.io.DataInputStream
import java.io.DataOutputStream
import java.net.Socket
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.TestCoroutineDispatcher
import kotlinx.coroutines.test.TestCoroutineScope
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.WifiP2pBroadcastReceiver
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.payload.SyncPayloadType
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.contract.P2PManagerListener
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
  private lateinit var payloadReceiptListener: DataSharingStrategy.PayloadReceiptListener
  private lateinit var coroutineScope: CoroutineScope

  private lateinit var wifiP2pInfo: WifiP2pInfo
  private lateinit var onConnectionInfo: (() -> Unit)
  private lateinit var wifiP2pGroup: WifiP2pGroup

  private lateinit var socket: Socket
  private lateinit var dataInputStream: DataInputStream
  private lateinit var dataOutputStream: DataOutputStream
  private lateinit var device: DeviceInfo
  private lateinit var wifiP2pDevice: WifiP2pDevice
  private lateinit var syncPayload: PayloadContract<out Any>
  private val groupOwnerAddress: String = "00:00:5e:00:53:af"
  private lateinit var onSocketConnectionMade: (socket: Socket?) -> Unit
  private lateinit var expectedManifest: Manifest
  private var exception = Exception("Error #0: Error")

  @Before
  fun setUp() {
    wifiP2pManager = mockk()
    wifiP2pChannel = mockk()
    wifiP2pReceiver = mockk()
    wifiP2pInfo = mockk()
    wifiP2pGroup = mockk()
    socket = mockk()
    context = spyk(Activity())
    onDeviceFound = mockk()
    pairingListener = mockk()
    operationListener = mockk()
    payloadReceiptListener = mockk()
    device = mockk()
    syncPayload = mockk()
    coroutineScope = TestCoroutineScope(TestCoroutineDispatcher())
    onSocketConnectionMade = mockk()
    expectedManifest = populateManifest()
    dataOutputStream = mockk()
    dataInputStream = mockk()
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
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "dataInputStream", dataInputStream)
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
  fun `searchDevices() initializes wifiP2pBroadcastReceiver`() {
    mockkConstructor(WifiP2pBroadcastReceiver::class)
    every { context.checkPermission(any(), any(), any()) } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.initialize(context, context.mainLooper, null) } returns wifiP2pChannel
    every { context.registerReceiver(any(), any()) } returns null
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs

    wifiDirectDataSharingStrategy.searchDevices(onDeviceFound, pairingListener)

    val p2PManagerListenerSlot = slot<P2PManagerListener>()
    verify {
      constructedWith<WifiP2pBroadcastReceiver>(
        EqMatcher(wifiP2pManager),
        EqMatcher(wifiP2pChannel),
        CapturingSlotMatcher(p2PManagerListenerSlot, P2PManagerListener::class),
        EqMatcher(context)
      )
    }
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
  fun `initiatePeerDiscoveryOnceAccessFineLocationGranted() calls initiatePeerDiscovery() when ACCESS_FINE_LOCATION permission is granted`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscoveryOnceAccessFineLocationGranted"
    )

    verify {
      wifiDirectDataSharingStrategy invoke
        "initiatePeerDiscovery" withArguments
        listOf(any<OnDeviceFound>())
    }
  }

  @Test
  fun `initiatePeerDiscoveryOnceAccessFineLocationGranted() calls requestAccessFineLocationIfNotGranted() when ACCESS_FINE_LOCATION permission is denied and build version code is greater than 23`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_DENIED
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 27)
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    every {
      wifiDirectDataSharingStrategy invokeNoArgs "requestAccessFineLocationIfNotGranted"
    } returns null
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscoveryOnceAccessFineLocationGranted"
    )

    verify { wifiDirectDataSharingStrategy invokeNoArgs "requestAccessFineLocationIfNotGranted" }
  }

  @Test
  fun `initiatePeerDiscoveryOnceAccessFineLocationGranted() calls handleMinimumSDKVersionNotMet() when ACCESS_FINE_LOCATION permission is denied and build version code is less than 23`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_DENIED
    ReflectionHelpers.setStaticField(Build.VERSION::class.java, "SDK_INT", 22)
    every { wifiP2pManager.discoverPeers(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "initiatePeerDiscoveryOnceAccessFineLocationGranted"
    )

    verify { wifiDirectDataSharingStrategy.handleMinimumSDKVersionNotMet(Build.VERSION_CODES.M) }
  }

  @Test
  fun `initiatePeerDiscovery() calls wifiP2pManager#discoverPeers()`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
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
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
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
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
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
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_DENIED

    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "requestDeviceInfo"
    )

    verify { wifiDirectDataSharingStrategy invokeNoArgs "handleAccessFineLocationNotGranted" }
  }

  @Test
  fun `connect() calls handleAccessFineLocationNotGranted() when ACCESS_FINE_LOCATION permission is denied`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_DENIED

    wifiDirectDataSharingStrategy.connect(device = device, operationListener = operationListener)

    verify { wifiDirectDataSharingStrategy invokeNoArgs "handleAccessFineLocationNotGranted" }
  }

  @Test
  fun `connect() calls wifiP2pManager#connect() when ACCESS_FINE_LOCATION permission is granted`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_GRANTED
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

  @Test
  fun `send() calls makeSocketConnections() when wifiP2pInfo() is not  null`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    coEvery { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) } just runs
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress

    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )

    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
  }

  @Test
  fun `send() calls dataOutputStream#flush, dataOutputStream#writeUTF and operationListener#onSuccess() when dataOutputStream is not null and payload datatype is string`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    every { syncPayload.getDataType() } returns SyncPayloadType.STRING
    every { syncPayload.getData() } returns "some data"
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress
    coEvery { dataOutputStream.writeUTF(any()) } just runs
    coEvery { dataOutputStream.flush() } just runs
    coEvery { operationListener.onSuccess(device) } just runs

    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )

    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
    verifySequence {
      dataOutputStream.writeUTF(SyncPayloadType.STRING.name)
      dataOutputStream.flush()
      dataOutputStream.writeUTF("some data")
      dataOutputStream.flush()
    }
    coVerify { operationListener.onSuccess(device) }
  }

  @Test
  fun `send() calls operationListener#onFailure() when dataOutputStream is null and payload datatype is string`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "dataOutputStream", null)
    every { syncPayload.getDataType() } returns SyncPayloadType.STRING
    every { syncPayload.getData() } returns "some data"
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress
    coEvery { operationListener.onFailure(device, any()) } just runs

    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )

    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
    var exceptionSlot = slot<Exception>()
    coVerify { operationListener.onFailure(device, capture(exceptionSlot)) }
    Assert.assertEquals("DataOutputStream is null", exceptionSlot.captured.message)
  }

  @Test
  fun `send() calls dataOutputStream#writeUTF, dataOutputStream#writeLong, dataOutputStream#write and operationListener#onSuccess() when dataOutputStream is not null and payload datatype is bytes`() {
    val payload = "some data"
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    every { syncPayload.getDataType() } returns SyncPayloadType.BYTES
    every { syncPayload.getData() } returns payload.toByteArray()
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress
    coEvery { dataOutputStream.writeUTF(any()) } just runs
    coEvery { dataOutputStream.writeLong(any()) } just runs
    coEvery { dataOutputStream.write(any(), any(), any()) } just runs
    coEvery { operationListener.onSuccess(device) } just runs

    wifiDirectDataSharingStrategy.send(
      device = device,
      operationListener = operationListener,
      syncPayload = syncPayload
    )
    val payloadSize = payload.toByteArray().size
    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
    coVerify { dataOutputStream.writeUTF(SyncPayloadType.BYTES.name) }
    coVerify { dataOutputStream.writeLong(payloadSize.toLong()) }
    coVerify { dataOutputStream.write(payload.toByteArray(), 0, payloadSize) }
    coVerify { operationListener.onSuccess(device) }
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

  @Test
  fun `receive() calls operationListener#onFailure() when wifiP2pInfo() is null`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pInfo", null)
    every { operationListener.onFailure(any(), any()) } just runs
    wifiDirectDataSharingStrategy.receive(
      device = device,
      payloadReceiptListener = payloadReceiptListener,
      operationListener = operationListener
    )

    val exceptionSlot = slot<Exception>()
    verify { operationListener.onFailure(device, capture(exceptionSlot)) }
    Assert.assertEquals(
      "Error receiving from Google Pixel(00:00:5e:00:53:af): WifiP2PInfo is not available",
      exceptionSlot.captured.message
    )
  }

  @Test
  fun `receive() calls makeSocketConnections() when wifiP2pInfo() is not null `() {
    coEvery { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) } just runs
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress

    wifiDirectDataSharingStrategy.receive(
      device = device,
      payloadReceiptListener = payloadReceiptListener,
      operationListener = operationListener
    )

    coVerify { wifiDirectDataSharingStrategy.makeSocketConnections(any(), any()) }
  }

  @Test
  fun `receive() calls dataInputStream#readUTF() and payloadReceiptListener#onPayloadReceived() when payload data type is string`() {
    val stringPayload = "some data"
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    every { dataInputStream.readUTF() } returnsMany
      (listOf(SyncPayloadType.STRING.name, stringPayload))
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress

    wifiDirectDataSharingStrategy.receive(
      device = device,
      payloadReceiptListener = payloadReceiptListener,
      operationListener = operationListener
    )

    coVerify(exactly = 2) { dataInputStream.readUTF() }
    val stringPayloadSlot = slot<StringPayload>()
    coVerify { payloadReceiptListener.onPayloadReceived(capture(stringPayloadSlot)) }
    Assert.assertEquals(stringPayload, stringPayloadSlot.captured.getData())
  }

  @Test
  fun `receive() calls dataInputStream#readLong(), dataInputStream#read(), logDebug() and payloadReceiptListener#onPayloadReceived() when payload data type is bytes`() {
    val bytePayload = "some data".toByteArray()
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    every { dataInputStream.readUTF() } returns SyncPayloadType.BYTES.name
    every { dataInputStream.readLong() } returns bytePayload.size.toLong()

    val byteArraySlot = slot<ByteArray>()
    val offsetSlot = slot<Int>()
    every { dataInputStream.read(capture(byteArraySlot), capture(offsetSlot), any()) } answers
      {
        if (offsetSlot.captured == 0) {
          bytePayload.forEachIndexed { index, byte -> byteArraySlot.captured[index] = byte }

          bytePayload.size
        } else {
          -1
        }
      }

    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress

    wifiDirectDataSharingStrategy.receive(
      device = device,
      payloadReceiptListener = payloadReceiptListener,
      operationListener = operationListener
    )

    coVerify { dataInputStream.readLong() }
    coVerify { dataInputStream.read(any(), 0, bytePayload.size) }
    verify { wifiDirectDataSharingStrategy invoke "logDebug" withArguments listOf("file size 0") }

    val bytePayloadSlot = slot<BytePayload>()
    verify { payloadReceiptListener.onPayloadReceived(capture(bytePayloadSlot)) }
    Assert.assertArrayEquals(bytePayload, bytePayloadSlot.captured.payload)
  }

  @Test
  fun `receive() calls operationListener#onFailure when payload data type is unknown`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    coEvery { wifiDirectDataSharingStrategy.getCurrentDevice() } returns device
    every { dataInputStream.readUTF() } returns "int"
    every { wifiDirectDataSharingStrategy invokeNoArgs "getGroupOwnerAddress" } returns
      groupOwnerAddress

    wifiDirectDataSharingStrategy.receive(
      device = device,
      payloadReceiptListener = payloadReceiptListener,
      operationListener = operationListener
    )

    coVerify { dataInputStream.readUTF() }
    val exceptionSlot = slot<Exception>()
    coVerify { operationListener.onFailure(device, capture(exceptionSlot)) }
    Assert.assertEquals("Unknown datatype: int", exceptionSlot.captured.message)
  }

  @Test
  fun `receiveManifest() call dataOutputStream#readUTF()`() {
    val manifestString = Gson().toJson(expectedManifest)
    every { dataInputStream.readUTF() } returnsMany
      (listOf(SyncPayloadType.MANIFEST.name, manifestString))

    val actualManifest = wifiDirectDataSharingStrategy.receiveManifest(device, operationListener)

    verify(exactly = 2) { dataInputStream.readUTF() }
    Assert.assertEquals(expectedManifest.dataType.type, actualManifest!!.dataType.type)
    Assert.assertEquals(expectedManifest.dataType.name, actualManifest!!.dataType.name)
    Assert.assertEquals(expectedManifest.dataType.position, actualManifest!!.dataType.position)
    Assert.assertEquals(expectedManifest.payloadSize, actualManifest!!.payloadSize)
    Assert.assertEquals(expectedManifest.recordsSize, actualManifest!!.recordsSize)
  }

  @Test
  fun `onErrorOccurred() calls closeSocketAndStreams()`() {
    every { wifiDirectDataSharingStrategy.closeSocketAndStreams() } just runs
    wifiDirectDataSharingStrategy.onErrorOccurred(exception)
    verify { wifiDirectDataSharingStrategy.closeSocketAndStreams() }
  }

  @Test
  fun `onConnectionFailed() calls closeSocketAndStreams()`() {
    every { wifiDirectDataSharingStrategy.closeSocketAndStreams() } just runs
    wifiDirectDataSharingStrategy.onConnectionFailed(device, exception)
    verify { wifiDirectDataSharingStrategy.closeSocketAndStreams() }
  }

  @Test
  fun `onDisconnectSucceeded() calls closeSocketAndStreams()`() {
    every { wifiDirectDataSharingStrategy.closeSocketAndStreams() } just runs
    wifiDirectDataSharingStrategy.onDisconnectSucceeded(device)
    verify { wifiDirectDataSharingStrategy.closeSocketAndStreams() }
  }

  @Test
  fun `getCurrentDevice() returns correct WifiDirectDevice`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "currentDevice", wifiP2pDevice)
    val actualCurrentDevice = wifiDirectDataSharingStrategy.getCurrentDevice()
    Assert.assertTrue(actualCurrentDevice is WifiDirectDataSharingStrategy.WifiDirectDevice)
    Assert.assertEquals(actualCurrentDevice!!.strategySpecificDevice, wifiP2pDevice)
  }

  @Test
  fun `getCurrentDevice() returns null when currentDevice is null`() {
    val actualCurrentDevice = wifiDirectDataSharingStrategy.getCurrentDevice()
    Assert.assertNull(actualCurrentDevice)
  }

  @Test
  fun `onResume() calls listenForWifiP2pIntents(), initiatePeerDiscoveryOnceAccessFineLocationGranted(), requestDeviceInfo() and requestConnectionInfo() when isScanning is true`() {
    every { wifiDirectDataSharingStrategy invokeNoArgs "listenForWifiP2pIntents" } returns null
    every {
      wifiDirectDataSharingStrategy invokeNoArgs
        "initiatePeerDiscoveryOnceAccessFineLocationGranted"
    } returns null
    every { wifiDirectDataSharingStrategy invokeNoArgs "requestDeviceInfo" } returns null
    every { wifiDirectDataSharingStrategy invokeNoArgs "requestConnectionInfo" } returns null

    wifiDirectDataSharingStrategy.onResume(isScanning = true)

    verify { wifiDirectDataSharingStrategy invokeNoArgs "listenForWifiP2pIntents" }
    verify {
      wifiDirectDataSharingStrategy invokeNoArgs
        "initiatePeerDiscoveryOnceAccessFineLocationGranted"
    }
    verify { wifiDirectDataSharingStrategy invokeNoArgs "requestDeviceInfo" }
    verify { wifiDirectDataSharingStrategy invokeNoArgs "requestConnectionInfo" }
  }

  @Test
  fun `onPause() calls context#unregisterReceiver()`() {
    every { context.unregisterReceiver(wifiP2pReceiver) } just runs

    wifiDirectDataSharingStrategy.onPause()

    verify { context.unregisterReceiver(wifiP2pReceiver) }
  }

  @Test
  fun `onConnectionInfoAvailable() sets wifiP2pGroup and currentDevice`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "wifiP2pInfo", null)
    Assert.assertNull(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "currentDevice"))
    Assert.assertNull(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "wifiP2pInfo"))
    Assert.assertNull(ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "wifiP2pGroup"))
    every { wifiP2pGroup.isGroupOwner } returns true
    every { wifiP2pGroup.clientList } returns listOf(wifiP2pDevice)

    wifiP2pInfo.groupFormed = true
    wifiP2pInfo.isGroupOwner = true
    wifiDirectDataSharingStrategy.onConnectionInfoAvailable(wifiP2pInfo, wifiP2pGroup)

    verify {
      wifiDirectDataSharingStrategy invoke
        "logDebug" withArguments
        listOf("Connection info available: groupFormed = true, isGroupOwner = true")
    }
    Assert.assertEquals(
      wifiP2pGroup,
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "wifiP2pGroup")
    )
    Assert.assertEquals(
      wifiP2pDevice,
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "currentDevice")
    )
  }

  @Test
  fun `onConnectionInfoAvailable() calls onConnectionInfoAvailable() with provided wifiP2pInfo and null wifiP2pGroup`() {
    wifiDirectDataSharingStrategy.onConnectionInfoAvailable(wifiP2pInfo)
    verify { wifiDirectDataSharingStrategy.onConnectionInfoAvailable(wifiP2pInfo, null) }
  }

  @Test
  fun `stopSearchingDevices() calls wifiP2pManager#stopPeerDiscovery() when isSearchingDevices flag is true`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "isSearchingDevices", true)
    Assert.assertTrue(
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "isSearchingDevices")
    )
    every { wifiP2pManager.stopPeerDiscovery(any(), any()) } just runs
    every { operationListener.onSuccess(null) } just runs
    every { operationListener.onFailure(any(), any()) } just runs

    wifiDirectDataSharingStrategy.stopSearchingDevices(operationListener)

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, capture(actionListenerSlot)) }

    Assert.assertFalse(
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "isSearchingDevices")
    )
  }

  @Test
  fun `stopSearchingDevices() calls logDebug() and operationListener#onSuccess() when isSearchingDevices flag is true and actionListener#onSuccess() is called`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "isSearchingDevices", true)
    Assert.assertTrue(
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "isSearchingDevices")
    )
    every { wifiP2pManager.stopPeerDiscovery(any(), any()) } just runs
    every { operationListener.onSuccess(null) } just runs
    every { operationListener.onFailure(any(), any()) } just runs

    wifiDirectDataSharingStrategy.stopSearchingDevices(operationListener)

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, capture(actionListenerSlot)) }
    actionListenerSlot.captured.onSuccess()
    verify { operationListener.onSuccess(null) }
    verify {
      wifiDirectDataSharingStrategy invoke
        "logDebug" withArguments
        listOf("Successfully stopped peer discovery")
    }
  }

  @Test
  fun `stopSearchingDevices() calls operationListener#onFailure() when isSearchingDevices flag is true and actionListener#onFailure() is called`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "isSearchingDevices", true)
    Assert.assertTrue(
      ReflectionHelpers.getField(wifiDirectDataSharingStrategy, "isSearchingDevices")
    )
    every { wifiP2pManager.stopPeerDiscovery(any(), any()) } just runs
    every { operationListener.onSuccess(null) } just runs
    every { operationListener.onFailure(any(), any()) } just runs

    wifiDirectDataSharingStrategy.stopSearchingDevices(operationListener)

    val actionListenerSlot = slot<WifiP2pManager.ActionListener>()
    verify { wifiP2pManager.stopPeerDiscovery(wifiP2pChannel, capture(actionListenerSlot)) }
    actionListenerSlot.captured.onFailure(0)
    val exceptionSlot = slot<Exception>()
    verify { operationListener.onFailure(null, capture(exceptionSlot)) }
    Assert.assertEquals(
      "Error occurred trying to stop peer discovery Error",
      exceptionSlot.captured.message
    )
  }

  @Test
  fun `closeSocketAndStreams() calls stopSearchingDevices(), dataInputStream#close(), dataOutputStream#close, dataOutputStream#flush and socket#close`() {
    ReflectionHelpers.setField(wifiDirectDataSharingStrategy, "socket", socket)
    every { wifiDirectDataSharingStrategy.stopSearchingDevices(null) } just runs
    every { dataInputStream.close() } just runs
    every { dataOutputStream.flush() } just runs
    every { dataOutputStream.close() } just runs
    every { socket.close() } just runs

    wifiDirectDataSharingStrategy.closeSocketAndStreams()
    verify { wifiDirectDataSharingStrategy.stopSearchingDevices(null) }
    verify { dataInputStream.close() }
    verify { dataOutputStream.flush() }
    verify { dataOutputStream.close() }
    verify { socket.close() }
  }

  @Test
  fun `getWifiP2pReason() returns correct reason for error code 0`() {
    val response =
      ReflectionHelpers.callInstanceMethod<String>(
        wifiDirectDataSharingStrategy,
        "getWifiP2pReason",
        ReflectionHelpers.ClassParameter.from(Int::class.java, 0)
      )
    Assert.assertEquals("Error", response)
  }

  @Test
  fun `getWifiP2pReason() returns correct reason for error code 1`() {
    val response =
      ReflectionHelpers.callInstanceMethod<String>(
        wifiDirectDataSharingStrategy,
        "getWifiP2pReason",
        ReflectionHelpers.ClassParameter.from(Int::class.java, 1)
      )
    Assert.assertEquals("Unsupported", response)
  }

  @Test
  fun `getWifiP2pReason() returns correct reason for error code 2`() {
    val response =
      ReflectionHelpers.callInstanceMethod<String>(
        wifiDirectDataSharingStrategy,
        "getWifiP2pReason",
        ReflectionHelpers.ClassParameter.from(Int::class.java, 2)
      )
    Assert.assertEquals("Busy", response)
  }

  @Test
  fun `getWifiP2pReason() returns correct reason for error code not in (0,1,2)`() {
    val response =
      ReflectionHelpers.callInstanceMethod<String>(
        wifiDirectDataSharingStrategy,
        "getWifiP2pReason",
        ReflectionHelpers.ClassParameter.from(Int::class.java, 7)
      )
    Assert.assertEquals("Unknown", response)
  }

  @Test
  fun `requestAccessFineLocationIfNotGranted() should call logDebug() and context#requestPermissions() when ACCESS_FINE_LOCATION permission is denied`() {
    every {
      context.checkPermission(android.Manifest.permission.ACCESS_FINE_LOCATION, any(), any())
    } returns PackageManager.PERMISSION_DENIED
    every { context.requestPermissions(any(), any()) } just runs
    ReflectionHelpers.callInstanceMethod<WifiDirectDataSharingStrategy>(
      wifiDirectDataSharingStrategy,
      "requestAccessFineLocationIfNotGranted"
    )
    verify {
      wifiDirectDataSharingStrategy invoke
        "logDebug" withArguments
        listOf("Wifi P2P: Requesting access fine location permission")
    }

    verify {
      context.requestPermissions(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION), any())
    }
  }

  private fun populateManifest(): Manifest {
    val dataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1)
    return Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
  }
}
