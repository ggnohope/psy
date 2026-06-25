package com.psy.ui.theme

import androidx.compose.ui.graphics.Color
import com.psy.data.settings.AccentPalette

/**
 * TEMPORARY bridge during the HostGuardIQ redesign (Phase 1 → Phase 2).
 *
 * The old "Candy" color names + [accentColorsFor] are kept so existing screens
 * compile before they are individually rebuilt in Phase 2. Each value maps onto
 * the closest HostGuardIQ token. Remove these as each screen is re-skinned.
 */

@Deprecated("Use LocalPsyColors.current.blue", ReplaceWith("LocalPsyColors.current.blue"))
val CandyViolet = Color(0xFF0A7CF6)   // → DS blue (primary)

@Deprecated("Use LocalPsyColors.current.teal", ReplaceWith("LocalPsyColors.current.teal"))
val CandySky = Color(0xFF0BB3B0)      // → DS teal

@Deprecated("Use LocalPsyColors.current.amber", ReplaceWith("LocalPsyColors.current.amber"))
val CandyPink = Color(0xFFF59E0B)     // → DS amber

@Deprecated("Use LocalPsyColors.current.red", ReplaceWith("LocalPsyColors.current.red"))
val CandyPinkDeep = Color(0xFFE0413A) // → DS red (expense/danger)

@Deprecated("Use LocalPsyColors.current.green", ReplaceWith("LocalPsyColors.current.green"))
val CandyGreen = Color(0xFF1F9D62)    // → DS green (income)

@Deprecated("Use accentPrimary()/PsyColors tokens")
data class AccentColors(val primary: Color, val secondary: Color, val tertiary: Color)

@Deprecated("Use accentPrimary(accent, dark)")
fun accentColorsFor(accent: AccentPalette): AccentColors = AccentColors(
    primary = accentPrimary(accent, dark = false),
    secondary = Color(0xFF0BB3B0),
    tertiary = Color(0xFFF59E0B),
)
