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
package org.smartregister.p2p.search.ui

import android.net.wifi.p2p.WifiP2pDevice
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlin.text.Typography.times
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.smartregister.p2p.CoroutineTestRule
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.OnDeviceFound
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.p2p.P2PEvent
import org.smartregister.p2p.search.ui.p2p.P2PViewModel

class P2PViewModelTest : RobolectricTest() {
  @get:Rule var coroutinesTestRule = CoroutineTestRule()
  @get:Rule var instantExecutorRule = InstantTaskExecutorRule()

  lateinit var p2PViewModel: P2PViewModel
  lateinit var dataSharingStrategy: DataSharingStrategy
  lateinit var deviceInfo: DeviceInfo

  @Before
  fun setUp() {
    dataSharingStrategy = mockk()

    p2PViewModel =
      spyk(
        P2PViewModel(
          dataSharingStrategy = dataSharingStrategy,
          dispatcherProvider = coroutinesTestRule.testDispatcherProvider
        )
      )

    val wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }
    deviceInfo = WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
  }

  @Test
  fun `onEvent() calls postUIAction() when P2PEvent is StartScanningEvent`() {
    p2PViewModel.onEvent(P2PEvent.StartScanning)
    verify { p2PViewModel.postUIAction(UIAction.REQUEST_LOCATION_PERMISSIONS_ENABLE_LOCATION) }
  }

  @Test
  fun `onEvent() calls connectToDevice() when P2PEvent is PairWithDevice`() {
    every { p2PViewModel.connectToDevice(deviceInfo) } just runs
    p2PViewModel.onEvent(P2PEvent.PairWithDevice(device = deviceInfo))
    verify { p2PViewModel.connectToDevice(deviceInfo) }
  }

  @Test
  fun `onEvent() updates p2PUiState#showP2PDialog to true when P2PEvent is CancelDataTransfer`() {
    Assert.assertFalse(p2PViewModel.p2PUiState.value.showP2PDialog)
    p2PViewModel.onEvent(P2PEvent.CancelDataTransfer)
    Assert.assertTrue(p2PViewModel.p2PUiState.value.showP2PDialog)
  }

  @Test
  fun `onEvent() calls cancelTransfer() when P2PEvent is ConnectionBreakConfirmed`() {
    every { p2PViewModel.cancelTransfer(any()) } just runs
    Assert.assertFalse(p2PViewModel.getRequestDisconnection())
    p2PViewModel.onEvent(P2PEvent.ConnectionBreakConfirmed)
    verify { p2PViewModel.cancelTransfer(P2PState.INITIATE_DATA_TRANSFER) }
    Assert.assertTrue(p2PViewModel.getRequestDisconnection())
  }

  @Test
  fun `onEvent() updates p2PUiState#showP2PDialog to false when P2PEvent is DismissConnectionBreakDialog`() {
    p2PViewModel.p2PUiState.value = p2PViewModel.p2PUiState.value.copy(showP2PDialog = true)
    Assert.assertTrue(p2PViewModel.p2PUiState.value.showP2PDialog)
    p2PViewModel.onEvent(P2PEvent.DismissConnectionBreakDialog)
    Assert.assertFalse(p2PViewModel.p2PUiState.value.showP2PDialog)
  }

  @Test
  fun `onEvent() updates p2PState to PROMPT_NEXT_TRANSFER when P2PEvent is DataTransferCompleteConfirmed`() {
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.onEvent(P2PEvent.DataTransferCompleteConfirmed)
    Assert.assertEquals(P2PState.PROMPT_NEXT_TRANSFER, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `onEvent() calls cancelTransfer() when P2PEvent is BottomSheetClosed`() {
    every { p2PViewModel.cancelTransfer(any()) } just runs
    p2PViewModel.onEvent(P2PEvent.BottomSheetClosed)
    verify { p2PViewModel.cancelTransfer(P2PState.PROMPT_NEXT_TRANSFER) }
  }

  @Test
  fun `startScanning() should call postUIAction() with UIAction#KEEP_SCREEN_ON and dataSharingStrategy#searchDevices()`() {
    every { dataSharingStrategy.searchDevices(any(), any()) } just runs

    p2PViewModel.startScanning()
    verify { p2PViewModel.postUIAction(UIAction.KEEP_SCREEN_ON, true) }
    verify { dataSharingStrategy.searchDevices(any(), any()) }
  }

  @Test
  fun `startScanning() should update deviceList and p2pState to PAIR_DEVICES_FOUND when device role is SENDER when onDeviceFound#deviceFound is called`() {
    val onDeviceFoundSlot = slot<OnDeviceFound>()
    every { dataSharingStrategy.searchDevices(capture(onDeviceFoundSlot), any()) } just runs
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    val devicesList = listOf(deviceInfo)
    onDeviceFoundSlot.captured.deviceFound(devicesList)

    Assert.assertEquals(devicesList, p2PViewModel.deviceList.getOrAwaitValue())
    Assert.assertEquals(P2PState.PAIR_DEVICES_FOUND, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `startScanning() should call postUIAction() with UIAction#KEEP_SCREEN_ON and update p2pState to PAIR_DEVICES_SEARCH_FAILED when onDeviceFound#failed is called`() {

    val onDeviceFoundSlot = slot<OnDeviceFound>()
    every { dataSharingStrategy.searchDevices(capture(onDeviceFoundSlot), any()) } just runs
    Assert.assertNull(p2PViewModel.p2PState.value)

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    onDeviceFoundSlot.captured.failed(java.lang.Exception())

    verify { p2PViewModel.postUIAction(UIAction.KEEP_SCREEN_ON, any()) }
    Assert.assertEquals(
      P2PState.PAIR_DEVICES_SEARCH_FAILED,
      p2PViewModel.p2PState.getOrAwaitValue()
    )
  }

  @Ignore("Fix  mocks not working  in lazy function")
  @Test
  fun `startScanning() should update currentConnectedDevice when pairing#onSuccess is called and device role is receiver`() {
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    every { dataSharingStrategy.isPairingInitiated() } returns true
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PViewModel.setCurrentConnectedDevice(null)
    p2PViewModel.deviceRole = DeviceRole.RECEIVER
    Assert.assertNull(p2PViewModel.getCurrentConnectedDevice())

    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertEquals(deviceInfo, p2PViewModel.getCurrentConnectedDevice())
    Assert.assertEquals(P2PState.WAITING_TO_RECEIVE_DATA, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Ignore("Fix  mocks not working  in lazy function")
  @Test
  fun `startScanning() should update currentConnectedDevice when pairing#onSuccess is called and device role is sender`() {
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    every { dataSharingStrategy.isPairingInitiated() } returns true
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PViewModel.setCurrentConnectedDevice(null)
    p2PViewModel.deviceRole = DeviceRole.SENDER
    Assert.assertNull(p2PViewModel.getCurrentConnectedDevice())

    p2PViewModel.startScanning()
    verify { p2PViewModel.postUIAction(UIAction.KEEP_SCREEN_ON, true) }

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertEquals(deviceInfo, p2PViewModel.getCurrentConnectedDevice())
  }

  @Test
  fun `startScanning() should call view#keepScreenOn() and update p2pState to PROMPT_NEXT_TRANSFER when pairing#onFailure is called`() {
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onFailure(deviceInfo, Exception(""))

    Assert.assertEquals(P2PState.PROMPT_NEXT_TRANSFER, p2PViewModel.p2PState.value)
  }

  @Test
  fun `startScanning() should calls postUIAction(UIAction#KEEP_SCREEN_ON, true) and update p2pState to DEVICE_DISCONNECTED when pairing#onDisconnect is called`() {
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    verify { p2PViewModel.postUIAction(UIAction.KEEP_SCREEN_ON, true) }

    pairingListenerSlot.captured.onDisconnected()

    Assert.assertEquals(P2PState.DEVICE_DISCONNECTED, p2PViewModel.p2PState.value)
  }

  @Test
  fun `initChannel() should call dataSharingStrategy#initChannel()`() {
    every { dataSharingStrategy.initChannel(any(), any()) } just runs

    p2PViewModel.initChannel()

    verify { dataSharingStrategy.initChannel(any(), any()) }
  }

  @Test
  fun `cancelTransfer() calls dataSharingStrategy#disconnect()`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PViewModel.cancelTransfer(P2PState.TRANSFER_CANCELLED)

    verify { dataSharingStrategy.disconnect(any(), any()) }
  }

  @Test
  fun `cancelTransfer() should update p2PState when dataSharingStrategy#disconnect() is successful`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PViewModel.cancelTransfer(P2PState.TRANSFER_CANCELLED)

    Assert.assertNull(p2PViewModel.p2PState.value)
    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.disconnect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onSuccess(deviceInfo)
    Assert.assertEquals(P2PState.TRANSFER_CANCELLED, p2PViewModel.p2PState.value)
  }

  @Test
  fun `connectToDevice() calls dataSharingStrategy#connect()`() {
    every { dataSharingStrategy.connect(any(), any()) } just runs

    p2PViewModel.connectToDevice(deviceInfo)

    verify { dataSharingStrategy.connect(any(), any()) }
  }

  @Test
  fun `connectToDevice() sets currentConnectedDevice, view#sendDeviceDetails() and updates p2PState on operationalListener success`() {
    every { dataSharingStrategy.connect(any(), any()) } just runs

    Assert.assertNull(p2PViewModel.getCurrentConnectedDevice())
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.connectToDevice(deviceInfo)

    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.connect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertEquals(deviceInfo, p2PViewModel.getCurrentConnectedDevice())
    Assert.assertEquals(P2PState.PREPARING_TO_SEND_DATA, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `connectToDevice() updates p2PState on operationalListener failure`() {
    every { dataSharingStrategy.connect(any(), any()) } just runs

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.connectToDevice(deviceInfo)

    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.connect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onFailure(deviceInfo, mockk())

    Assert.assertEquals(P2PState.CONNECT_TO_DEVICE_FAILED, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `showTransferCompleteDialog() updates p2PState to TRANSFER_COMPLETE`() {
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.showTransferCompleteDialog(p2PState = P2PState.TRANSFER_COMPLETE)
    Assert.assertEquals(P2PState.TRANSFER_COMPLETE, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `updateP2PState() updates p2PState with correct value`() {
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.updateP2PState(P2PState.TRANSFER_COMPLETE)
    Assert.assertEquals(P2PState.TRANSFER_COMPLETE, p2PViewModel.p2PState.getOrAwaitValue())
  }

  @Test
  fun `closeP2PScreen() calls postUIAction(UIAction#FINISH) when dataSharingStrategy#getCurrentDevice() is null`() {
    every { dataSharingStrategy.getCurrentDevice() } returns null
    p2PViewModel.closeP2PScreen()
    verify { p2PViewModel.postUIAction(UIAction.FINISH) }
  }

  @Test
  fun `closeP2PScreen() calls view#finish() when dataSharingStrategy#disconnect() is successful`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PViewModel.closeP2PScreen()

    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.disconnect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onSuccess(deviceInfo)
  }

  @Test
  fun `closeP2PScreen() calls view#finish() when dataSharingStrategy#disconnect() fails`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PViewModel.closeP2PScreen()

    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.disconnect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onFailure(deviceInfo, java.lang.Exception())
  }

  @Test
  fun `cancelTransfer() should update p2PState when dataSharingStrategy#disconnect() fails`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    p2PViewModel.cancelTransfer(P2PState.TRANSFER_CANCELLED)

    Assert.assertNull(p2PViewModel.p2PState.value)
    val operationalListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify { dataSharingStrategy.disconnect(any(), capture(operationalListenerSlot)) }

    operationalListenerSlot.captured.onFailure(deviceInfo, mockk())
    Assert.assertEquals(P2PState.TRANSFER_CANCELLED, p2PViewModel.p2PState.value)
  }

  @Test
  fun `cancelTransfer() should update p2PState when dataSharingStrategy#getCurrentDevice() returns null`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns null
    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.cancelTransfer(P2PState.TRANSFER_CANCELLED)

    verify(exactly = 0) { dataSharingStrategy.disconnect(any(), any()) }

    Assert.assertEquals(P2PState.TRANSFER_CANCELLED, p2PViewModel.p2PState.value)
  }
}
