package org.smartregister.p2p.search.ui.theme

import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.ui.graphics.Color

val DangerColor = Color(0xFFFF333F)
val InfoColor = Color(0xFF006EB8)
val DefaultColor = Color(0xFF6F7274)
val ProfileBackgroundColor = Color(0xFFF2F4F7)

private val PrimaryColor = Color(0xFF005084)
private val PrimaryVariantColor = Color(0xFF003D66)
private val ErrorColor = Color(0xFFDD0000)

val LightColors =
  lightColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)

val DarkColors =
  darkColors(primary = PrimaryColor, primaryVariant = PrimaryVariantColor, error = ErrorColor)
