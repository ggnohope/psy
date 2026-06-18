package com.psy.ui.theme

import androidx.compose.ui.graphics.Color
import com.psy.data.settings.AccentPalette

// Candy Pop palette
val CandyViolet = Color(0xFFA18CFF)
val CandySky = Color(0xFF7FD8FF)
val CandyPink = Color(0xFFFF8FD6)
val CandyPinkDeep = Color(0xFFFF5FA2)
val CandyGreen = Color(0xFF22C55E)

val SurfaceLight = Color(0xFFF4F0FF)
val OnSurfaceLight = Color(0xFF2B2640)
val SurfaceDark = Color(0xFF1C1830)
val OnSurfaceDark = Color(0xFFEDE9FF)

// Accent color sets: (primary, secondary, tertiary)
data class AccentColors(val primary: Color, val secondary: Color, val tertiary: Color)

fun accentColorsFor(accent: AccentPalette): AccentColors = when (accent) {
    AccentPalette.CANDY_VIOLET -> AccentColors(CandyViolet, CandySky, CandyPink)
    AccentPalette.CANDY_PINK   -> AccentColors(CandyPink, CandyPinkDeep, CandyViolet)
    AccentPalette.CANDY_MINT   -> AccentColors(CandyGreen, CandySky, CandyViolet)
}
