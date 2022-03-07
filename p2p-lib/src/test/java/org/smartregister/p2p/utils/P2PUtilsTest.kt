package org.smartregister.p2p.utils

import android.app.Activity
import android.app.Application
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.slot
import io.mockk.unmockkStatic
import io.mockk.verify
import org.junit.Assert
import org.junit.Test
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity

class P2PUtilsTest : RobolectricTest() {

    @Test
    fun startP2PScreenShouldCallStartActivity() {
        val spyActivity = mockk<Activity>()
        val intentSlot = slot<Intent>()

        every { spyActivity.packageName } returns "org.smartregister.p2p"
        every { spyActivity.baseContext } returns ApplicationProvider.getApplicationContext()
        every { spyActivity.startActivity(any()) } just runs

        startP2PScreen(spyActivity)

        verify { spyActivity.startActivity(capture(intentSlot)) }

        Assert.assertEquals("org.smartregister.p2p" ,intentSlot.captured.component!!.packageName)
        Assert.assertEquals(P2PDeviceSearchActivity::class.java.name ,intentSlot.captured.component!!.className)
    }

    @Test
    fun getUsernameShouldReturnDemo() {
        Assert.assertEquals("demo", getUsername(ApplicationProvider.getApplicationContext()))
    }

    @Test
    fun getDeviceNameShouldReturnUsernameAndDeviceModel() {
        mockkStatic(::getDeviceModel)

        every { getDeviceModel() } returns "Google Emulator"

        val deviceName = getDeviceName(ApplicationProvider.getApplicationContext())

        Assert.assertEquals("demo (Google Emulator)", deviceName)

        unmockkStatic(::getDeviceModel)
    }

    @Test
    fun capitalShouldChangeLowercaseText() {
        Assert.assertEquals("Demo", "demo".capitalize())
    }

    @Test
    fun capitalShouldRetainCapitalizedText() {
        Assert.assertEquals("Demo", "Demo".capitalize())
    }

    @Test
    fun capitalShouldRetainUppercaseText() {
        Assert.assertEquals("DEMO", "DEMO".capitalize())
    }

    @Test
    fun capitalShouldCapitalizedMixedcase() {
        Assert.assertEquals("DeMO", "deMO".capitalize())
    }
}