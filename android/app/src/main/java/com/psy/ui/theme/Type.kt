package com.psy.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Default to system FontFamily.Default for now; swap to a rounded font
// (e.g. Quicksand / Baloo 2 in res/font) in the Theming & Lock plan.
private val CandyFont = FontFamily.Default

val CandyTypography = Typography(
    headlineMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.ExtraBold, fontSize = 24.sp),
    titleMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.Bold, fontSize = 16.sp),
    bodyMedium = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.Medium, fontSize = 14.sp),
    labelSmall = TextStyle(fontFamily = CandyFont, fontWeight = FontWeight.SemiBold, fontSize = 11.sp),
)
