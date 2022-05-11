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
package org.smartregister.p2p.ui

import android.net.wifi.p2p.WifiP2pDevice
import io.mockk.clearAllMocks
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.CoroutineTestRule
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

class P2PReceiverViewModelTest : RobolectricTest() {

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  private lateinit var context: P2PDeviceSearchActivity
  private lateinit var dataSharingStrategy: DataSharingStrategy
  private lateinit var p2PReceiverViewModel: P2PReceiverViewModel
  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private lateinit var expectedManifest: Manifest
  private lateinit var expectedDeviceInfo: DeviceInfo

  @Before
  fun setUp() {
    clearAllMocks()
    context = mockk()
    dataSharingStrategy = mockk(relaxed = false)
    syncReceiverHandler = mockk(relaxed = true)

    expectedDeviceInfo = populateDeviceInfo()
    every { dataSharingStrategy.getCurrentDevice() } answers { expectedDeviceInfo }
    p2PReceiverViewModel = spyk(P2PReceiverViewModel(context, dataSharingStrategy))
    ReflectionHelpers.setField(p2PReceiverViewModel, "syncReceiverHandler", syncReceiverHandler)
  }

  @Test
  fun `getSendingDeviceAppLifetimeKey() returns correct sending device appLifetime key`() {
    val appLifetimeKey = "ecd51f4c-ad4f-46a5-bda0-df38c5196aa8"
    ReflectionHelpers.setField(p2PReceiverViewModel, "sendingDeviceAppLifetimeKey", appLifetimeKey)
    val sendingDeviceAppLifeTimeKey = p2PReceiverViewModel.getSendingDeviceAppLifetimeKey()
    Assert.assertNotNull(sendingDeviceAppLifeTimeKey)
    Assert.assertEquals(appLifetimeKey, sendingDeviceAppLifeTimeKey)
  }

  @Test
  fun `processIncomingManifest() with manifest  calls syncReceiver#processManifest()`() {
    val dataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1)
    expectedManifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
    every { p2PReceiverViewModel.listenForIncomingManifest() } answers { expectedManifest }
    p2PReceiverViewModel.processIncomingManifest()
    verify(exactly = 1) { syncReceiverHandler.processManifest(manifest = expectedManifest) }
  }

  @Test
  fun `processIncomingManifest() with sync complete manifest value  calls p2PReceiverViewModel#handleDataTransferCompleteManifest()`() {
    val dataType =
      DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 1)
    expectedManifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
    every { p2PReceiverViewModel.listenForIncomingManifest() } answers { expectedManifest }
    p2PReceiverViewModel.processIncomingManifest()
    verify(exactly = 1) { p2PReceiverViewModel.handleDataTransferCompleteManifest() }
  }

  @Test
  fun `listenForIncomingManifest() returns correct manifest`() {
    val dataType = DataType(name = "Group", type = DataType.Filetype.JSON, position = 0)
    expectedManifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
    every { dataSharingStrategy.receiveManifest(device = any(), operationListener = any()) } answers
      {
        expectedManifest
      }
    val actualManifest = p2PReceiverViewModel.listenForIncomingManifest()
    Assert.assertEquals(expectedManifest.recordsSize, actualManifest!!.recordsSize)
    Assert.assertEquals(expectedManifest.payloadSize, actualManifest!!.payloadSize)
    Assert.assertEquals(expectedManifest.dataType.name, actualManifest!!.dataType.name)
    Assert.assertEquals(expectedManifest.dataType.type, actualManifest!!.dataType.type)
    Assert.assertEquals(expectedManifest.dataType.position, actualManifest!!.dataType.position)
  }

  @Test
  fun `handleDataTransferCompleteManifest() calls showTransferCompleteDialog()`() {

    p2PReceiverViewModel.handleDataTransferCompleteManifest()

    coVerify(exactly = 1) { context.showTransferCompleteDialog() }
  }

  private fun populateDeviceInfo(): DeviceInfo {
    val wifiP2pDevice =
      WifiP2pDevice().apply {
        this.deviceName = deviceName
        this.deviceAddress = deviceAddress
      }
    return WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
  }
}
