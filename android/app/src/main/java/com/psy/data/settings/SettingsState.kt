package com.psy.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette { CANDY_VIOLET, CANDY_PINK, CANDY_MINT }

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.CANDY_VIOLET,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
)
