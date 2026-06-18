package com.psy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.psy.data.settings.AccentPalette
import com.psy.data.settings.ThemeMode

private fun colorSchemeFor(accent: AccentPalette, dark: Boolean): ColorScheme {
    val colors = accentColorsFor(accent)
    return if (dark) {
        darkColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            tertiary = colors.tertiary,
            surface = SurfaceDark,
            onSurface = OnSurfaceDark,
            background = SurfaceDark,
            onBackground = OnSurfaceDark,
        )
    } else {
        lightColorScheme(
            primary = colors.primary,
            secondary = colors.secondary,
            tertiary = colors.tertiary,
            surface = SurfaceLight,
            onSurface = OnSurfaceLight,
            background = SurfaceLight,
            onBackground = OnSurfaceLight,
        )
    }
}

@Composable
fun PsyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPalette = AccentPalette.CANDY_VIOLET,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    MaterialTheme(
        colorScheme = colorSchemeFor(accent, dark),
        typography = CandyTypography,
        shapes = CandyShapes,
        content = content,
    )
}
