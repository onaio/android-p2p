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
import org.junit.Ignore
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.P2pReceivedHistoryDao
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PReceiverViewModel
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

@Config(shadows = [ShadowAppDatabase::class])
class SyncReceiverHandlerTest : RobolectricTest() {

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
    manifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)

    syncReceiverHandler = spyk(SyncReceiverHandler(p2PReceiverViewModel = p2PReceiverViewModel))
    ReflectionHelpers.setField(syncReceiverHandler, "currentManifest", manifest)
  }

  @Test
  fun `processManifest() calls p2PReceiverViewModel#handleDataTransferCompleteManifest() when data type name is sync complete`() {
    dataType = DataType(name = Constants.SYNC_COMPLETE, type = DataType.Filetype.JSON, position = 0)
    val manifest = Manifest(dataType = dataType, recordsSize = 25, payloadSize = 50)
    every { p2PReceiverViewModel.upDateProgress(any(), any()) } just runs
    every { p2PReceiverViewModel.handleDataTransferCompleteManifest() } just runs

    syncReceiverHandler.processManifest(manifest = manifest)

    verify(exactly = 1) {
      p2PReceiverViewModel.upDateProgress("Transferring %,d records", manifest.recordsSize)
    }
    verify(exactly = 1) { p2PReceiverViewModel.handleDataTransferCompleteManifest() }
  }

  @Test
  fun `processManifest() calls p2PReceiverViewModel#processChunkData() when manifest has does not have sync complete data type name`() {
    every { p2PReceiverViewModel.upDateProgress(any(), any()) } just runs
    every { p2PReceiverViewModel.processChunkData() } just runs

    syncReceiverHandler.processManifest(manifest = manifest)

    verify(exactly = 1) {
      p2PReceiverViewModel.upDateProgress("Transferring %,d records", manifest.recordsSize)
    }
    verify(exactly = 1) { p2PReceiverViewModel.processChunkData() }
  }

  @Test
  fun `processData() calls updateLastRecord() and p2PReceiverViewModel#processIncomingManifest()`() {
    every { p2PReceiverViewModel.processIncomingManifest() } just runs
    coEvery { syncReceiverHandler.updateLastRecord(any(), any()) } just runs
    runBlocking { syncReceiverHandler.processData(jsonArray) }

    verify(exactly = 1) { p2PReceiverViewModel.processIncomingManifest() }
    coVerify(exactly = 1) { syncReceiverHandler.updateLastRecord(any(), any()) }
  }

  @Test
  @Ignore("Fix mocking of P2pReceivedHistoryDao")
  fun `updateLastRecord() updates existing received history record for entity`() {
    val receivedHistory: P2PReceivedHistory = mockk()
    every { receivedHistory.entityType } answers { entityType }
    every { receivedHistory.lastUpdatedAt } answers { 0L }
    every { receivedHistory.appLifetimeKey } answers { appLifetimeKey }
    every { p2pReceivedHistoryDao.getHistory(any(), any()) } answers { receivedHistory }
    ReflectionHelpers.setField(syncReceiverHandler, "p2pReceivedHistoryDao", p2pReceivedHistoryDao)
    every { p2PReceiverViewModel.getSendingDeviceAppLifetimeKey() } answers { appLifetimeKey }

    runBlocking { syncReceiverHandler.updateLastRecord(entityType, lastUpdatedAt) }

    val receivedHistorySlot = slot<P2PReceivedHistory>()
    verify(exactly = 1) {
      p2pReceivedHistoryDao.updateReceivedHistory(capture(receivedHistorySlot))
    }
    Assert.assertEquals(lastUpdatedAt, receivedHistorySlot.captured.lastUpdatedAt)
  }

  @Test
  @Ignore("Fix mocking of P2pReceivedHistoryDao")
  fun `updateLastRecord() creates new received history record for entity`() {
    every { p2pReceivedHistoryDao.getHistory(any(), any()) } answers { null }
    ReflectionHelpers.setField(syncReceiverHandler, "p2pReceivedHistoryDao", p2pReceivedHistoryDao)
    every { p2PReceiverViewModel.getSendingDeviceAppLifetimeKey() } answers { appLifetimeKey }

    runBlocking { syncReceiverHandler.updateLastRecord(entityType, lastUpdatedAt) }

    val receivedHistorySlot = slot<P2PReceivedHistory>()
    verify(exactly = 1) { p2pReceivedHistoryDao.addReceivedHistory(capture(receivedHistorySlot)) }
    Assert.assertEquals(lastUpdatedAt, receivedHistorySlot.captured.lastUpdatedAt)
    Assert.assertEquals(entityType, receivedHistorySlot.captured.entityType)
    Assert.assertEquals(appLifetimeKey, receivedHistorySlot.captured.appLifetimeKey)
  }
}