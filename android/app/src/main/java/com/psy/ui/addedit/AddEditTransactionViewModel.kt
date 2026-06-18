package com.psy.ui.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import com.psy.domain.model.Currency
import com.psy.domain.model.Transaction
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
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
import java.time.LocalDate
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
    val categories: List<Category> = emptyList(),
    val accounts: List<Account> = emptyList(),
    val selectedCategoryId: Long? = null,
    val selectedAccountId: Long? = null,
    val date: Long = 0L,
    val note: String = "",
    val currency: Currency = Currency.VND,
    val canSave: Boolean = false,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class AddEditTransactionViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
    private val ledgerRepo: LedgerRepository,
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
            var prefillDate = todayEpochMillis()
            var prefillNote = ""

            if (isEdit) {
                val tx = txRepo.getById(txId)
                if (tx != null) {
                    initialType = tx.type
                    // Reverse the amount: amountMinor / 10^fractionDigits → whole units (Long string).
                    val divisor = pow10(currency.fractionDigits)
                    prefillAmountText = if (divisor > 0L) (tx.amountMinor / divisor).toString() else "0"
                    prefillCategoryId = tx.categoryId
                    prefillAccountId = tx.accountId
                    prefillDate = tx.date
                    prefillNote = tx.note
                    originalCreatedAt = tx.createdAt
                }
            } else {
                prefillDate = todayEpochMillis()
            }

            // 4. Load initial categories for the chosen type.
            val categories = categoryRepo.observeByType(initialType.toCategoryType()).first()

            val parsedAmount = prefillAmountText.toLongOrNull() ?: 0L
            val canSave = parsedAmount > 0 && prefillCategoryId != null && prefillAccountId != null

            _uiState.update {
                AddEditUiState(
                    isEdit = isEdit,
                    type = initialType,
                    amountText = prefillAmountText,
                    categories = categories,
                    accounts = accounts,
                    selectedCategoryId = prefillCategoryId,
                    selectedAccountId = prefillAccountId,
                    date = prefillDate,
                    note = prefillNote,
                    currency = currency,
                    canSave = canSave,
                )
            }
        }
    }

    // ---------------------------------------------------------------------------
    // Event handlers
    // ---------------------------------------------------------------------------

    fun onTypeChange(newType: TxType) {
        viewModelScope.launch {
            val categories = categoryRepo.observeByType(newType.toCategoryType()).first()
            val currentCategoryId = _uiState.value.selectedCategoryId
            // Clear selection if it doesn't exist in the new type's category list.
            val newCategoryId = if (categories.any { it.id == currentCategoryId }) currentCategoryId else null
            _uiState.update { state ->
                state.copy(
                    type = newType,
                    categories = categories,
                    selectedCategoryId = newCategoryId,
                    canSave = computeCanSave(state.amountText, newCategoryId, state.selectedAccountId),
                )
            }
        }
    }

    fun onAmountChange(text: String) {
        // Strip non-digit characters to keep amountText clean.
        val digits = text.filter { it.isDigit() }
        _uiState.update { state ->
            state.copy(
                amountText = digits,
                canSave = computeCanSave(digits, state.selectedCategoryId, state.selectedAccountId),
            )
        }
    }

    fun selectCategory(id: Long) {
        _uiState.update { state ->
            state.copy(
                selectedCategoryId = id,
                canSave = computeCanSave(state.amountText, id, state.selectedAccountId),
            )
        }
    }

    fun selectAccount(id: Long) {
        _uiState.update { state ->
            state.copy(
                selectedAccountId = id,
                canSave = computeCanSave(state.amountText, state.selectedCategoryId, id),
            )
        }
    }

    fun onDateChange(epochMillis: Long) {
        _uiState.update { it.copy(date = epochMillis) }
    }

    fun onNoteChange(text: String) {
        _uiState.update { it.copy(note = text) }
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

            val tx = Transaction(
                id = if (isEdit) txId else 0L,
                ledgerId = activeLedgerId,
                type = state.type,
                amountMinor = amountMinor,
                categoryId = state.selectedCategoryId,
                accountId = state.selectedAccountId!!,
                note = state.note,
                date = state.date,
                createdAt = if (isEdit) originalCreatedAt else now,
                updatedAt = now,
            )
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
        categoryId: Long?,
        accountId: Long?,
    ): Boolean {
        val amount = amountText.toLongOrNull() ?: 0L
        return amount > 0L && categoryId != null && accountId != null
    }

    private fun pow10(n: Int): Long {
        var result = 1L
        repeat(n) { result *= 10L }
        return result
    }

    private fun todayEpochMillis(): Long {
        val zone = ZoneId.systemDefault()
        return LocalDate.now(zone).atStartOfDay(zone).toInstant().toEpochMilli()
    }
}

// Extension to map TxType → CategoryType (mirrors the enum relationship)
private fun TxType.toCategoryType(): CategoryType = when (this) {
    TxType.INCOME -> CategoryType.INCOME
    TxType.EXPENSE -> CategoryType.EXPENSE
    TxType.TRANSFER -> CategoryType.EXPENSE // fallback; Transfer UI (Task 5) will handle this
}
