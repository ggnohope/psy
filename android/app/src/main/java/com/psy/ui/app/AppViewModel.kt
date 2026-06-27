package com.psy.ui.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.auth.AuthTokenStore
import com.psy.data.settings.SettingsRepository
import com.psy.data.settings.SettingsState
import com.psy.domain.repository.AuthRepository
import com.psy.domain.repository.BackupRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

private const val AUTO_BACKUP_THROTTLE_MS = 5 * 60 * 1000L // 5 minutes

@HiltViewModel
class AppViewModel @Inject constructor(
    private val settingsRepo: SettingsRepository,
    private val authRepository: AuthRepository,
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

    /**
     * "Signed in" = a JWT is stored (presence check only — offline OK after first login).
     * Tri-state: null = unknown/loading (until DataStore emits) so the gate shows a loader
     * instead of flashing the LoginScreen for an already-signed-in user on cold start.
     */
    val isSignedIn: StateFlow<Boolean?> = tokenStore.tokenFlow
        .map { it != null }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null,
        )

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _isPreparingData = MutableStateFlow(false)
    val isPreparingData: StateFlow<Boolean> = _isPreparingData.asStateFlow()

    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked

    private var hasResolvedInitial = false
    private var lastBackgroundedAt = 0L
    private val prepareMutex = Mutex()
    // Auto-backup gate: the token for which restore-or-seed has SUCCEEDED. Set on the main
    // thread, read from the IO appScope in onStop/logout, hence @Volatile. backupNow only
    // runs when this matches the current token, so a freshly wiped/empty local state can
    // never overwrite the cloud backup before it is restored.
    @Volatile
    private var preparedToken: String? = null

    init {
        // On cold start: as soon as settings emit and lockEnabled=true, lock immediately.
        viewModelScope.launch {
            settingsRepo.settings.collect { state ->
                if (!hasResolvedInitial && state.lockEnabled) {
                    _isLocked.value = true
                }
            }
        }
        // If a token already exists on cold start, restore/seed before opening the app shell.
        viewModelScope.launch {
            tokenStore.tokenFlow.collect { token ->
                if (token == null) {
                    preparedToken = null
                    _isPreparingData.value = false
                } else {
                    prepareLocalDataForToken(token)
                }
            }
        }
    }

    /** Shows a transient message (e.g. login error). */
    fun showMessage(message: String) {
        _uiMessage.value = message
    }

    /**
     * Google sign-in: verify the ID token with the backend, then prepare local data
     * (restore-or-seed). Flips [isSignedIn] reactively so the gate opens.
     */
    fun signInGoogle(idToken: String) {
        viewModelScope.launch {
            _isPreparingData.value = true
            authRepository.signInGoogle(idToken)
                .onSuccess {
                    _uiMessage.value = null
                    val token = tokenStore.tokenFlow.first { it != null } ?: return@onSuccess
                    prepareLocalDataForToken(token)
                }
                .onFailure {
                    _isPreparingData.value = false
                    _uiMessage.value = it.message ?: "Lỗi đăng nhập Google"
                }
        }
    }

    private suspend fun prepareLocalDataForToken(token: String) {
        prepareMutex.withLock {
            if (preparedToken == token) return
            _isPreparingData.value = true
            runCatching { backupRepository.prepareLocalDataAfterLogin() }
                .onSuccess {
                    preparedToken = token
                    _uiMessage.value = null
                }
                .onFailure {
                    _uiMessage.value = it.message ?: "Không thể chuẩn bị dữ liệu"
                }
            _isPreparingData.value = false
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
     * Records the background time for lock timeout, then triggers a best-effort auto-backup if
     * signed in and throttle (≥ 5 min since last sync) is satisfied. Non-blocking — runs on appScope.
     */
    fun onStop(nowMillis: Long) {
        lastBackgroundedAt = nowMillis
        appScope.launch {
            val token = tokenStore.currentToken() ?: return@launch
            // Never auto-backup until restore-or-seed has succeeded this session, else an
            // empty/freshly-wiped local state would overwrite the good cloud backup.
            if (preparedToken != token) return@launch
            val lastSync = tokenStore.lastSyncAtFlow.first() ?: 0L
            if (nowMillis - lastSync > AUTO_BACKUP_THROTTLE_MS) {
                runCatching { backupRepository.backupNow() }
            }
        }
    }

    /**
     * Logout: best-effort final backup → wipe local DB → clear token. Clearing the token flips
     * [isSignedIn] false so AppRoot shows LoginScreen. Runs on appScope so it outlives navigation.
     */
    fun logout() {
        appScope.launch {
            // Only push a final backup if local data is in a known-good (prepared) state.
            if (preparedToken == tokenStore.currentToken()) {
                runCatching { backupRepository.backupNow() }
            }
            runCatching { backupRepository.wipeLocal() }
            authRepository.signOut()
        }
    }

    /** Clears the lock gate — called by LockScreen on successful auth. */
    fun unlock() {
        _isLocked.value = false
    }

    /** Delegates PIN verification to the repository. */
    suspend fun verifyPin(pin: String): Boolean = settingsRepo.verifyPin(pin)
}
