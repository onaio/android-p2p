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
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.spyk
import io.mockk.verify
import java.util.TreeSet
import org.json.JSONArray
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.data.JsonData
import org.smartregister.p2p.search.ui.P2PSenderViewModel
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants

@Config(shadows = [ShadowAppDatabase::class])
class SyncSenderHandlerTest : RobolectricTest() {

  private lateinit var dataSyncOrder: TreeSet<DataType>
  private lateinit var p2PSenderViewModel: P2PSenderViewModel
  private lateinit var receivedHistory: List<P2PReceivedHistory>
  private lateinit var syncSenderHandler: SyncSenderHandler
  private lateinit var senderTransferDao: SenderTransferDao
  private val entity = "Group"
  private val lastUpdatedAt = 12345L
  private val groupResourceString =
    "{\n" +
      "  \"resourceType\": \"Group\",\n" +
      "  \"id\": \"213fdbd5-5323-415d-81b2-d58dcf218093\",\n" +
      "  \"identifier\": [\n" +
      "    {\n" +
      "      \"use\": \"official\",\n" +
      "      \"value\": \"11\"\n" +
      "    },\n" +
      "    {\n" +
      "      \"use\": \"secondary\",\n" +
      "      \"value\": \"9ebd45bb-bd80-40de-be39-a30053daa601\"\n" +
      "    }\n" +
      "  ],\n" +
      "  \"active\": true,\n" +
      "  \"type\": \"person\",\n" +
      "  \"code\": {\n" +
      "    \"coding\": [\n" +
      "      {\n" +
      "        \"system\": \"https://www.snomed.org\",\n" +
      "        \"code\": \"35359004\",\n" +
      "        \"display\": \"Family\"\n" +
      "      }\n" +
      "    ]\n" +
      "  },\n" +
      "  \"name\": \"Moonwalker\",\n" +
      "  \"managingEntity\": {\n" +
      "    \"reference\": \"Organization/105\"\n" +
      "  }\n" +
      "}"

  @Before
  fun setUp() {
    clearAllMocks()
    senderTransferDao = mockk()
    val jsonArray = JSONArray()
    jsonArray.put(groupResourceString)
    val jsonData = JsonData(jsonArray = jsonArray, highestRecordId = lastUpdatedAt)
    every { senderTransferDao.getJsonData(any(), any(), any()) } answers { jsonData }
    P2PLibrary.init(
      P2PLibrary.Options(
        RuntimeEnvironment.application,
        "password",
        "demo",
        senderTransferDao,
        mockk()
      )
    )
    p2PSenderViewModel = mockk()

    val history = mockk<P2PReceivedHistory>()
    every { history.appLifetimeKey } answers { "appLifetimeKey" }
    every { history.entityType } answers { entity }
    every { history.lastUpdatedAt } answers { lastUpdatedAt }
    receivedHistory = listOf(history)
    dataSyncOrder = getDataTypes()
    every { p2PSenderViewModel.sendManifest(any()) } answers { null }
    syncSenderHandler =
      spyk(
        SyncSenderHandler(
          dataSyncOrder = dataSyncOrder,
          p2PSenderViewModel = p2PSenderViewModel,
          receivedHistory = receivedHistory
        )
      )
  }

  @Test
  fun `startSyncProcess() calls generateRecordsToSend() and sendNextManifest()`() {
    syncSenderHandler.startSyncProcess()
    verify(exactly = 1) { syncSenderHandler.generateRecordsToSend() }
    verify(exactly = 1) { syncSenderHandler.sendNextManifest() }
  }

  @Test
  fun `sendJsonDataManifest() calls p2PSenderViewModel#sendManifest() when data exists for specific data type`() {

    val originalRemainingLastRecordIds =
      ReflectionHelpers.getField<HashMap<String, Long>>(syncSenderHandler, "remainingLastRecordIds")
    Assert.assertNull(originalRemainingLastRecordIds.get(entity))
    val originalAwaitingDataTypeRecordsBatchSize =
      ReflectionHelpers.getField<Int>(syncSenderHandler, "awaitingDataTypeRecordsBatchSize")
    Assert.assertEquals(0, originalAwaitingDataTypeRecordsBatchSize)

    syncSenderHandler.sendJsonDataManifest(
      DataType(name = entity, type = DataType.Filetype.JSON, position = 0)
    )

    val updatedRemainingLastRecordIds =
      ReflectionHelpers.getField<HashMap<String, Long>>(syncSenderHandler, "remainingLastRecordIds")
    Assert.assertEquals(lastUpdatedAt, updatedRemainingLastRecordIds.get(entity))
    val updatedAwaitingDataTypeRecordsBatchSize =
      ReflectionHelpers.getField<Int>(syncSenderHandler, "awaitingDataTypeRecordsBatchSize")
    Assert.assertEquals(1, updatedAwaitingDataTypeRecordsBatchSize)

    val saveSlot = slot<Manifest>()
    verify(exactly = 1) { p2PSenderViewModel.sendManifest(capture(saveSlot)) }
    val capturedManifest = saveSlot.captured
    Assert.assertEquals(1, capturedManifest.recordsSize)
    Assert.assertEquals(entity, capturedManifest.dataType.name)
    Assert.assertEquals(0, capturedManifest.dataType.position)
    Assert.assertEquals(DataType.Filetype.JSON, capturedManifest.dataType.type)
  }

