package com.psy.data.settings

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AccentPalette { BLUE, AMBER, TEAL }

data class SettingsState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val accent: AccentPalette = AccentPalette.BLUE,
    val lockEnabled: Boolean = false,
    val pinHash: String? = null,
    val biometricEnabled: Boolean = false,
)
