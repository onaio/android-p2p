package org.smartregister.p2p.ui

import androidx.test.ext.junit.rules.ActivityScenarioRule
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.android.controller.ActivityController
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity

/**
 * Test for class [P2PDeviceSearchActivity]
 */
class P2PDeviceSearchActivityTest : RobolectricTest() {

    // Fixes  Main looper has queued unexecuted runnables. This might be the cause of the test failure error
    @get:Rule val activityRule = ActivityScenarioRule(P2PDeviceSearchActivity::class.java)

    private lateinit var p2PDeviceSearchActivity: P2PDeviceSearchActivity
    private lateinit var p2PDeviceSearchActivityController :
            ActivityController<P2PDeviceSearchActivity>

    private val GET_WIFI_P2P_REASON = "getWifiP2pReason"

    @Before
    fun setUp() {
        p2PDeviceSearchActivityController =
            Robolectric.buildActivity(P2PDeviceSearchActivity::class.java)
        p2PDeviceSearchActivity = p2PDeviceSearchActivityController
            .create().resume().get()

    }

    @After
    fun tearDown() {
        p2PDeviceSearchActivityController.pause().stop().destroy()
    }

    @Test
    fun testGetDeviceRole() {
        ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", false)
        Assert.assertEquals(DeviceRole.RECEIVER, p2PDeviceSearchActivity.getDeviceRole())

        ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", true)
        Assert.assertEquals(DeviceRole.SENDER, p2PDeviceSearchActivity.getDeviceRole())
    }

    @Test
    fun testGetWifiP2pReason() {

        var wifiP2pReason = ReflectionHelpers.callInstanceMethod<Any>(
            p2PDeviceSearchActivity,
            GET_WIFI_P2P_REASON,
            ReflectionHelpers.ClassParameter(Int::class.java, 0))

        Assert.assertEquals("Error", wifiP2pReason)

        wifiP2pReason = ReflectionHelpers.callInstanceMethod<Any>(
            p2PDeviceSearchActivity,
            GET_WIFI_P2P_REASON,
            ReflectionHelpers.ClassParameter(Int::class.java, 1))

        Assert.assertEquals("Unsupported", wifiP2pReason)

        wifiP2pReason = ReflectionHelpers.callInstanceMethod<Any>(
            p2PDeviceSearchActivity,
            GET_WIFI_P2P_REASON,
            ReflectionHelpers.ClassParameter(Int::class.java, 2))

        Assert.assertEquals("Busy", wifiP2pReason)

        wifiP2pReason = ReflectionHelpers.callInstanceMethod<Any>(
            p2PDeviceSearchActivity,
            GET_WIFI_P2P_REASON,
            ReflectionHelpers.ClassParameter(Int::class.java, 3))

        Assert.assertEquals("Unknown", wifiP2pReason)
    }

}