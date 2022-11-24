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
package org.smartregister.p2p.search.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val DangerColor = Color(0xFFFF333F)
val InfoColor = Color(0xFF006EB8)
val DefaultColor = Color(0xFF6F7274)
val WhiteColor = Color(0xFFF2F4F7)
val SuccessColor = Color(0xFF1DB11B)

private val PrimaryColor = Color(0xFF005084)
private val PrimaryVariantColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors =
  lightColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)

val DarkColors =
  darkColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)
