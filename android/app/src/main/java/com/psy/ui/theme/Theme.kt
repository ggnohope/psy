package com.psy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = CandyViolet,
    secondary = CandySky,
    tertiary = CandyPink,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
)

private val DarkColors = darkColorScheme(
    primary = CandyViolet,
    secondary = CandySky,
    tertiary = CandyPink,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
)

@Composable
fun PsyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography = CandyTypography,
        shapes = CandyShapes,
        content = content,
    )
}
