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

import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.spyk
import io.mockk.verify
import java.util.TreeSet
import org.junit.Before
import org.junit.Test
import org.robolectric.annotation.Config
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.dao.ReceiverTransferDao
import org.smartregister.p2p.dao.SenderTransferDao
import org.smartregister.p2p.data_sharing.DataSharingStrategy
import org.smartregister.p2p.payload.StringPayload
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.contract.P2pModeSelectContract
import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.sync.DataType

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-05-2022. */
@Config(shadows = [ShadowAppDatabase::class])
internal class P2PSenderViewModelTest : RobolectricTest() {

  lateinit var p2PSenderViewModel: P2PSenderViewModel
  lateinit var view: P2pModeSelectContract.View
  lateinit var p2pSenderTransferDao: SenderTransferDao
  lateinit var p2pReceiverTransferDao: ReceiverTransferDao

  @Before
  internal fun setUp() {
    view = mockk()
    val dataSharingStrategy = mockk<DataSharingStrategy>()
    p2PSenderViewModel = spyk(P2PSenderViewModel(view, dataSharingStrategy))

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
  }

  @Test fun sendDeviceDetails() {}

  @Test fun requestSyncParams() {}

  @Test fun sendSyncComplete() {}

  @Test fun sendChunkData() {}

  @Test fun sendManifest() {}

  @Test fun getCurrentConnectedDevice() {}

  @Test fun processReceivedHistory() {}

  @Test
  fun processReceivedHistoryShouldCallSendSyncCompleteWhenSyncPayloadInsideJsonDataIsNull() {
    val syncPayload = StringPayload("[]")

    every { p2pSenderTransferDao.getP2PDataTypes() } returns TreeSet<DataType>()

    p2PSenderViewModel.processReceivedHistory(syncPayload)

    verify { p2PSenderViewModel.sendSyncComplete() }
  }

  @Test fun updateSenderSyncComplete() {}
}
