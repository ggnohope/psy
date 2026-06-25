package com.psy.ui.theme

import androidx.compose.ui.graphics.Color
import com.psy.data.settings.AccentPalette

/** Primary hue per accent choice; rebinds PsyColors.blue. */
fun accentPrimary(accent: AccentPalette, dark: Boolean): Color = when (accent) {
    AccentPalette.BLUE  -> if (dark) Color(0xFF3D97F8) else Color(0xFF0A7CF6)
    AccentPalette.AMBER -> if (dark) Color(0xFFFBB43D) else Color(0xFFF59E0B)
    AccentPalette.TEAL  -> if (dark) Color(0xFF19E3E0) else Color(0xFF0BB3B0)
}

/** Soft variant of the accent primary (for active pills/tiles). */
fun accentSoft(accent: AccentPalette, dark: Boolean): Color = when (accent) {
    AccentPalette.BLUE  -> if (dark) Color(0x2E3D97F8) else Color(0xFFE8F2FE)
    AccentPalette.AMBER -> if (dark) Color(0x33FBB43D) else Color(0xFFFEF0D4)
    AccentPalette.TEAL  -> if (dark) Color(0x3319E3E0) else Color(0xFFDCF8F7)
}
