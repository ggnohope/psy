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

    // --- Set-PIN dialog state ---
    val setPinDialogOpen: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val pinEntry: MutableStateFlow<String> = MutableStateFlow("")
    val pinConfirm: MutableStateFlow<String> = MutableStateFlow("")
    val pinError: MutableStateFlow<String> = MutableStateFlow("")

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
        pinEntry.value = ""
        pinConfirm.value = ""
        pinError.value = ""
        setPinDialogOpen.value = true
    }

    fun onPinEntryChange(s: String) {
        // Accept only digits, max 6
        val filtered = s.filter { it.isDigit() }.take(6)
        pinEntry.value = filtered
    }

    fun onPinConfirmChange(s: String) {
        val filtered = s.filter { it.isDigit() }.take(6)
        pinConfirm.value = filtered
    }

    fun confirmSetPin() {
        val entry = pinEntry.value
        val confirm = pinConfirm.value
        if (entry.length in 4..6 && entry == confirm) {
            viewModelScope.launch {
                settingsRepo.setPin(entry)
                settingsRepo.setLockEnabled(true)
            }
            pinError.value = ""
            setPinDialogOpen.value = false
            pinEntry.value = ""
            pinConfirm.value = ""
        } else {
            pinError.value = "PIN phải 4-6 số và khớp"
        }
    }

    fun closeSetPin() {
        setPinDialogOpen.value = false
        pinEntry.value = ""
        pinConfirm.value = ""
        pinError.value = ""
    }

    fun setBiometricEnabled(enabled: Boolean) {
        viewModelScope.launch { settingsRepo.setBiometricEnabled(enabled) }
    }
}