  @Test
  fun `sendJsonDataManifest() calls sendNextManifest() when data does not exists for specific data type`() {

    val jsonArray = JSONArray()
    val jsonData = JsonData(jsonArray = jsonArray, highestRecordId = lastUpdatedAt)
    val groupDataType = DataType(name = entity, DataType.Filetype.JSON, 0)
    every { senderTransferDao.getJsonData(any(), any(), any()) } answers { jsonData }
    every { syncSenderHandler.sendNextManifest() } answers { null }

    val originalRemainingLastRecordIds =
      ReflectionHelpers.getField<HashMap<String, Long>>(syncSenderHandler, "remainingLastRecordIds")
    Assert.assertNull(originalRemainingLastRecordIds.get(entity))
    val originalAwaitingDataTypeRecordsBatchSize =
      ReflectionHelpers.getField<Int>(syncSenderHandler, "awaitingDataTypeRecordsBatchSize")
    Assert.assertEquals(0, originalAwaitingDataTypeRecordsBatchSize)
    val originalDataSyncOrder =
      ReflectionHelpers.getField<TreeSet<DataType>>(syncSenderHandler, "dataSyncOrder")
    Assert.assertTrue(originalDataSyncOrder.contains(groupDataType))

    syncSenderHandler.sendJsonDataManifest(
      DataType(name = entity, type = DataType.Filetype.JSON, position = 0)
    )

    val updatedRemainingLastRecordIds =
      ReflectionHelpers.getField<HashMap<String, Long>>(syncSenderHandler, "remainingLastRecordIds")
    Assert.assertNull(updatedRemainingLastRecordIds.get(entity))
    val updatedAwaitingDataTypeRecordsBatchSize =
      ReflectionHelpers.getField<Int>(syncSenderHandler, "awaitingDataTypeRecordsBatchSize")
    Assert.assertEquals(0, updatedAwaitingDataTypeRecordsBatchSize)
    val updatedDataSyncOrder =
      ReflectionHelpers.getField<TreeSet<DataType>>(syncSenderHandler, "dataSyncOrder")
    Assert.assertFalse(updatedDataSyncOrder.contains(groupDataType))

    verify(exactly = 1) { syncSenderHandler.sendNextManifest() }
  }

  @Test
  fun `sendNextManifest() calls sendJsonDataManifest() when dataSyncOrder is not empty`() {
    syncSenderHandler.sendNextManifest()
    verify(exactly = 1) { syncSenderHandler.sendJsonDataManifest(dataSyncOrder.first()) }
  }

  @Test
  fun `sendNextManifest() calls p2PSenderViewModel#sendManifest() and p2PSenderViewModel#updateSenderSyncComplete() when dataSyncOrder is empty`() {
    dataSyncOrder.clear()
    ReflectionHelpers.setField(syncSenderHandler, "dataSyncOrder", dataSyncOrder)
    every { p2PSenderViewModel.updateSenderSyncComplete(any()) } answers { null }
    every { p2PSenderViewModel.sendManifest(any()) } answers { null }

    syncSenderHandler.sendNextManifest()
    val saveBooleanSlot = slot<Boolean>()
    verify(exactly = 1) { p2PSenderViewModel.updateSenderSyncComplete(capture(saveBooleanSlot)) }
    Assert.assertTrue(saveBooleanSlot.captured)
    val saveSlot = slot<Manifest>()
    verify(exactly = 1) { p2PSenderViewModel.sendManifest(capture(saveSlot)) }
    val capturedManifest = saveSlot.captured
    Assert.assertEquals(0, capturedManifest.recordsSize)
    Assert.assertEquals(Constants.SYNC_COMPLETE, capturedManifest.dataType.name)
    Assert.assertEquals(0, capturedManifest.dataType.position)
    Assert.assertEquals(DataType.Filetype.JSON, capturedManifest.dataType.type)
  }

  @Test
  fun `processManifestSent() calls p2PSenderViewModel#sendChunkData() when sendingSyncCompleteManifest is false`() {
    val awaitingPayload = BytePayload(groupResourceString.toByteArray())
    ReflectionHelpers.setField(syncSenderHandler, "awaitingPayload", awaitingPayload)
    every { p2PSenderViewModel.sendChunkData(any()) } answers { null }
    syncSenderHandler.processManifestSent()
    verify(exactly = 1) { p2PSenderViewModel.sendChunkData(awaitingPayload) }
  }

  @Test
  fun `processManifestSent() updates sendingSyncCompleteManifest to false when sendingSyncCompleteManifest is true`() {
    ReflectionHelpers.setField(syncSenderHandler, "sendingSyncCompleteManifest", true)
    val awaitingPayload = BytePayload(groupResourceString.toByteArray())
    ReflectionHelpers.setField(syncSenderHandler, "awaitingPayload", awaitingPayload)
    every { p2PSenderViewModel.sendChunkData(any()) } answers { null }
    syncSenderHandler.processManifestSent()
    verify(exactly = 0) { p2PSenderViewModel.sendChunkData(awaitingPayload) }

    val sendingSyncCompleteManifest =
      ReflectionHelpers.getField<Boolean>(syncSenderHandler, "sendingSyncCompleteManifest")
    Assert.assertFalse(sendingSyncCompleteManifest)
  }

  fun getDataTypes(): TreeSet<DataType> =
    TreeSet<DataType>(
      listOf("Group").mapIndexed { index, resourceType ->
        DataType(name = resourceType, DataType.Filetype.JSON, index)
      }
    )
}
