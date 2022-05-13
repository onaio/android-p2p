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
import androidx.test.core.app.ApplicationProvider
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
import java.util.TreeSet
import org.json.JSONArray
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncSenderHandler
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-05-2022. */
@Config(shadows = [ShadowAppDatabase::class])
internal class P2PSenderViewModelTest : RobolectricTest() {

  lateinit var p2PSenderViewModel: P2PSenderViewModel
  lateinit var view: P2pModeSelectContract.View
  lateinit var p2pSenderTransferDao: SenderTransferDao
  lateinit var p2pReceiverTransferDao: ReceiverTransferDao
  lateinit var syncSenderHandler: SyncSenderHandler
  lateinit var deviceInfo: DeviceInfo
  lateinit var dataSharingStrategy: DataSharingStrategy

  @Before
  internal fun setUp() {
    view = mockk()
    dataSharingStrategy = mockk()
    syncSenderHandler = mockk()
    p2PSenderViewModel = spyk(P2PSenderViewModel(view, dataSharingStrategy))
    ReflectionHelpers.setField(p2PSenderViewModel, "syncSenderHandler", syncSenderHandler)

    p2pReceiverTransferDao = mockk()
    p2pSenderTransferDao = mockk()
    val p2pLibraryOptions =
      P2PLibrary.Options(
        ApplicationProvider.getApplicationContext(),
        "",
        "username",
        p2pSenderTransferDao,
        p2pReceiverTransferDao
      )

    P2PLibrary.init(p2pLibraryOptions)

    val wifiP2pDevice =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }
    deviceInfo = WifiDirectDataSharingStrategy.WifiDirectDevice(wifiP2pDevice)
    every { view.getCurrentConnectedDevice() } returns deviceInfo
    every { dataSharingStrategy.getCurrentDevice() } returns deviceInfo
  }

  @Test
  fun `sendDeviceDetails() should call dataSharingStrategy#send() with device details map`() {
    every { dataSharingStrategy.send(deviceInfo, any(), any()) } just runs
    p2PSenderViewModel.sendDeviceDetails(deviceInfo)

    val payloadSlot = slot<PayloadContract<out Any>>()

    verify { dataSharingStrategy.send(deviceInfo, capture(payloadSlot), any()) }
    var map: MutableMap<String, String?> = HashMap()
    Gson().fromJson((payloadSlot.captured as StringPayload).string, map.javaClass)
  }

  @Test
  fun `sendDeviceDetails() should call dataSharingStrategy#receive() when device details map is sent successfully`() {
    every { dataSharingStrategy.send(deviceInfo, any(), any()) } just runs
    every { dataSharingStrategy.receive(deviceInfo, any(), any()) } just runs
    p2PSenderViewModel.sendDeviceDetails(deviceInfo)

    val payloadSlot = slot<PayloadContract<out Any>>()
    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify {
      dataSharingStrategy.send(deviceInfo, capture(payloadSlot), capture(operationListenerSlot))
    }

    operationListenerSlot.captured.onSuccess(deviceInfo)

    verify { dataSharingStrategy.receive(deviceInfo, any(), any()) }
  }

  @Test
  fun `sendDeviceDetails() should call processReceivedHistory() when device details map is sent successfully and received history is received`() {
    every { dataSharingStrategy.send(deviceInfo, any(), any()) } just runs
    every { dataSharingStrategy.receive(deviceInfo, any(), any()) } just runs
    every { p2PSenderViewModel.processReceivedHistory(any()) } just runs
    p2PSenderViewModel.sendDeviceDetails(deviceInfo)

    val payloadSlot = slot<PayloadContract<out Any>>()
    val operationListenerSlot = slot<DataSharingStrategy.OperationListener>()
    verify {
      dataSharingStrategy.send(deviceInfo, capture(payloadSlot), capture(operationListenerSlot))
    }

    operationListenerSlot.captured.onSuccess(deviceInfo)

    val payloadReceiptListenerSlot = slot<DataSharingStrategy.PayloadReceiptListener>()

    verify { dataSharingStrategy.receive(deviceInfo, capture(payloadReceiptListenerSlot), any()) }

    val receivedHistoryPayload = StringPayload("")
    payloadReceiptListenerSlot.captured.onPayloadReceived(receivedHistoryPayload)

    verify { p2PSenderViewModel.processReceivedHistory(receivedHistoryPayload) }
  }

  @Test
  fun `requestSyncParams() should call dataSharingStrategy#send() with SEND_SYNC_PARAMS payload`() {
    every { dataSharingStrategy.send(any(), any(), any()) } just runs
    p2PSenderViewModel.requestSyncParams(deviceInfo)

    val payloadSlot = slot<PayloadContract<out Any>>()
    verify { dataSharingStrategy.send(deviceInfo, capture(payloadSlot), any()) }

    Assert.assertEquals(
      Gson().toJson(Constants.SEND_SYNC_PARAMS),
      (payloadSlot.captured as StringPayload).string
    )
  }

  @Test
  fun `sendSyncComplete() should call dataSharingStrategy#disconnect() and view#showTransferCompleteDialog()`() {
    every { dataSharingStrategy.disconnect(any(), any()) } just runs
    every { view.showTransferCompleteDialog() } just runs

    p2PSenderViewModel.sendSyncComplete()

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify(exactly = 1) { dataSharingStrategy.disconnect(deviceInfo, any()) }
    verify(exactly = 1) { view.showTransferCompleteDialog() }

    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun `sendChunkData() should call dataSharingStrategy#send()`() {
    val payload = StringPayload("[]")
    every { dataSharingStrategy.send(any(), any(), any()) } just runs

    p2PSenderViewModel.sendChunkData(payload)

    verify { dataSharingStrategy.send(deviceInfo, payload, any()) }
  }

  @Ignore
  @Test
  fun `sendChunkData() should call syncSenderHandler#sendNextManifest() when sending succeeds`() {
    ReflectionHelpers.setField(p2PSenderViewModel, "syncSenderHandler", syncSenderHandler)
    val payload = StringPayload("[]")
    every { dataSharingStrategy.send(any(), any(), any()) } just runs
    coEvery { syncSenderHandler.sendNextManifest() } just runs

    p2PSenderViewModel.sendChunkData(payload)

    val operationListener = slot<DataSharingStrategy.OperationListener>()

    verify { dataSharingStrategy.send(deviceInfo, payload, capture(operationListener)) }
    operationListener.captured.onSuccess(deviceInfo)

    coVerify { syncSenderHandler.sendNextManifest() }
  }

  @Test
  fun `sendManifest() should call dataSharingStrategy#sendManifest()`() {
    every { dataSharingStrategy.sendManifest(any(), any(), any()) } just runs

    val manifest = Manifest(DataType("Patient", DataType.Filetype.JSON, 0), 250, 50)

    p2PSenderViewModel.sendManifest(manifest)

    verify { dataSharingStrategy.sendManifest(deviceInfo, manifest, any()) }
  }

  @Test
  fun `getCurrentConnectedDevice() should call view#getCurrentConnectedDevice()`() {
    p2PSenderViewModel.getCurrentConnectedDevice()

    verify { view.getCurrentConnectedDevice() }
  }

  @Ignore
  @Test
  fun `processReceivedHistory() should call #startSyncProcess() and #sendManifest when syncPayload and JsonData are not empty`() {
    val syncPayload = StringPayload("[]")
    val dataTypes = TreeSet<DataType>()

    dataTypes.add(DataType("Patient", DataType.Filetype.JSON, 0))

    every { p2pSenderTransferDao.getJsonData(any(), any(), any()) } returns JsonData(JSONArray(), 0)
    every { p2pSenderTransferDao.getP2PDataTypes() } returns dataTypes
    coEvery { syncSenderHandler.startSyncProcess() } just runs
    every { dataSharingStrategy.sendManifest(any(), any(), any()) } just runs
    every { view.senderSyncComplete(any()) } just runs

    p2PSenderViewModel.processReceivedHistory(syncPayload)

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    verify(exactly = 1) { p2PSenderViewModel.sendManifest(any()) }
  }

  @Test
  fun `processReceivedHistory() should call #sendSyncComplete() when sync payload inside JsonData is null`() {
    val syncPayload = StringPayload("[]")

    every { p2pSenderTransferDao.getP2PDataTypes() } returns TreeSet<DataType>()

    p2PSenderViewModel.processReceivedHistory(syncPayload)

    verify { p2PSenderViewModel.sendSyncComplete() }
  }

  @Test fun updateSenderSyncComplete() {}
}
