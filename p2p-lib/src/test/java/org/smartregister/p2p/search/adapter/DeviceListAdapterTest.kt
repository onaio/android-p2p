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
package org.smartregister.p2p.search.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.p2p.robolectric.RobolectricTest

internal class DeviceListAdapterTest : RobolectricTest() {

  private lateinit var deviceListAdapter: DeviceListAdapter

  @Before
  fun setUp() {
    deviceListAdapter = DeviceListAdapter(listOf(), {})
  }

  @Test
  fun onCreateViewHolder() {
    val layout = LinearLayout(ApplicationProvider.getApplicationContext())
    val viewHolder = deviceListAdapter.onCreateViewHolder(layout, 0)

    Assert.assertNotNull(viewHolder)
  }

  @Test
  fun onBindViewHolderShouldUpdateViews() {
    val deviceAddress = "00:00:5e:00:53:af"
    val deviceName = "Google Pixel"
    val device =
      WifiP2pDevice().apply {
        this.deviceName = deviceName
        this.deviceAddress = deviceAddress
      }
    val peerDevices = listOf(device)
    deviceListAdapter = DeviceListAdapter(peerDevices, {})

    val layout = LinearLayout(ApplicationProvider.getApplicationContext())
    val viewHolder = deviceListAdapter.onCreateViewHolder(layout, 0)

    deviceListAdapter.onBindViewHolder(viewHolder, 0)

    Assert.assertEquals(deviceName, viewHolder.deviceName.text)
    Assert.assertEquals(deviceAddress, viewHolder.deviceAddress.text)
  }

  @Test
  fun getItemCountShouldReturnActualCountDevices() {
    Assert.assertEquals(0, deviceListAdapter.itemCount)

    val device =
      WifiP2pDevice().apply {
        deviceName = "Google Pixel"
        deviceAddress = "00:00:5e:00:53:af"
      }
    val peerDevices = listOf(device, device, device)
    deviceListAdapter = DeviceListAdapter(peerDevices, {})

    Assert.assertEquals(3, deviceListAdapter.itemCount)
  }
}
