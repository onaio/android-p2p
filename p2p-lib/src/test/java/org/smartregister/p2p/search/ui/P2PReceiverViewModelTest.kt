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
import android.os.Looper
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.mockk.clearAllMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.CoroutineTestRule
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncReceiverHandler
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

@Config(shadows = [ShadowAppDatabase::class])
class P2PReceiverViewModelTest : RobolectricTest() {

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  private val entity = "Group"
  private val lastUpdatedAt = 12345L
  private lateinit var view: P2PDeviceSearchActivity
  private lateinit var dataSharingStrategy: DataSharingStrategy
  private lateinit var p2PReceiverViewModel: P2PReceiverViewModel
  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private lateinit var expectedManifest: Manifest
  private lateinit var expectedDeviceInfo: DeviceInfo
  private lateinit var receivedHistory: List<P2PReceivedHistory>
  private val appLifetimeKey = "ecd51f4c-ad4f-46a5-bda0-df38c5196aa8"

  @Before
  fun setUp() {
    clearAllMocks()
    view = mockk()
    dataSharingStrategy = mockk(relaxed = false)
    syncReceiverHandler = mockk()
    val p2pLibraryOptions =
      P2PLibrary.Options(RuntimeEnvironment.application, "", "username", mockk(), mockk())

    P2PLibrary.init(p2pLibraryOptions)

    val history = P2PReceivedHistory()
    history.entityType = entity
    history.appLifetimeKey = appLifetimeKey
    history.lastUpdatedAt = lastUpdatedAt
    receivedHistory = listOf(history)

    expectedDeviceInfo = populateDeviceInfo()
    every { dataSharingStrategy.getCurrentDevice() } answers { expectedDeviceInfo }
    p2PReceiverViewModel = spyk(P2PReceiverViewModel(view, dataSharingStrategy))
    ReflectionHelpers.setField(p2PReceiverViewModel, "syncReceiverHandler", syncReceiverHandler)
  }

  @Test
  fun `getSendingDeviceAppLifetimeKey() returns correct sending device appLifetime key`() {
    ReflectionHelpers.setField(p2PReceiverViewModel, "sendingDeviceAppLifetimeKey", appLifetimeKey)
    val sendingDeviceAppLifeTimeKey = p2PReceiverViewModel.getSendingDeviceAppLifetimeKey()
    Assert.assertEquals(appLifetimeKey, sendingDeviceAppLifeTimeKey)
  }

  @Test
  fun `processIncomingManifest() with manifest  calls syncReceiver#processManifest()`() {
    val dataType = DataType(name = "Patient", type = DataType.Filetype.JSON, position = 1)
    expectedManifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
    every { syncReceiverHandler.processManifest(manifest = expectedManifest) } just runs
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

    coVerify(exactly = 1) { view.showTransferCompleteDialog() }
  }

  @Test
  fun `handleDataTransferCompleteManifest() calls dataSharingStrategy#disconnect`() {
    coEvery { view.showTransferCompleteDialog() } answers { null }
    p2PReceiverViewModel.handleDataTransferCompleteManifest()
    verify(exactly = 1) { dataSharingStrategy.disconnect(device = expectedDeviceInfo, any()) }
  }

  private fun populateDeviceInfo(): DeviceInfo {
    val wifiP2pDevice =
      WifiP2pDevice().apply {
        this.deviceName = deviceName
        this.deviceAddress = deviceAddress
      }
    return WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
  }

  @Test
  fun `processChunkData() calls dataSharingStrategy#receive()`() {
    every { dataSharingStrategy.receive(any(), any(), any()) } just runs
    coEvery { syncReceiverHandler.processData(any()) } just runs

    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    val payloadReceiptListener = slot<DataSharingStrategy.PayloadReceiptListener>()

    p2PReceiverViewModel.processChunkData()

    verify {
      dataSharingStrategy.receive(
        expectedDeviceInfo,
        capture(payloadReceiptListener),
        capture(operationListenerSlot)
      )
    }
  }

  @Test
  fun `processChunkData() calls syncReceiverHandler#processData() when chunk data is received`() =
      runBlocking {
    ReflectionHelpers.setField(p2PReceiverViewModel, "syncReceiverHandler", syncReceiverHandler)
    every { dataSharingStrategy.receive(any(), any(), any()) } just runs

    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    val payloadReceiptListener = slot<DataSharingStrategy.PayloadReceiptListener>()

    p2PReceiverViewModel.processChunkData()

    verify {
      dataSharingStrategy.receive(
        expectedDeviceInfo,
        capture(payloadReceiptListener),
        capture(operationListenerSlot)
      )
    }

    val data =
      "[\n" +
        "    {\n" +
        "      \"use\": \"official\",\n" +
        "      \"value\": \"32343254\"\n" +
        "    },\n" +
        "    {\n" +
        "      \"use\": \"secondary\",\n" +
        "      \"value\": \"a05ff632-9b8d-45f7-81df-1f5a7191d69d\"\n" +
        "    }\n" +
        "  ]"
    val chunkData = BytePayload(data.toByteArray())
    payloadReceiptListener.captured.onPayloadReceived(chunkData)

    delay(1000)

    coVerify(exactly = 1) { syncReceiverHandler.processData(any()) }
  }

