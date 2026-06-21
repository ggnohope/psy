package com.psy.ui.addedit

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.data.photo.PhotoStorage
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import com.psy.domain.model.Currency
import com.psy.domain.model.Transaction
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.CategoryGroupRepository
import com.psy.domain.repository.CategoryRepository
import com.psy.domain.repository.LedgerRepository
import com.psy.domain.repository.TransactionRepository
import com.psy.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class AddEditUiState(
    val isEdit: Boolean = false,
    val type: TxType = TxType.EXPENSE,
    /**
     * Amount parsing rule (v1, VND-focused):
     * - amountText holds raw digit characters the user typed (non-digits stripped on input).
     * - On save: parse amountText as a Long integer → this is the "whole-unit" value.
     * - amountMinor = typedLong * 10^currency.fractionDigits.
     * - For VND (fractionDigits = 0): amountMinor = typedLong (i.e., typed integer IS the minor amount).
     * - For USD (fractionDigits = 2): amountMinor = typedLong * 100
     *   (user types whole dollars; cents assumed zero — acceptable for v1 simplicity).
     * - Empty or zero amountText → canSave = false.
     */
    val amountText: String = "",
    /** Parent groups for the current type (empty for TRANSFER). */
    val groups: List<CategoryGroup> = emptyList(),
    /** Leaf categories of the currently selected group. */
    val leaves: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    /** Currently selected parent group id; null for TRANSFER or when no groups. */
    val selectedGroupId: Long? = null,
    /** Currently selected LEAF category id. */
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    /** Destination account for TRANSFER type; null for INCOME/EXPENSE. */
    val toAccountId: Long? = null,
    val date: Long = 0L,
    val note: String = "",
    val currency: Currency = Currency.VND,
    val canSave: Boolean = false,
    /** Absolute path of the attached photo stored in internal storage; null if none. */
    val photoUri: String? = null,
    /** Transient one-liner error message; null when no error pending. */
    val photoErrorMessage: String? = null,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val groupRepo: CategoryGroupRepository,
    private val accountRepo: AccountRepository,
    private val ledgerRepo: LedgerRepository,
    private val photoStorage: PhotoStorage,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    // txId = -1L means new transaction; any other value means editing.
    private val txId: Long = savedStateHandle[Routes.ARG_TX_ID] ?: -1L
    private val isEdit: Boolean get() = txId != -1L

    private val _uiState = MutableStateFlow(AddEditUiState())
    val uiState: StateFlow<AddEditUiState> = _uiState.asStateFlow()

    // One-shot done signal: sent exactly once per save/delete, consumed by LaunchedEffect.
    private val _doneChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val doneEvent = _doneChannel.receiveAsFlow()

    // Track the ledger id and the original createdAt for edits.
    private var activeLedgerId: Long = 0L
    private var originalCreatedAt: Long = 0L

    init {
        viewModelScope.launch {
            // 1. Resolve the active ledger → currency and ledgerId.
            val ledger = ledgerRepo.firstOrNull()
            val currency = if (ledger != null) Currency.of(ledger.currency) else Currency.VND
            activeLedgerId = ledger?.id ?: 0L

            // 2. Load all accounts; default-select the first one.
            val accounts = accountRepo.observeAll().first()
            val defaultAccountId = accounts.firstOrNull()?.id

            // 3. Determine initial type (EXPENSE by default, or from the loaded tx).
            // If editing, load the transaction first to know its type.
            var initialType = TxType.EXPENSE
            var prefillAmountText = ""
            var prefillCategoryId: Long? = null
            var prefillAccountId = defaultAccountId
            var prefillToAccountId: Long? = null
            var prefillDate = nowEpochMillis()
            var prefillNote = ""
            var prefillPhotoUri: String? = null

            if (isEdit) {
                val tx = txRepo.getById(txId)
                if (tx != null) {
                    initialType = tx.type
                    // Reverse the amount: amountMinor / 10^fractionDigits → whole units (Long string).
                    val divisor = pow10(currency.fractionDigits)
                    prefillAmountText = if (divisor > 0L) (tx.amountMinor / divisor).toString() else "0"
                    if (tx.type == TxType.TRANSFER) {
                        // TRANSFER: category is null; prefill both from/to accounts.
                        prefillCategoryId = null
                        prefillAccountId = tx.accountId
                        prefillToAccountId = tx.toAccountId
                    } else {
                        prefillCategoryId = tx.categoryId
                        prefillAccountId = tx.accountId
                        prefillToAccountId = null
                    }
                    prefillDate = tx.date
                    prefillNote = tx.note
                    prefillPhotoUri = tx.photoUri
                    originalCreatedAt = tx.createdAt
                }
            } else {
                prefillDate = nowEpochMillis()
            }

            // 4. Load groups + leaves for the chosen type (empty for TRANSFER).
            var groups: List<CategoryGroup> = emptyList()
            var leaves: List<Category> = emptyList()
            var selectedGroupId: Long? = null
            if (initialType != TxType.TRANSFER) {
                groups = groupRepo.observeByType(initialType.toCategoryType()).first()
                if (isEdit && prefillCategoryId != null) {
                    // Find the leaf's parent group so the picker shows the right selection.
                    val allLeaves = categoryRepo.observeAll().first()
                    val leaf = allLeaves.firstOrNull { it.id == prefillCategoryId }
                    selectedGroupId = leaf?.groupId ?: groups.firstOrNull()?.id
                } else {
                    selectedGroupId = groups.firstOrNull()?.id
                }
                if (selectedGroupId != null) {
                    leaves = categoryRepo.observeByGroup(selectedGroupId).first()
                }
                // For NEW transactions, default-select the first leaf of the group.
                if (!isEdit) {
                    prefillCategoryId = leaves.firstOrNull()?.id
                }
            }

            val canSave = computeCanSave(
                amountText = prefillAmountText,
                type = initialType,
                categoryId = prefillCategoryId,
                accountId = prefillAccountId,
                toAccountId = prefillToAccountId,
            )

            _uiState.update {
                AddEditUiState(
                    isEdit = isEdit,
                    type = initialType,
                    amountText = prefillAmountText,
                    groups = groups,
                    leaves = leaves,
                    accounts = accounts,
                    selectedGroupId = selectedGroupId,
                    selectedCategoryId = prefillCategoryId,
                    selectedAccountId = prefillAccountId,
                    toAccountId = prefillToAccountId,
                    date = prefillDate,
                    note = prefillNote,
                    currency = currency,
                    canSave = canSave,
                    photoUri = prefillPhotoUri,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Event handlers
    // ---------------------------------------------------------------------------

    fun onTypeChange(newType: TxType) {
        viewModelScope.launch {
            if (newType == TxType.TRANSFER) {
                // TRANSFER: clear category; keep accounts; no groups/leaves needed.
                _uiState.update { state ->
                    state.copy(
                        type = TxType.TRANSFER,
                        groups = emptyList(),
                        leaves = emptyList(),
                        selectedGroupId = null,
                        selectedCategoryId = null,
                        // toAccountId stays null until user picks; from account keeps default.
                        canSave = computeCanSave(
                            amountText = state.amountText,
                            type = TxType.TRANSFER,
                            categoryId = null,
                            accountId = state.selectedAccountId,
                            toAccountId = state.toAccountId,
                        ),
                    )
                }
            } else {
                // INCOME or EXPENSE: reload groups → first group's leaves; clear toAccountId.
                val groups = groupRepo.observeByType(newType.toCategoryType()).first()
                val selectedGroupId = groups.firstOrNull()?.id
                val leaves = if (selectedGroupId != null) {
                    categoryRepo.observeByGroup(selectedGroupId).first()
                } else {
                    emptyList()
                }
                val newCategoryId = leaves.firstOrNull()?.id
                _uiState.update { state ->
                    state.copy(
                        type = newType,
                        groups = groups,
                        leaves = leaves,
                        selectedGroupId = selectedGroupId,
                        selectedCategoryId = newCategoryId,
                        toAccountId = null,
                        canSave = computeCanSave(
                            amountText = state.amountText,
                            type = newType,
                            categoryId = newCategoryId,
                            accountId = state.selectedAccountId,
                            toAccountId = null,
                        ),
                    )
                }
            }
        }
    }

    fun onAmountChange(text: String) {
        // Strip non-digit characters to keep amountText clean.
        val digits = text.filter { it.isDigit() }
        _uiState.update { state ->
            state.copy(
                amountText = digits,
                canSave = computeCanSave(
                    amountText = digits,
                    type = state.type,
                    categoryId = state.selectedCategoryId,
                    accountId = state.selectedAccountId,
                    toAccountId = state.toAccountId,
                ),
            )
        }
    }

    fun selectGroup(groupId: Long) {
        viewModelScope.launch {
            val leaves = categoryRepo.observeByGroup(groupId).first()
            val newCategoryId = leaves.firstOrNull()?.id
            _uiState.update { state ->
                state.copy(
                    selectedGroupId = groupId,
                    leaves = leaves,
                    selectedCategoryId = newCategoryId,
                    canSave = computeCanSave(
                        amountText = state.amountText,
                        type = state.type,
                        categoryId = newCategoryId,
                        accountId = state.selectedAccountId,
                        toAccountId = state.toAccountId,
                    ),
                )
            }
        }
    }

    fun selectCategory(id: Long) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = id,
                canSave = computeCanSave(
                    amountText = state.amountText,
                    type = state.type,
                    categoryId = id,
                    accountId = state.selectedAccountId,
                    toAccountId = state.toAccountId,
                ),
            )
        }
    }

    fun selectAccount(id: Long) {
        _uiState.update { state ->
            state.copy(
                selectedAccountId = id,
                canSave = computeCanSave(
                    amountText = state.amountText,
                    type = state.type,
                    categoryId = state.selectedCategoryId,
                    accountId = id,
                    toAccountId = state.toAccountId,
                ),
            )
        }
    }

    fun onToAccountChange(id: Long) {
        _uiState.update { state ->
            state.copy(
                toAccountId = id,
                canSave = computeCanSave(
                    amountText = state.amountText,
                    type = state.type,
                    categoryId = state.selectedCategoryId,
                    accountId = state.selectedAccountId,
                    toAccountId = id,
                ),
            )
        }
    }

    /**
     * Update the calendar day from a DatePicker (which yields start-of-day millis),
     * while preserving the currently selected time-of-day (hour:minute) of [date].
     */
    fun onDateChange(epochMillis: Long) {
        _uiState.update { state ->
            val zone = ZoneId.systemDefault()
            val current = Instant.ofEpochMilli(state.date).atZone(zone)
            val newDay = Instant.ofEpochMilli(epochMillis).atZone(zone).toLocalDate()
            val combined = newDay
                .atTime(current.hour, current.minute, current.second)
                .atZone(zone)
                .toInstant()
                .toEpochMilli()
            state.copy(date = combined)
        }
    }

    /** Set the time-of-day on the existing calendar day of [date]. */
    fun onTimeChange(hour: Int, minute: Int) {
        _uiState.update { state ->
            val zone = ZoneId.systemDefault()
            val updated = Instant.ofEpochMilli(state.date)
                .atZone(zone)
                .withHour(hour)
                .withMinute(minute)
                .withSecond(0)
                .toInstant()
                .toEpochMilli()
            state.copy(date = updated)
        }
    }

    fun onNoteChange(text: String) {
        _uiState.update { it.copy(note = text) }
    }

    // ---------------------------------------------------------------------------
    // Photo attachment
    // ---------------------------------------------------------------------------

    fun onPickPhoto(uri: Uri) {
        viewModelScope.launch {
            // Clear any previous error
            _uiState.update { it.copy(photoErrorMessage = null) }
            val name = "img_${System.currentTimeMillis()}"
            try {
                val path = photoStorage.savePicked(uri, name)
                _uiState.update { it.copy(photoUri = path) }
            } catch (e: Exception) {
                // Non-blocking: leave photoUri unchanged, surface a transient error
                _uiState.update {
                    it.copy(photoErrorMessage = "Không thể đính kèm ảnh: ${e.message}")
                }
            }
        }
    }

    fun onRemovePhoto() {
        val oldPath = _uiState.value.photoUri
        _uiState.update { it.copy(photoUri = null, photoErrorMessage = null) }
        if (oldPath != null) {
            viewModelScope.launch {
                photoStorage.delete(oldPath)
            }
        }
    }

    fun clearPhotoError() {
        _uiState.update { it.copy(photoErrorMessage = null) }
    }

    // ---------------------------------------------------------------------------
    // Save / Delete
    // ---------------------------------------------------------------------------

    fun save(now: Long) {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            _uiState.update { it.copy(canSave = false) }
            val typed = state.amountText.toLongOrNull() ?: 0L
            val divisor = pow10(state.currency.fractionDigits)
            val amountMinor = typed * divisor

            val tx = if (state.type == TxType.TRANSFER) {
                Transaction(
                    id = if (isEdit) txId else 0L,
                    ledgerId = activeLedgerId,
                    type = TxType.TRANSFER,
                    amountMinor = amountMinor,
                    categoryId = null,
                    accountId = state.selectedAccountId!!,
                    toAccountId = state.toAccountId,
                    note = state.note,
                    date = state.date,
                    createdAt = if (isEdit) originalCreatedAt else now,
                    updatedAt = now,
                    photoUri = state.photoUri,
                )
            } else {
                Transaction(
                    id = if (isEdit) txId else 0L,
                    ledgerId = activeLedgerId,
                    type = state.type,
                    amountMinor = amountMinor,
                    categoryId = state.selectedCategoryId,
                    accountId = state.selectedAccountId!!,
                    toAccountId = null,
                    note = state.note,
                    date = state.date,
                    createdAt = if (isEdit) originalCreatedAt else now,
                    updatedAt = now,
                    photoUri = state.photoUri,
                )
            }
            txRepo.upsert(tx)
            _doneChannel.send(Unit)
        }
    }

    fun delete() {
        if (!isEdit) return
        viewModelScope.launch {
            val tx = txRepo.getById(txId) ?: return@launch
            txRepo.delete(tx)
            _doneChannel.send(Unit)
        }
    }

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    private fun computeCanSave(
        amountText: String,
        type: TxType,
        categoryId: Long?,
        accountId: Long?,
        toAccountId: Long?,
    ): Boolean {
        val amount = amountText.toLongOrNull() ?: 0L
        if (amount <= 0L) return false
        return when (type) {
            TxType.INCOME, TxType.EXPENSE -> categoryId != null && accountId != null
            TxType.TRANSFER -> accountId != null && toAccountId != null && accountId != toAccountId
        }
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10L }
        return result
    }

    private fun nowEpochMillis(): Long = Instant.now().toEpochMilli()
}

// Extension to map TxType → CategoryType (mirrors the enum relationship)
private fun TxType.toCategoryType(): CategoryType = when (this) {
    TxType.INCOME -> CategoryType.INCOME
    TxType.EXPENSE -> CategoryType.EXPENSE
    TxType.TRANSFER -> CategoryType.EXPENSE // fallback; Transfer UI (Task 5) will handle this
}
