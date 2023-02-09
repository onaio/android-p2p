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
import androidx.annotation.NonNull
import androidx.annotation.Nullable

/** Created by Ephraim Kigamba - ekigamba@ona.io on 26/03/2019 */
class Settings(@NonNull context: Context) {
  private val sharedPreferences: SharedPreferences

  @get:Nullable
  val hashKey: String?
    get() = sharedPreferences.getString(Constants.Prefs.KEY_HASH, null)

  fun saveHashKey(@NonNull hashKey: String?) {
    sharedPreferences.edit().putString(Constants.Prefs.KEY_HASH, hashKey).apply()
  }

  init {
    sharedPreferences = context.getSharedPreferences(Constants.Prefs.NAME, Context.MODE_PRIVATE)
  }
}
