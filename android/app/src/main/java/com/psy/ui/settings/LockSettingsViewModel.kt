package com.psy.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsState
import com.psy.ui.lock.BiometricAuthenticator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LockSettingsViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    @ApplicationContext context: Context,
) : ViewModel() {

    val settings: StateFlow<SettingsState> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    val isBiometricAvailable: Boolean = BiometricAuthenticator.isAvailable(context)

    // --- Set-PIN dialog state. The keypad screen drives the enter→confirm flow in local
    // Compose state and persists via [trySavePin]; this flag only controls visibility. ---
    val setPinDialogOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)

    /** Called when the user toggles the app lock switch ON. */
    fun requestEnableLock() {
        if (settings.value.pinHash == null) {
            // No PIN set yet — open the set-PIN dialog
            openSetPin()
        } else {
            viewModelScope.launch { settingsRepo.setLockEnabled(true) }
        }
    }

    /** Called when the user toggles the app lock switch OFF. */
    fun disableLock() {
        viewModelScope.launch { settingsRepo.setLockEnabled(false) }
    }

    fun openSetPin() {
        setPinDialogOpen.value = true
    }

    /**
     * Persist a confirmed 4-digit PIN and enable the lock. Called by the keypad-based
     * set-PIN screen, which handles the enter→confirm two-stage flow in local Compose state.
     * Returns true if the PIN was valid (4 digits) and saved.
     */
    fun trySavePin(pin: String): Boolean {
        if (pin.length != 4) return false
        savePin(pin)
        return true
    }

    private fun savePin(pin: String) {
        viewModelScope.launch {
            settingsRepo.setPin(pin)
            settingsRepo.setLockEnabled(true)
        }
    }

    fun closeSetPin() {
        setPinDialogOpen.value = false
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setBiometricEnabled(enabled) }
    }
}
