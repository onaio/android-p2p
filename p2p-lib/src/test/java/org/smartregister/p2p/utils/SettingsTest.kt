/*
 * Copyright 2022-2023 Ona Systems, Inc
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

import android.content.Context
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert
import org.junit.Test
import org.robolectric.util.ReflectionHelpers
import org.smartregister.p2p.robolectric.RobolectricTest

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 10-05-2022. */
class SettingsTest : RobolectricTest() {

  val settings = Settings(ApplicationProvider.getApplicationContext())
  val sharedPreferences = mockk<SharedPreferences>()

  @Test
  fun getHashKeyShouldCallSharedPreferencesGetString() {
    ReflectionHelpers.setField(settings, "sharedPreferences", sharedPreferences)
    val hashKeyVal = "bd220c15-7a26-49e4-9c62-f0e5e3570070"

    every { sharedPreferences.getString(Constants.Prefs.KEY_HASH, null) } returns hashKeyVal

    Assert.assertEquals(hashKeyVal, settings.hashKey)
  }

  @Test
  fun saveHashKeyShouldReflect() {
    val sharedPrefs =
      ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE)

    val hashKeyVal = "64543394-7d75-43d8-9b61-c192b63e1894"
    Assert.assertNotEquals(hashKeyVal, sharedPrefs.getString(Constants.Prefs.KEY_HASH, null))

    settings.saveHashKey(hashKeyVal)

    Assert.assertEquals(hashKeyVal, sharedPrefs.getString(Constants.Prefs.KEY_HASH, null))
  }
}
