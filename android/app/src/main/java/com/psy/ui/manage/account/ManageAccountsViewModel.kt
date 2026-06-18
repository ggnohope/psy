package com.psy.ui.manage.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Account
import com.psy.domain.model.AccountType
import com.psy.domain.repository.AccountRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageAccountsUiState(
    val accounts: List<Account> = emptyList(),
    // editor
    val editorOpen: Boolean = false,
    val editingId: Long? = null,
    val draftName: String = "",
    val draftType: AccountType = AccountType.CASH,
    val draftIcon: String = "💵",
    val draftColor: Long = 0xFF22C55E,
)

@HiltViewModel
class ManageAccountsViewModel @Inject constructor(
    private val repo: AccountRepository,
) : ViewModel() {

    private val _editorOpen = MutableStateFlow(false)
    private val _editingId = MutableStateFlow<Long?>(null)
    private val _draftName = MutableStateFlow("")
    private val _draftType = MutableStateFlow(AccountType.CASH)
    private val _draftIcon = MutableStateFlow("💵")
    private val _draftColor = MutableStateFlow<Long>(0xFF22C55EL)

    private val _accounts = repo.observeAll()

    val uiState: StateFlow<ManageAccountsUiState> = combine(
        _accounts,
        _editorOpen,
        _editingId,
        _draftName,
        _draftType,
        _draftIcon,
        _draftColor,
    ) { values ->
        @Suppress("UNCHECKED_CAST")
        val accounts = values[0] as List<Account>
        val editorOpen = values[1] as Boolean
        val editingId = values[2] as Long?
        val draftName = values[3] as String
        val draftType = values[4] as AccountType
        val draftIcon = values[5] as String
        val draftColor = values[6] as Long
        ManageAccountsUiState(
            accounts = accounts,
            editorOpen = editorOpen,
            editingId = editingId,
            draftName = draftName,
            draftType = draftType,
            draftIcon = draftIcon,
            draftColor = draftColor,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = ManageAccountsUiState(),
    )

    // ── Editor ───────────────────────────────────────────────────────────────

    fun startAdd() {
        _editingId.value = null
        _draftName.value = ""
        _draftType.value = AccountType.CASH
        _draftIcon.value = "💵"
        _draftColor.value = 0xFF22C55EL
        _editorOpen.value = true
    }

    fun startEdit(account: Account) {
        _editingId.value = account.id
        _draftName.value = account.name
        _draftType.value = account.type
        _draftIcon.value = account.icon
        _draftColor.value = account.color
        _editorOpen.value = true
    }

    fun onNameChange(name: String) {
        _draftName.value = name
    }

    fun onTypeChange(type: AccountType) {
        _draftType.value = type
    }

    fun onIconChange(icon: String) {
        _draftIcon.value = icon
    }

    fun onColorChange(color: Long) {
        _draftColor.value = color
    }

    fun closeEditor() {
        _editorOpen.value = false
    }

    fun saveEditor() {
        val account = Account(
            id = _editingId.value ?: 0L,
            name = _draftName.value.trim(),
            type = _draftType.value,
            icon = _draftIcon.value,
            color = _draftColor.value,
        )
        viewModelScope.launch {
            repo.upsert(account)
            _editorOpen.value = false
        }
    }
}
