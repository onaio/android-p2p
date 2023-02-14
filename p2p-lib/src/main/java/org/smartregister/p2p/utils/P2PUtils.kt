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
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.os.Build
import java.util.Locale
import org.smartregister.p2p.search.ui.P2PDeviceSearchActivity

/** Created by Ephraim Kigamba - nek.eam@gmail.com on 25-02-2022. */
fun startP2PScreen(context: Context) {
  context.startActivity(Intent(context, P2PDeviceSearchActivity::class.java))
}

fun getUsername(context: Context): String {
  return "demo"
}

fun getDeviceName(context: Context): String = "${getUsername(context)} (${getDeviceModel()})"

fun getDeviceModel(): String {
  val manufacturer = Build.MANUFACTURER
  val model = Build.MODEL
  return if (model
      .lowercase(Locale.getDefault())
      .startsWith(manufacturer.lowercase(Locale.getDefault()))
  ) {
    model.capitalize()
  } else {
    manufacturer.capitalize() + " " + model
  }
}

fun String.capitalize(): String = replaceFirstChar {
  if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString()
}

fun isAppDebuggable(context: Context) =
  0 != context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE

fun Long.divideToPercent(divideTo: Long): Int {
  return if (divideTo == 0L) 0 else ((this / divideTo.toFloat()) * 100).toInt()
}
