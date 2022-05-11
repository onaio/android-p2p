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
package org.smartregister.p2p.ui

import io.mockk.mockk
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.robolectric.Robolectric
import org.robolectric.RuntimeEnvironment
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.P2PLibrary
import org.smartregister.p2p.authentication.model.DeviceRole
import org.smartregister.p2p.robolectric.RobolectricTest
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity
import org.smartregister.p2p.shadows.ShadowAppDatabase

/*import org.smartregister.p2p.shadows.ShadowAppDatabase
import org.smartregister.p2p.shadows.ShadowAppDatabase2*/

/** Test for class [P2PDeviceSearchActivity] */
@Config(shadows = [ShadowAppDatabase::class])
class P2PDeviceSearchActivityTest : RobolectricTest() {

  // Fixes  Main looper has queued unexecuted runnables. This might be the cause of the test failure
  // error
  // @get:Rule val activityRule = ActivityScenarioRule(P2PDeviceSearchActivity::class.java)

  private lateinit var p2PDeviceSearchActivity: P2PDeviceSearchActivity
  private lateinit var p2PDeviceSearchActivityController:
    ActivityController<P2PDeviceSearchActivity>

  private val GET_WIFI_P2P_REASON = "getWifiP2pReason"

  /*@BeforeClass
  fun setupBeforeClass() {
    P2PLibrary.init(P2PLibrary.Options(RuntimeEnvironment.application, "password", "demo", mockk(), mockk()))
  }*/

  @Before
  fun setUp() {
    P2PLibrary.init(
      P2PLibrary.Options(RuntimeEnvironment.application, "password", "demo", mockk(), mockk())
    )

    p2PDeviceSearchActivityController =
      Robolectric.buildActivity(P2PDeviceSearchActivity::class.java)
    p2PDeviceSearchActivity = p2PDeviceSearchActivityController.create().resume().get()
  }

  @After
  fun tearDown() {
    p2PDeviceSearchActivityController.pause().stop().destroy()
  }

  @Test
  fun testGetDeviceRole() {
    // TODO Fix this test
    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", false)
    Assert.assertEquals(DeviceRole.RECEIVER, p2PDeviceSearchActivity.getDeviceRole())

    ReflectionHelpers.setField(p2PDeviceSearchActivity, "isSender", true)
    Assert.assertEquals(DeviceRole.SENDER, p2PDeviceSearchActivity.getDeviceRole())
  }

  @Ignore
  @Test
  fun testGetWifiP2pReason() {
    // TODO Fix this test
    var wifiP2pReason =
      ReflectionHelpers.callInstanceMethod<Any>(
        p2PDeviceSearchActivity,
        GET_WIFI_P2P_REASON,
        ReflectionHelpers.ClassParameter(Int::class.java, 0)
      )

    Assert.assertEquals("Error", wifiP2pReason)

    wifiP2pReason =
      ReflectionHelpers.callInstanceMethod<Any>(
        p2PDeviceSearchActivity,
        GET_WIFI_P2P_REASON,
        ReflectionHelpers.ClassParameter(Int::class.java, 1)
      )

    Assert.assertEquals("Unsupported", wifiP2pReason)

    wifiP2pReason =
      ReflectionHelpers.callInstanceMethod<Any>(
        p2PDeviceSearchActivity,
        GET_WIFI_P2P_REASON,
        ReflectionHelpers.ClassParameter(Int::class.java, 2)
      )

    Assert.assertEquals("Busy", wifiP2pReason)

    wifiP2pReason =
      ReflectionHelpers.callInstanceMethod<Any>(
        p2PDeviceSearchActivity,
        GET_WIFI_P2P_REASON,
        ReflectionHelpers.ClassParameter(Int::class.java, 3)
      )

    Assert.assertEquals("Unknown", wifiP2pReason)
  }
}
