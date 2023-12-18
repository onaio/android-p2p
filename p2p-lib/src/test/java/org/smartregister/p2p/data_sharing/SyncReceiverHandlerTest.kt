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
package org.smartregister.p2p.data_sharing

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
import org.json.JSONArray
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.CoroutineTestRule
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.P2pReceivedHistoryDao
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.model.P2PState
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

@Config(shadows = [ShadowAppDatabase::class])
class SyncReceiverHandlerTest : RobolectricTest() {

  @get:Rule var coroutinesTestRule = CoroutineTestRule()

  private lateinit var dataType: DataType
  private lateinit var jsonArray: JSONArray
  private lateinit var manifest: Manifest
  private lateinit var p2PReceiverViewModel: P2PReceiverViewModel
  private lateinit var syncReceiverHandler: SyncReceiverHandler
  private lateinit var receiverTransferDao: ReceiverTransferDao
  private lateinit var p2pReceivedHistoryDao: P2pReceivedHistoryDao
  private val lastUpdatedAt = 12345L
  private val groupResourceString = "group resource string"
  private val appLifetimeKey = "31986b0b-7cb6-4c6b-93c5-9c9da0ace19a"
  private val entityType = "Group"

  @Before
  fun setUp() {
    clearAllMocks()
    p2pReceivedHistoryDao = mockk()
    p2PReceiverViewModel = mockk()
    receiverTransferDao = mockk()

    jsonArray = JSONArray()
    jsonArray.put(groupResourceString)
    every { receiverTransferDao.receiveJson(any(), any()) } answers { lastUpdatedAt }
    P2PLibrary.init(
      P2PLibrary.Options(
        RuntimeEnvironment.application,
        "password",
        "demo",
        mockk(),
        receiverTransferDao
      )
    )

    dataType = DataType(name = "Group", type = DataType.Filetype.JSON, position = 0)
    manifest =
      Manifest(
        dataType = dataType,
        recordsSize = 25,
        payloadSize = 50,
        recordCount = RecordCount(50L, hashMapOf())
      )

    syncReceiverHandler =
      spyk(
        SyncReceiverHandler(
          p2PReceiverViewModel = p2PReceiverViewModel,
          coroutinesTestRule.testDispatcherProvider
        )
      )
    ReflectionHelpers.setField(syncReceiverHandler, "currentManifest", manifest)
  }

  @Test
  fun `processManifest() calls p2PReceiverViewModel#handleDataTransferCompleteManifest() when data type name is sync complete`() {
    dataType = DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 0)
    val manifest =
      Manifest(
        dataType = dataType,
        recordsSize = 25,
        payloadSize = 50,
        recordCount = RecordCount(50L, hashMapOf())
      )
    every { p2PReceiverViewModel.updateTransferProgress(any(), any()) } just runs
    every {
      p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
    } just runs

    syncReceiverHandler.processManifest(manifest = manifest)

    verify(exactly = 1) {
      p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.TRANSFER_COMPLETE)
    }
  }

  @Test
  fun `processManifest() calls p2PReceiverViewModel#handleDataTransferCompleteManifest() when data type name is data up to date`() {
    dataType =
      DataType(name = Constants.DATA_UP_TO_DATE, type = DataType.Filetype.JSON, position = 0)
    val manifest =
      Manifest(
        dataType = dataType,
        recordsSize = 0,
        payloadSize = 0,
        recordCount = RecordCount(0L, hashMapOf())
      )
    every { p2PReceiverViewModel.updateTransferProgress(any(), any()) } just runs
    every { p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.DATA_UP_TO_DATE) } just
      runs

    syncReceiverHandler.processManifest(manifest = manifest)

    verify(exactly = 1) {
      p2PReceiverViewModel.handleDataTransferCompleteManifest(P2PState.DATA_UP_TO_DATE)
    }
  }

  @Test
  fun `processManifest() calls p2PReceiverViewModel#processChunkData() when manifest has does not have sync complete data type name`() {
    every { p2PReceiverViewModel.updateTransferProgress(any(), any()) } just runs
    every { p2PReceiverViewModel.processChunkData() } just runs

    syncReceiverHandler.processManifest(manifest = manifest)

    verify(exactly = 1) { p2PReceiverViewModel.processChunkData() }
  }

  @Test
  fun `processData() calls addOrUpdateLastRecord(), updateTransferProgress() and p2PReceiverViewModel#processIncomingManifest()`() {
    every { p2PReceiverViewModel.processIncomingManifest() } just runs
    every { p2PReceiverViewModel.updateTransferProgress(any(), any()) } just runs
    coEvery { syncReceiverHandler.addOrUpdateLastRecord(any(), any()) } just runs
    ReflectionHelpers.setField(syncReceiverHandler, "totalRecordCount", 2)
    runBlocking { syncReceiverHandler.processData(jsonArray) }

    verify { p2PReceiverViewModel.updateTransferProgress(1L, 2L) }
    verify(exactly = 1) { p2PReceiverViewModel.processIncomingManifest() }
    coVerify(exactly = 1) { syncReceiverHandler.addOrUpdateLastRecord(any(), any()) }
  }

  @Test
  fun `addOrUpdateLastRecord calls p2pReceivedHistoryDao#updateReceivedHistory() which updates existing received history record for entity`() {
    val receivedHistory = P2PReceivedHistory()
    receivedHistory.entityType = entityType
    receivedHistory.appLifetimeKey = appLifetimeKey
    receivedHistory.lastUpdatedAt = 0
    every { p2pReceivedHistoryDao.updateReceivedHistory(any()) } just runs
    every { p2pReceivedHistoryDao.getHistory(any(), any()) } answers { receivedHistory }
    every { syncReceiverHandler invokeNoArgs "getP2pReceivedHistoryDao" } returns
      p2pReceivedHistoryDao
    every { p2PReceiverViewModel.getSendingDeviceAppLifetimeKey() } answers { appLifetimeKey }

    runBlocking { syncReceiverHandler.addOrUpdateLastRecord(entityType, lastUpdatedAt) }

    val receivedHistorySlot = slot<P2PReceivedHistory>()
    verify(exactly = 1) {
      p2pReceivedHistoryDao.updateReceivedHistory(capture(receivedHistorySlot))
    }
    Assert.assertEquals(lastUpdatedAt, receivedHistorySlot.captured.lastUpdatedAt)
    Assert.assertEquals(entityType, receivedHistorySlot.captured.entityType)
  }

  @Test
  fun `addOrUpdateLastRecord calls p2pReceivedHistoryDao#addReceivedHistory() which creates new received history record for entity when p2p received history is null`() {
    every { p2pReceivedHistoryDao.getHistory(any(), any()) } answers { null }
    every { syncReceiverHandler invokeNoArgs "getP2pReceivedHistoryDao" } returns
      p2pReceivedHistoryDao
    every { p2PReceiverViewModel.getSendingDeviceAppLifetimeKey() } answers { appLifetimeKey }
    every { p2pReceivedHistoryDao.addReceivedHistory(any()) } just runs

    runBlocking { syncReceiverHandler.addOrUpdateLastRecord(entityType, lastUpdatedAt) }

    val receivedHistorySlot = slot<P2PReceivedHistory>()
    verify(exactly = 1) { p2pReceivedHistoryDao.addReceivedHistory(capture(receivedHistorySlot)) }
    Assert.assertEquals(lastUpdatedAt, receivedHistorySlot.captured.lastUpdatedAt)
    Assert.assertEquals(entityType, receivedHistorySlot.captured.entityType)
    Assert.assertEquals(appLifetimeKey, receivedHistorySlot.captured.appLifetimeKey)
  }
}
