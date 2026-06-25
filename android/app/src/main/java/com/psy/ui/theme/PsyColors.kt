package com.psy.ui.theme

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

/** Full HostGuardIQ semantic token set (mirrors iOS PsyColors / the --c-* CSS vars). */
data class PsyColors(
    val bg: Color,
    val surface: Color,
    val sunken: Color,
    val hair: Color,
    val text: Color,
    val text2: Color,
    val text3: Color,
    val blue: Color,        // primary (rebound by accent)
    val blueSoft: Color,
    val amber: Color,
    val amberSoft: Color,
    val teal: Color,
    val tealSoft: Color,
    val green: Color,
    val greenSoft: Color,
    val red: Color,
    val redSoft: Color,
    val isDark: Boolean,
) {
    // Brand grounds (both themes)
    val navy get() = Color(0xFF0A2540)
    val navyDeep get() = Color(0xFF061A30)
    val incomeTint get() = Color(0xFF7BE3B0)   // on navy
    val expenseTint get() = Color(0xFFF8A09B)  // on navy
    val heroGradient: Brush
        get() = Brush.linearGradient(listOf(Color(0xFF103458), Color(0xFF0A2540), Color(0xFF061A30)))
    val accentLine: Brush
        get() = Brush.horizontalGradient(listOf(blue, Color(0xFF19E3E0)))
}

val LightPsyColors = PsyColors(
    bg = Color(0xFFF7F9FC), surface = Color(0xFFFFFFFF), sunken = Color(0xFFEEF2F8),
    hair = Color(0xFFDDE5EF), text = Color(0xFF0A2540), text2 = Color(0xFF33455C),
    text3 = Color(0xFF5B6B80), blue = Color(0xFF0A7CF6), blueSoft = Color(0xFFE8F2FE),
    amber = Color(0xFFF59E0B), amberSoft = Color(0xFFFEF0D4), teal = Color(0xFF0BB3B0),
    tealSoft = Color(0xFFDCF8F7), green = Color(0xFF1F9D62), greenSoft = Color(0xFFE6F6ED),
    red = Color(0xFFE0413A), redSoft = Color(0xFFFDECEC), isDark = false,
)

val DarkPsyColors = PsyColors(
    bg = Color(0xFF061A30), surface = Color(0xFF0D2A48), sunken = Color(0xFF103458),
    hair = Color(0xFF1C486F), text = Color(0xFFEEF2F8), text2 = Color(0xFFAEC4DA),
    text3 = Color(0xFF7E96AE), blue = Color(0xFF3D97F8), blueSoft = Color(0x2E3D97F8),
    amber = Color(0xFFFBB43D), amberSoft = Color(0x33FBB43D), teal = Color(0xFF19E3E0),
    tealSoft = Color(0x3319E3E0), green = Color(0xFF3CC987), greenSoft = Color(0x333CC987),
    red = Color(0xFFF06B65), redSoft = Color(0x33F06B65), isDark = true,
)

val LocalPsyColors = staticCompositionLocalOf { LightPsyColors }
