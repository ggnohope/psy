package com.psy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.psy.data.settings.AccentPalette
import com.psy.data.settings.ThemeMode

@Composable
fun PsyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    accent: AccentPalette = AccentPalette.BLUE,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT  -> false
        ThemeMode.DARK   -> true
    }
    val base = if (dark) DarkPsyColors else LightPsyColors
    val psyColors = base.copy(
        blue = accentPrimary(accent, dark),
        blueSoft = accentSoft(accent, dark),
    )
    // Material3 scheme for built-in widgets; mapped onto our tokens.
    val scheme = if (dark) {
        darkColorScheme(
            primary = psyColors.blue, onPrimary = Color.White,
            surface = psyColors.surface, onSurface = psyColors.text,
            background = psyColors.bg, onBackground = psyColors.text,
            surfaceVariant = psyColors.sunken, outline = psyColors.hair,
            error = psyColors.red,
        )
    } else {
        lightColorScheme(
            primary = psyColors.blue, onPrimary = Color.White,
            surface = psyColors.surface, onSurface = psyColors.text,
            background = psyColors.bg, onBackground = psyColors.text,
            surfaceVariant = psyColors.sunken, outline = psyColors.hair,
            error = psyColors.red,
        )
    }
    CompositionLocalProvider(LocalPsyColors provides psyColors) {
        MaterialTheme(
            colorScheme = scheme,
            typography = PsyTypography,
            shapes = PsyShapes,
            content = content,
        )
    }
}
