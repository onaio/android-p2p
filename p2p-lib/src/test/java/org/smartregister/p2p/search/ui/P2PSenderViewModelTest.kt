/*
 * License text copyright (c) 2020 MariaDB Corporation Ab, All Rights Reserved.
 * “Business Source License” is a trademark of MariaDB Corporation Ab.
 *
 * Parameters
 *
 * Licensor:             Ona Systems, Inc.
 * Licensed Work:        android-p2p. The Licensed Work is (c) 2023 Ona Systems, Inc.
 * Additional Use Grant: You may make production use of the Licensed Work,
 *                       provided such use does not include offering the Licensed Work
 *                       to third parties on a hosted or embedded basis which is
 *                       competitive with Ona Systems' products.
 * Change Date:          Four years from the date the Licensed Work is published.
 * Change License:       MPL 2.0
 *
 * For information about alternative licensing arrangements for the Licensed Work,
 * please contact licensing@ona.io.
 *
 * Notice
 *
 * Business Source License 1.1
 *
 * Terms
 *
 * The Licensor hereby grants you the right to copy, modify, create derivative
 * works, redistribute, and make non-production use of the Licensed Work. The
 * Licensor may make an Additional Use Grant, above, permitting limited production use.
 *
 * Effective on the Change Date, or the fourth anniversary of the first publicly
 * available distribution of a specific version of the Licensed Work under this
 * License, whichever comes first, the Licensor hereby grants you rights under
 * the terms of the Change License, and the rights granted in the paragraph
 * above terminate.
 *
 * If your use of the Licensed Work does not comply with the requirements
 * currently in effect as described in this License, you must purchase a
 * commercial license from the Licensor, its affiliated entities, or authorized
 * resellers, or you must refrain from using the Licensed Work.
 *
 * All copies of the original and modified Licensed Work, and derivative works
 * of the Licensed Work, are subject to this License. This License applies
 * separately for each version of the Licensed Work and the Change Date may vary
 * for each version of the Licensed Work released by Licensor.
 *
 * You must conspicuously display this License on each original or modified copy
 * of the Licensed Work. If you receive the Licensed Work in original or
 * modified form from a third party, the terms and conditions set forth in this
 * License apply to your use of that work.
 *
 * Any use of the Licensed Work in violation of this License will automatically
 * terminate your rights under this License for the current and all other
 * versions of the Licensed Work.
 *
 * This License does not grant you any right in any trademark or logo of
 * Licensor or its affiliates (provided that you may use a trademark or logo of
 * Licensor as expressly required by this License).
 *
 * TO THE EXTENT PERMITTED BY APPLICABLE LAW, THE LICENSED WORK IS PROVIDED ON
 * AN “AS IS” BASIS. LICENSOR HEREBY DISCLAIMS ALL WARRANTIES AND CONDITIONS,
 * EXPRESS OR IMPLIED, INCLUDING (WITHOUT LIMITATION) WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE, NON-INFRINGEMENT, AND
 * TITLE.
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
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Shadows
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.CoroutineTestRule
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.data_sharing.DeviceInfo
import org.smartregister.p2p.data_sharing.Manifest
import org.smartregister.p2p.data_sharing.SyncSenderHandler
import org.smartregister.p2p.data_sharing.WifiDirectDataSharingStrategy
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.model.TransferProgress
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-05-2022. */
@Config(shadows = [ShadowAppDatabase::class])
internal class P2PSenderViewModelTest : RobolectricTest() {

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  lateinit var p2PSenderViewModel: P2PSenderViewModel
  lateinit var p2pSenderTransferDao: SenderTransferDao
  lateinit var p2pReceiverTransferDao: ReceiverTransferDao
  lateinit var syncSenderHandler: SyncSenderHandler
  lateinit var deviceInfo: DeviceInfo
  lateinit var dataSharingStrategy: DataSharingStrategy

  @Before
  internal fun setUp() {
    dataSharingStrategy = mockk()
    syncSenderHandler = mockk()
    p2PSenderViewModel =
      spyk(P2PSenderViewModel(dataSharingStrategy, coroutinesTestRule.testDispatcherProvider))
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

    p2PSenderViewModel.sendSyncComplete()

    Shadows.shadowOf(Looper.getMainLooper()).idle()

    verify(exactly = 1) { dataSharingStrategy.disconnect(deviceInfo, any()) }
  }

  @Test
  fun `sendChunkData() should call dataSharingStrategy#send()`() {
    val payload = StringPayload("[]")
    every { dataSharingStrategy.send(any(), any(), any()) } just runs

    p2PSenderViewModel.sendChunkData(payload)

    verify { dataSharingStrategy.send(deviceInfo, payload, any()) }
  }

