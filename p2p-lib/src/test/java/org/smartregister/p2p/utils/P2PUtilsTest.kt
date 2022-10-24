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
package org.smartregister.p2p.utils

import android.app.Activity
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

/** Test for class [P2PUtils] */
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

    Assert.assertEquals("org.smartregister.p2p", intentSlot.captured.component!!.packageName)
    Assert.assertEquals(
      P2PDeviceSearchActivity::class.java.name,
      intentSlot.captured.component!!.className
    )
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

  @Test
  fun divideToPercentShouldReturnCorrectValues() {
    var number: Long = 10
    Assert.assertEquals(50, number.divideToPercent(20))
  }
}
