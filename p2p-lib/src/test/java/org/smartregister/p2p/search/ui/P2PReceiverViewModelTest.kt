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
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.model.TransferProgress
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
    p2PReceiverViewModel =
      spyk(P2PReceiverViewModel(dataSharingStrategy, coroutinesTestRule.testDispatcherProvider))
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
    expectedManifest =
      Manifest(
        dataType = dataType,
        recordsSize = 25,
        payloadSize = 50,
        recordCount = RecordCount(50L, hashMapOf())
      )
    every { syncReceiverHandler.processManifest(manifest = expectedManifest) } just runs
    every { p2PReceiverViewModel.listenForIncomingManifest() } answers { expectedManifest }
    p2PReceiverViewModel.processIncomingManifest()
    verify(exactly = 1) { syncReceiverHandler.processManifest(manifest = expectedManifest) }
  }

  @Test
  fun `processIncomingManifest() with sync complete manifest value  calls p2PReceiverViewModel#handleDataTransferCompleteManifest()`() {
    val dataType =
      DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 1)
    expectedManifest =
      Manifest(
        dataType = dataType,
        recordsSize = 25,
        payloadSize = 50,
        recordCount = RecordCount(50L, hashMapOf())
      )
    every { p2PReceiverViewModel.listenForIncomingManifest() } answers { expectedManifest }
    p2PReceiverViewModel.processIncomingManifest()
    verify(exactly = 1) {
      p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
    }
  }

  @Test
  fun `listenForIncomingManifest() returns correct manifest`() {
    val dataType = DataType(name = "Group", type = DataType.Filetype.JSON, position = 0)
    expectedManifest =
      Manifest(
        dataType = dataType,
        recordsSize = 25,
        payloadSize = 50,
        recordCount = RecordCount(50L, hashMapOf())
      )
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
  fun `handleDataTransferCompleteManifest() calls postUIAction() with UIAction#SHOW_TRANSFER_COMPLETE_DIALOG and P2PState#TRANSFER_COMPLETE params`() {
    p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
    verify {
      p2PReceiverViewModel.postUIAction(
        UIAction.SHOW_TRANSFER_COMPLETE_DIALOG,
        P2PState.TRANSFER_COMPLETE
      )
    }
  }

  @Test
  fun `handleDataTransferCompleteManifest() calls dataSharingStrategy#disconnect`() {
    p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
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
  fun `processChunkData() calls syncReceiverHandler#processData() when chunk data is received`() {
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
    // coVerify { view.showTransferProgressDialog() }
  }

  @Test
  fun `checkIfDeviceKeyHasChanged() calls p2pReceiverViewModel#sendLastReceivedRecords() with empty list when received history is null`() {
    every { p2PReceiverViewModel.getReceivedHistory(appLifetimeKey) } returns null
    every { p2PReceiverViewModel.sendLastReceivedRecords(any()) } just runs

    p2PReceiverViewModel.checkIfDeviceKeyHasChanged(appLifetimeKey)

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
      P2PReceiverViewModel.Factory(
          wifiDirectDataSharingStrategy,
          coroutinesTestRule.testDispatcherProvider
        )
        .create(P2PReceiverViewModel::class.java)
    )
    Assert.assertTrue(
      P2PReceiverViewModel.Factory(
          wifiDirectDataSharingStrategy,
          coroutinesTestRule.testDispatcherProvider
        )
        .create(P2PReceiverViewModel::class.java) is
        P2PReceiverViewModel
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
    p2PReceiverViewModel.updateTransferProgress(totalReceivedRecords = 10, totalRecords = 40)

    val transferProgressSlot = slot<TransferProgress>()
    verify { p2PReceiverViewModel.postUIAction(any(), capture(transferProgressSlot)) }

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