  @Test
  fun `processSenderDeviceDetails() calls checkIfDeviceKeyHasChanged() and context#showTransferProgressDialog()`() {
    every { dataSharingStrategy.receive(any(), any(), any()) } just runs

    val payloadSlot = slot<PayloadContract<out Any>>()
    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    val payloadReceiptListener = slot<DataSharingStrategy.PayloadReceiptListener>()

    p2PReceiverViewModel.processSenderDeviceDetails()

    verify {
      dataSharingStrategy.receive(
        expectedDeviceInfo,
        capture(payloadReceiptListener),
        capture(operationListenerSlot)
      )
    }

    val deviceInfo: MutableMap<String, String?> = HashMap()
    deviceInfo[Constants.BasicDeviceDetails.KEY_APP_LIFETIME_KEY] = appLifetimeKey

    val syncPayload =
      StringPayload(
        Gson().toJson(deviceInfo),
      )

    payloadReceiptListener.captured.onPayloadReceived(syncPayload)
    verify { p2PReceiverViewModel.checkIfDeviceKeyHasChanged(appLifetimeKey = appLifetimeKey) }
    coVerify { view.showTransferProgressDialog() }
  }

  @Test
  fun `checkIfDeviceKeyHasChanged() calls p2pReceiverViewModel#sendLastReceivedRecords() with empty list when received history is null`() =
      runBlocking {
    every { p2PReceiverViewModel.getReceivedHistory(appLifetimeKey) } returns null
    every { p2PReceiverViewModel.sendLastReceivedRecords(any()) } just runs

    p2PReceiverViewModel.checkIfDeviceKeyHasChanged(appLifetimeKey)

    delay(1000)

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    val receivedHistorySlot = slot<List<P2PReceivedHistory>>()
    coVerify { p2PReceiverViewModel.sendLastReceivedRecords(capture(receivedHistorySlot)) }
    Assert.assertTrue(receivedHistorySlot.captured.isEmpty())
  }

  @Test
  fun `checkIfDeviceKeyHasChanged() calls p2pReceiverViewModel#sendLastReceivedRecords() with retrieved received history value`() =
      runBlocking {
    coEvery { p2PReceiverViewModel.sendLastReceivedRecords(any()) } just runs
    every { p2PReceiverViewModel.getReceivedHistory(appLifetimeKey) } returns receivedHistory

    p2PReceiverViewModel.checkIfDeviceKeyHasChanged(appLifetimeKey)

    val receivedHistorySlot = slot<List<P2PReceivedHistory>>()
    coVerify { p2PReceiverViewModel.sendLastReceivedRecords(capture(receivedHistorySlot)) }

    delay(2000)

    Assert.assertEquals(1, receivedHistorySlot.captured.size)
    Assert.assertEquals(entity, receivedHistorySlot.captured[0].entityType)
    Assert.assertEquals(lastUpdatedAt, receivedHistorySlot.captured[0].lastUpdatedAt)
    Assert.assertEquals(appLifetimeKey, receivedHistorySlot.captured[0].appLifetimeKey)
  }

  @Test
  fun `sendLastReceivedRecords() sends correct received history data`() {
    every { dataSharingStrategy.send(any(), any(), any()) } just runs

    p2PReceiverViewModel.sendLastReceivedRecords(receivedHistory)

    val payloadSlot = slot<PayloadContract<out Any>>()
    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify {
      dataSharingStrategy.send(
        expectedDeviceInfo,
        capture(payloadSlot),
        capture(operationListenerSlot)
      )
    }

    val receivedHistoryListType = object : TypeToken<List<P2PReceivedHistory?>?>() {}.type
    val actualReceivedHistory: List<P2PReceivedHistory> =
      Gson().fromJson((payloadSlot.captured as StringPayload).string, receivedHistoryListType)

    Assert.assertEquals(entity, actualReceivedHistory[0].entityType)
    Assert.assertEquals(lastUpdatedAt, actualReceivedHistory[0].lastUpdatedAt)
    Assert.assertEquals(appLifetimeKey, actualReceivedHistory[0].appLifetimeKey)
  }

  @Test
  fun `Factory#constructor() should return instance of P2PReceiverViewModel`() {
    val wifiDirectDataSharingStrategy: WifiDirectDataSharingStrategy = mockk()
    every { wifiDirectDataSharingStrategy.setCoroutineScope(any()) } just runs

    Assert.assertNotNull(
      P2PReceiverViewModel.Factory(mockk(), wifiDirectDataSharingStrategy)
        .create(P2PReceiverViewModel::class.java)
    )
    Assert.assertTrue(
      P2PReceiverViewModel.Factory(mockk(), wifiDirectDataSharingStrategy)
        .create(P2PReceiverViewModel::class.java) is
        P2PReceiverViewModel
    )
  }
}
