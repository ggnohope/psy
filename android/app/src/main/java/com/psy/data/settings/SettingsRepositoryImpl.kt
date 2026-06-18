package com.psy.data.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.security.MessageDigest
import javax.inject.Inject

class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private companion object {
        val KEY_MODE = stringPreferencesKey("theme_mode")
        val KEY_ACCENT = stringPreferencesKey("accent_palette")
        val KEY_LOCK = booleanPreferencesKey("lock_enabled")
        val KEY_PIN_HASH = stringPreferencesKey("pin_hash")
        val KEY_BIO = booleanPreferencesKey("biometric_enabled")
    }

    override val settings: Flow<SettingsState> = dataStore.data.map { p ->
        SettingsState(
            themeMode = runCatching {
                ThemeMode.valueOf(p[KEY_MODE] ?: "SYSTEM")
            }.getOrDefault(ThemeMode.SYSTEM),
            accent = runCatching {
                AccentPalette.valueOf(p[KEY_ACCENT] ?: "CANDY_VIOLET")
            }.getOrDefault(AccentPalette.CANDY_VIOLET),
            lockEnabled = p[KEY_LOCK] ?: false,
            pinHash = p[KEY_PIN_HASH],
            biometricEnabled = p[KEY_BIO] ?: false,
        )
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[KEY_MODE] = mode.name }
    }

    override suspend fun setAccent(accent: AccentPalette) {
        dataStore.edit { it[KEY_ACCENT] = accent.name }
    }

    override suspend fun setLockEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_LOCK] = enabled }
    }

    override suspend fun setPin(pin: String) {
        dataStore.edit { it[KEY_PIN_HASH] = sha256("psy_salt:$pin") }
    }

    override suspend fun clearPin() {
        dataStore.edit { it.remove(KEY_PIN_HASH) }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_BIO] = enabled }
    }

    override suspend fun verifyPin(pin: String): Boolean {
        val current = settings.first().pinHash
        return current != null && current == sha256("psy_salt:$pin")
    }

    private fun sha256(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(input.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
}
