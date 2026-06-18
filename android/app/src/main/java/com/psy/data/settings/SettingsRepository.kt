package com.psy.data.settings

import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settings: Flow<SettingsState>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setAccent(accent: AccentPalette)
    suspend fun setLockEnabled(enabled: Boolean)
    suspend fun setPin(pin: String)
    suspend fun clearPin()
    suspend fun setBiometricEnabled(enabled: Boolean)
    suspend fun verifyPin(pin: String): Boolean
}
