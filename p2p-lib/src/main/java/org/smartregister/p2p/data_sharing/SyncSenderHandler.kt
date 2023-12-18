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

import java.util.TreeSet
import kotlinx.coroutines.withContext
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.model.P2PReceivedHistory
import org.smartregister.p2p.model.RecordCount
import org.smartregister.p2p.payload.BytePayload
import org.smartregister.p2p.payload.PayloadContract
import org.smartregister.p2p.search.ui.P2PSenderViewModel
import org.smartregister.p2p.sync.DataType
import org.smartregister.p2p.utils.Constants
import org.smartregister.p2p.utils.DispatcherProvider
import timber.log.Timber

class SyncSenderHandler
constructor(
  val p2PSenderViewModel: P2PSenderViewModel,
  val dataSyncOrder: TreeSet<DataType>,
  val receivedHistory: List<P2PReceivedHistory>,
  private val dispatcherProvider: DispatcherProvider
) {
  private val remainingLastRecordIds = HashMap<String, Long>()
  private val batchSize = 25
  private var awaitingDataTypeRecordsBatchSize = 0
  private var totalSentRecordCount: Long = 0

  private lateinit var awaitingPayload: PayloadContract<out Any>
  private var sendingSyncCompleteManifest = false
  private var recordsBatchOffset = 0
  private var recordCount: RecordCount = RecordCount(0L, hashMapOf())

  suspend fun startSyncProcess() {
    Timber.i("Start sync process")
    generateLastRecordIds()
    populateTotalRecordCount()
    // notify Ui data transfer is to begin
    p2PSenderViewModel.notifyDataTransferStarting()
    sendNextManifest(isInitialManifest = true)
  }

  fun generateLastRecordIds() {
    for (dataType in dataSyncOrder) {
      remainingLastRecordIds[dataType.name] = 0L
    }

    if (receivedHistory.isNotEmpty()) {
      for (dataTypeHistory in receivedHistory) {
        if (dataTypeHistory.lastUpdatedAt == 0L) {
          continue
        }
        remainingLastRecordIds[dataTypeHistory.entityType!!] = dataTypeHistory.lastUpdatedAt
      }
    }
  }

  fun populateTotalRecordCount() {
    recordCount =
      P2PLibrary.getInstance().getSenderTransferDao().getTotalRecordCount(remainingLastRecordIds)
  }

  suspend fun sendNextManifest(isInitialManifest: Boolean = false) {
    Timber.i("in send next manifest")
    if (!dataSyncOrder.isEmpty()) {
      sendJsonDataManifest(dataSyncOrder.first())
    } else {
      val name = if (isInitialManifest) Constants.DATA_UP_TO_DATE else Constants.SYNC_COMPLETE
      val manifest =
        Manifest(
          dataType = DataType(name, DataType.Filetype.JSON, 0),
          recordsSize = 0,
          payloadSize = 0,
          recordCount = recordCount
        )

      sendingSyncCompleteManifest = true
      p2PSenderViewModel.sendManifest(manifest = manifest)
      p2PSenderViewModel.updateSenderSyncComplete(senderSyncComplete = true)
    }
  }

  suspend fun sendJsonDataManifest(dataType: DataType) {
    Timber.i("Sending json manifest")
    val nullableRecordId = remainingLastRecordIds[dataType.name]
    val lastRecordId = nullableRecordId ?: 0L

    withContext(dispatcherProvider.io()) {
      val jsonData =
        P2PLibrary.getInstance()
          .getSenderTransferDao()
          .getJsonData(dataType, lastRecordId, batchSize, recordsBatchOffset)

      // send actual manifest
      if (jsonData != null && (jsonData.getJsonArray()?.length()!! > 0)) {
        Timber.i("Json data is has content")
        val recordsArray = jsonData.getJsonArray()

        recordsBatchOffset += batchSize
        Timber.i("Batch offset $recordsBatchOffset")
        Timber.i("remaining records last updated is ${remainingLastRecordIds[dataType.name]}")

        val recordsJsonString = recordsArray.toString()
        awaitingDataTypeRecordsBatchSize = recordsArray!!.length()
        Timber.i(
          "Progress update: Sending | ${dataType.name} x $awaitingDataTypeRecordsBatchSize | UPTO ${jsonData.getHighestRecordId()}"
        )
        awaitingPayload =
          BytePayload(
            recordsArray.toString().toByteArray(),
          )

        if (recordsJsonString.isNotBlank()) {

          val manifest =
            Manifest(
              dataType = dataType,
              recordsSize = awaitingDataTypeRecordsBatchSize,
              payloadSize = recordsJsonString.length,
              totalRecordCount = recordCount.totalRecordCount,
              recordCount = recordCount
            )

          p2PSenderViewModel.sendManifest(manifest = manifest)
        }
      } else {
        // signifies all data has been sent for a particular datatype
        recordsBatchOffset = 0
        Timber.i("Json data is null")
        dataSyncOrder.remove(dataType)
        sendNextManifest()
      }
    }
  }

  fun processManifestSent() {
    if (sendingSyncCompleteManifest) {
      sendingSyncCompleteManifest = false
    } else {
      p2PSenderViewModel.sendChunkData(awaitingPayload)
    }
  }

  open fun updateTotalSentRecordCount() {
    this.totalSentRecordCount = totalSentRecordCount + awaitingDataTypeRecordsBatchSize
    Timber.i(
      "Progress update: Updating progress to $totalSentRecordCount out of ${recordCount.totalRecordCount}"
    )
    p2PSenderViewModel.updateTransferProgress(
      totalSentRecords = totalSentRecordCount,
      totalRecords = recordCount.totalRecordCount
    )
  }
}
