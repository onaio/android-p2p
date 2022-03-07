package org.smartregister.p2p.search.adapter

import android.net.wifi.p2p.WifiP2pDevice
import android.widget.LinearLayout
import androidx.test.core.app.ApplicationProvider
import io.mockk.mockk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.smartregister.p2p.robolectric.RobolectricTest

/**
 * Created by Ephraim Kigamba - nek.eam@gmail.com on 04-03-2022.
 */
internal class DeviceListAdapterTest : RobolectricTest() {

    private lateinit var deviceListAdapter : DeviceListAdapter

    @Before
    fun setUp() {
        deviceListAdapter = DeviceListAdapter(listOf(), {})
    }

    @After
    fun tearDown() {
    }

    @Test
    fun onCreateViewHolder() {
        val layout = LinearLayout(ApplicationProvider.getApplicationContext())
        val viewHolder = deviceListAdapter.onCreateViewHolder(layout, 0)

        Assert.assertNotNull(viewHolder)
    }

    @Test
    fun onBindViewHolderShouldUpdateViews() {
        val device = WifiP2pDevice().apply {
            deviceName = "Google Pixel"
            deviceAddress = "00:00:5e:00:53:af"
        }
        val peerDevices = listOf(device)
        deviceListAdapter = DeviceListAdapter(peerDevices, {})

        val layout = LinearLayout(ApplicationProvider.getApplicationContext())
        val viewHolder = deviceListAdapter.onCreateViewHolder(layout, 0)

        deviceListAdapter.onBindViewHolder(viewHolder, 0)

        Assert.assertEquals("Google Pixel", viewHolder.deviceName.text)
        Assert.assertEquals("00:00:5e:00:53:af", viewHolder.deviceAddress.text)
    }

    @Test
    fun getItemCount() {
    }
}