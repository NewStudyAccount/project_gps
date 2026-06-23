package com.gps.dashboard.ui.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.gps.dashboard.R

val MonoFontFamily = FontFamily(
    Font(R.font.jetbrains_mono_regular, FontWeight.Normal),
    Font(R.font.jetbrains_mono_medium, FontWeight.Medium),
    Font(R.font.jetbrains_mono_bold, FontWeight.Bold),
)

val DataLabel = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    color = Accent,
)

val DataValue = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 24.sp,
    color = Primary,
)

val DataValueSmall = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Medium,
    fontSize = 20.sp,
    color = Primary,
)

val DataUnit = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 14.sp,
    color = TextDim,
)

val CompassDegree = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 24.sp,
    color = Primary,
)

val TopBarText = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 12.sp,
    color = TextPrimary,
)

val MetaText = TextStyle(
    fontFamily = MonoFontFamily,
    fontWeight = FontWeight.Normal,
    fontSize = 11.sp,
    color = TextDim,
)
