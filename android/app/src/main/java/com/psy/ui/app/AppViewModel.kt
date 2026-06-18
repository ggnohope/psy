package com.psy.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
) : ViewModel() {

    val settings: StateFlow<SettingsState> = settingsRepo.settings.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsState(),
    )

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var hasResolvedInitial = false
    private var lastBackgroundedAt = 0L

    init {
        // On cold start: as soon as settings emit and lockEnabled=true, lock immediately.
        viewModelScope.launch {
            settingsRepo.settings.collect { state ->
                if (!hasResolvedInitial && state.lockEnabled) {
                    _isLocked.value = true
                }
            }
        }
    }

    /**
     * Called from LifecycleEventObserver on ON_START.
     * Locks the app if lockEnabled and either this is the first resolve or the app has been
     * backgrounded for more than 2 seconds.
     */
    fun onStart(nowMillis: Long) {
        val state = settings.value
        if (state.lockEnabled) {
            val elapsed = nowMillis - lastBackgroundedAt
            if (!hasResolvedInitial || elapsed > 2_000L) {
                _isLocked.value = true
            }
        }
        hasResolvedInitial = true
    }

    /**
     * Called from LifecycleEventObserver on ON_STOP.
     * Records the time the app went to background for timeout calculation.
     */
    fun onStop(nowMillis: Long) {
        lastBackgroundedAt = nowMillis
    }

    /** Clears the lock gate — called by LockScreen on successful auth. */
    fun unlock() {
        _isLocked.value = false
    }

    /** Delegates PIN verification to the repository. */
    suspend fun verifyPin(pin: String): Boolean = settingsRepo.verifyPin(pin)
}
