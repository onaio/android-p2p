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
package org.smartregister.p2p.contract

import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.search.contract.P2pModeSelectContract

class P2pModeSelectContractTest {
  private lateinit var view: P2pModeSelectContract.View

  @Before
  fun setUp() {
    view = mockk()
  }

  @Test
  fun `view#getDeviceRole() returns correct value`() {
    every { view.getDeviceRole() } returns DeviceRole.RECEIVER

    Assert.assertEquals(DeviceRole.RECEIVER, view.getDeviceRole())
  }

  @Test
  fun `view#showP2PSelectPage accepts correct values`() {
    every { view.showP2PSelectPage(DeviceRole.RECEIVER, "receiver") } just runs
    view.showP2PSelectPage(DeviceRole.RECEIVER, "receiver")
    verify(exactly = 1) { view.showP2PSelectPage(DeviceRole.RECEIVER, "receiver") }
  }

  @Test
  fun `view#senderSyncComplete accepts correct values`() {
    every { view.senderSyncComplete(true) } just runs
    view.senderSyncComplete(true)
    verify(exactly = 1) { view.senderSyncComplete(true) }
  }
}