  @Test
  fun `sendChunkData() should call syncSenderHandler#sendNextManifest() when sending succeeds`() {
    ReflectionHelpers.setField(p2PSenderViewModel, "syncSenderHandler", syncSenderHandler)
    val payload = StringPayload("[]")
    every { dataSharingStrategy.send(any(), any(), any()) } just runs
    every { syncSenderHandler.updateTotalSentRecordCount() } just runs
    coEvery { syncSenderHandler.sendNextManifest() } just runs

    p2PSenderViewModel.sendChunkData(payload)

    val operationListener = slot<DataSharingStrategy.OperationListener>()

    verify { dataSharingStrategy.send(deviceInfo, payload, capture(operationListener)) }
    operationListener.captured.onSuccess(deviceInfo)

    verify { runBlocking { syncSenderHandler.sendNextManifest() } }
    verify { syncSenderHandler.updateTotalSentRecordCount() }
  }

  @Test
  fun `sendManifest() should call dataSharingStrategy#sendManifest()`() {
    every { dataSharingStrategy.sendManifest(any(), any(), any()) } just runs

    val manifest =
      Manifest(
        DataType("Patient", DataType.Filetype.JSON, 0),
        250,
        50,
        recordCount = RecordCount(250L, hashMapOf())
      )

    p2PSenderViewModel.sendManifest(manifest)

    verify { dataSharingStrategy.sendManifest(deviceInfo, manifest, any()) }
  }

  @Test
  fun `getCurrentConnectedDevice() should call dataSharingStrategy#getCurrentConnectedDevice()`() {
    p2PSenderViewModel.getCurrentConnectedDevice()
    verify { dataSharingStrategy.getCurrentDevice() }
  }

  @Test
  fun `processReceivedHistory() should call syncSenderHandler#startSyncProcess() when syncPayload and JsonData are not empty`() {
    val syncPayload = StringPayload("[]")
    val dataTypes = TreeSet<DataType>()
    dataTypes.add(DataType("Patient", DataType.Filetype.JSON, 0))
    every { p2pSenderTransferDao.getP2PDataTypes() } returns dataTypes
    every { p2pSenderTransferDao.getTotalRecordCount(any()) } returns RecordCount(0L, hashMapOf())
    coEvery { syncSenderHandler.startSyncProcess() } just runs
    every { p2PSenderViewModel.createSyncSenderHandler(any(), any()) } returns syncSenderHandler

    p2PSenderViewModel.processReceivedHistory(syncPayload)

    Shadows.shadowOf(Looper.getMainLooper()).idle()
    coVerify { syncSenderHandler.startSyncProcess() }
  }

  @Test
  fun `processReceivedHistory() should call disconnect() when sync payload inside JsonData is null`() {
    val syncPayload = StringPayload("[]")

    every { p2pSenderTransferDao.getP2PDataTypes() } returns TreeSet<DataType>()
    every { p2pSenderTransferDao.getTotalRecordCount(any()) } returns RecordCount(0L, hashMapOf())
    every { p2PSenderViewModel.disconnect() } just runs

    p2PSenderViewModel.processReceivedHistory(syncPayload)

    verify { p2PSenderViewModel.disconnect() }
  }

  @Test
  fun `updateSenderSyncComplete() should call view#senderSyncComplete`() {
    val syncComplete = true

    p2PSenderViewModel.updateSenderSyncComplete(syncComplete)

    Shadows.shadowOf(Looper.getMainLooper()).idle()
  }

  @Test
  fun `Factory#constructor() should return instance of P2PSenderViewModel`() {
    val wifiDirectDataSharingStrategy: WifiDirectDataSharingStrategy = mockk()
    every { wifiDirectDataSharingStrategy.setCoroutineScope(any()) } just runs

    Assert.assertNotNull(
      P2PSenderViewModel.Factory(
          wifiDirectDataSharingStrategy,
          coroutinesTestRule.testDispatcherProvider
        )
        .create(P2PSenderViewModel::class.java)
    )
    Assert.assertTrue(
      P2PSenderViewModel.Factory(
          wifiDirectDataSharingStrategy,
          coroutinesTestRule.testDispatcherProvider
        )
        .create(P2PSenderViewModel::class.java) is
        P2PSenderViewModel
    )
  }

  @Test
  fun `updateTransferProgress() calls view#updateTransferProgress()`() {
    val expectedTransferProgress =
      TransferProgress(
        totalRecordCount = 40,
        transferredRecordCount = 10,
        percentageTransferred = 25
      )
    p2PSenderViewModel.updateTransferProgress(totalSentRecords = 10, totalRecords = 40)

    val transferProgressSlot = slot<TransferProgress>()
    verify { p2PSenderViewModel.postUIAction(any(), capture(transferProgressSlot)) }

    Assert.assertEquals(
      expectedTransferProgress.transferredRecordCount,
      transferProgressSlot.captured.transferredRecordCount
    )
    Assert.assertEquals(
      expectedTransferProgress.totalRecordCount,
      transferProgressSlot.captured.totalRecordCount
    )
    Assert.assertEquals(
      expectedTransferProgress.percentageTransferred,
      transferProgressSlot.captured.percentageTransferred
    )
  }
}
