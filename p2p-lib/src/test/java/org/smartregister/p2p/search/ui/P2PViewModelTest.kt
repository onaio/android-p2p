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
  lateinit var view: P2PDeviceSearchActivity
  lateinit var dataSharingStrategy: DataSharingStrategy
  lateinit var deviceInfo: DeviceInfo

  @Before
  fun setUp() {
    view = mockk(relaxed = true)
    dataSharingStrategy = mockk()

    p2PViewModel =
      spyk(
        P2PViewModel(
          view = view,
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
  fun `onEvent() calls view#requestLocationPermissionsAndEnableLocation() when P2PEvent is StartScanningEvent`() {
    every { view.requestLocationPermissionsAndEnableLocation() } just runs
    p2PViewModel.onEvent(P2PEvent.StartScanning)
    verify { view.requestLocationPermissionsAndEnableLocation() }
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
  fun `startScanning() should call view#keepScreenOn() and dataSharingStrategy#searchDevices()`() {
    every { view.keepScreenOn(true) } just runs
    every { dataSharingStrategy.searchDevices(any(), any()) } just runs

    p2PViewModel.startScanning()

    verify { view.keepScreenOn(true) }
    verify { dataSharingStrategy.searchDevices(any(), any()) }
  }

  @Test
  fun `startScanning() should update deviceList and p2pState to PAIR_DEVICES_FOUND when device role is SENDER when onDeviceFound#deviceFound is called`() {
    every { view.keepScreenOn(true) } just runs
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
  fun `startScanning() should update currentConnectedDevice, call view#processSenderDeviceDetails() when pairing#onSuccess is called and device role is receiver`() {
    every { view.keepScreenOn(true) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PViewModel.setCurrentConnectedDevice(null)
    p2PViewModel.deviceRole = DeviceRole.RECEIVER
    Assert.assertNull(p2PViewModel.getCurrentConnectedDevice())

    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertEquals(deviceInfo, p2PViewModel.getCurrentConnectedDevice())
    Assert.assertEquals(P2PState.WAITING_TO_RECEIVE_DATA, p2PViewModel.p2PState.getOrAwaitValue())
    verify { view.processSenderDeviceDetails() }
  }

  @Test
  fun `startScanning() should update currentConnectedDevice when pairing#onSuccess is called and device role is sender`() {
    every { view.keepScreenOn(true) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    p2PViewModel.setCurrentConnectedDevice(null)
    p2PViewModel.deviceRole = DeviceRole.SENDER
    Assert.assertNull(p2PViewModel.getCurrentConnectedDevice())

    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onSuccess(deviceInfo)

    Assert.assertEquals(deviceInfo, p2PViewModel.getCurrentConnectedDevice())
    Assert.assertNull(p2PViewModel.p2PState.value)
    verify(exactly = 0) { view.processSenderDeviceDetails() }
  }

  @Test
  fun `startScanning() should call view#keepScreenOn() and update p2pState to PROMPT_NEXT_TRANSFER when pairing#onFailure is called`() {
    every { view.keepScreenOn(true) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onFailure(deviceInfo, Exception(""))

    verify { view.keepScreenOn(false) }
    Assert.assertEquals(P2PState.PROMPT_NEXT_TRANSFER, p2PViewModel.p2PState.value)
  }

  @Test
  fun `startScanning() should call view#keepScreenOn() and update p2pState to TRANSFER_COMPLETE when pairing#onDisconnect is called`() {
    every { view.keepScreenOn(true) } just runs
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
    val pairingListenerSlot = slot<DataSharingStrategy.PairingListener>()
    every { dataSharingStrategy.searchDevices(any(), capture(pairingListenerSlot)) } just runs

    Assert.assertNull(p2PViewModel.p2PState.value)
    p2PViewModel.startScanning()

    pairingListenerSlot.captured.onDisconnected()

    verify { view.keepScreenOn(false) }
    Assert.assertEquals(P2PState.TRANSFER_COMPLETE, p2PViewModel.p2PState.value)
  }

  @Test
  fun `initChannel() should call dataSharingStrategy#initChannel()`() {
    every { dataSharingStrategy.initChannel(any(), any()) } just runs

    p2PViewModel.initChannel()

    verify { dataSharingStrategy.initChannel(any(), any()) }
  }
}
