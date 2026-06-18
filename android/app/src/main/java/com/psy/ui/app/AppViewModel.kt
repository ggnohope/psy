package com.psy.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.auth.AuthTokenStore
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsState
import com.psy.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

private const val AUTO_BACKUP_THROTTLE_MS = 5 * 60 * 1000L // 5 minutes

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val backupRepository: BackupRepository,
    private val tokenStore: AuthTokenStore,
) : ViewModel() {

    // Application-scoped coroutine scope for best-effort background ops (outlives ViewModel)
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

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
     * Also triggers a best-effort auto-backup if conditions are met (signed in, auto-backup
     * enabled, throttle ≥ 5 min since last sync). Non-blocking — runs on appScope.
     */
    fun onStop(nowMillis: Long) {
        lastBackgroundedAt = nowMillis
        // Best-effort auto-backup: read state and trigger if eligible
        appScope.launch {
            val token = tokenStore.currentToken() ?: return@launch
            val autoBackup = tokenStore.autoBackupFlow.first()
            if (!autoBackup) return@launch
            val lastSync = tokenStore.lastSyncAtFlow.first() ?: 0L
            if (nowMillis - lastSync > AUTO_BACKUP_THROTTLE_MS) {
                runCatching { backupRepository.backupNow() }
            }
        }
    }

    /** Clears the lock gate — called by LockScreen on successful auth. */
    fun unlock() {
        _isLocked.value = false
    }

    /** Delegates PIN verification to the repository. */
    suspend fun verifyPin(pin: String): Boolean = settingsRepo.verifyPin(pin)
}
