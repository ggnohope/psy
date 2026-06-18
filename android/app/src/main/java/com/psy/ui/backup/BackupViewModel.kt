package com.psy.ui.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.auth.AuthTokenStore
import com.psy.domain.repository.AuthRepository
import com.psy.domain.repository.AuthState
import com.psy.domain.repository.BackupRepository
import com.psy.domain.repository.RestoreOutcome
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class BackupViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val backupRepository: BackupRepository,
    private val tokenStore: AuthTokenStore,
) : ViewModel() {

    val authState: StateFlow<AuthState> = authRepository.authState.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthState(signedIn = false, email = null),
    )

    val lastSyncAt: StateFlow<Long?> = backupRepository.lastSyncAt.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = null,
    )

    val autoBackup: StateFlow<Boolean> = tokenStore.autoBackupFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = false,
    )

    private val _uiMessage = MutableStateFlow<String?>(null)
    val uiMessage: StateFlow<String?> = _uiMessage.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    fun signInDev(email: String) {
        viewModelScope.launch {
            _busy.value = true
            val result = authRepository.signInDev(email)
            _busy.value = false
            result.fold(
                onSuccess = { _uiMessage.value = "Đã đăng nhập" },
                onFailure = { _uiMessage.value = it.message ?: "Lỗi đăng nhập" },
            )
        }
    }

    fun signInGoogle(idToken: String) {
        viewModelScope.launch {
            _busy.value = true
            val result = authRepository.signInGoogle(idToken)
            _busy.value = false
            result.fold(
                onSuccess = { _uiMessage.value = "Đã đăng nhập" },
                onFailure = { _uiMessage.value = it.message ?: "Lỗi đăng nhập Google" },
            )
        }
    }

    fun signOut() {
        viewModelScope.launch {
            authRepository.signOut()
        }
    }

    fun backupNow() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepository.backupNow()
            _busy.value = false
            result.fold(
                onSuccess = { _uiMessage.value = "Đã sao lưu" },
                onFailure = { _uiMessage.value = it.message ?: "Lỗi sao lưu" },
            )
        }
    }

    fun restore() {
        viewModelScope.launch {
            _busy.value = true
            val result = backupRepository.restore()
            _busy.value = false
            result.fold(
                onSuccess = { outcome ->
                    _uiMessage.value = when (outcome) {
                        RestoreOutcome.Restored -> "Đã khôi phục"
                        RestoreOutcome.NoBackup -> "Chưa có bản sao lưu"
                    }
                },
                onFailure = { _uiMessage.value = it.message ?: "Lỗi khôi phục" },
            )
        }
    }

    fun setAutoBackup(enabled: Boolean) {
        viewModelScope.launch {
            tokenStore.setAutoBackup(enabled)
        }
    }

    fun consumeMessage() {
        _uiMessage.value = null
    }
}
