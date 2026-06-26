package com.psy.ui.budget

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Budget
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.CategoryType
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.model.Account
import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.BudgetRepository
import com.psy.domain.repository.CategoryGroupRepository
import com.psy.domain.repository.CategoryRepository
import com.psy.domain.repository.LedgerRepository
import com.psy.domain.repository.TransactionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Editor mode
// ---------------------------------------------------------------------------

enum class EditorMode { TOTAL, CATEGORY }

// ---------------------------------------------------------------------------
// Internal editor snapshot (combined from multiple MutableStateFlows)
// ---------------------------------------------------------------------------

private data class EditorSnapshot(
    val open: Boolean,
    val editingBudget: Budget?,
    val mode: EditorMode,
    val groupId: Long?,
    val amountText: String,
)

/** Domain inputs bundled to keep the main combine within the 5-flow typed limit. */
private data class DomainData(
    val monthTxns: List<com.psy.domain.model.Transaction>,
    val budgets: List<Budget>,
    val categories: List<com.psy.domain.model.Category>,
    val groups: List<CategoryGroup>,
    val accounts: List<Account>,
)

// ---------------------------------------------------------------------------
// UI state data classes
// ---------------------------------------------------------------------------

/**
 * Represents the overall (total) budget for a ledger.
 * [budget] carries the full domain object so [BudgetViewModel.startEdit] can
 * be called directly without reconstructing it.
 */
data class TotalBudget(
    val budget: Budget,
    val limitMinor: Long,
    val spentMinor: Long,
    val percent: Float,
)

/**
 * Represents a per-category budget row.
 * [budget] carries the full domain object for the same reason.
 */
data class CategoryBudgetItem(
    val budget: Budget,
    val group: CategoryGroup?,
    val limitMinor: Long,
    val spentMinor: Long,
    val percent: Float,
)

data class BudgetUiState(
    val monthLabel: YearMonth = YearMonth.now(),
    val currency: Currency = Currency.VND,
    val total: TotalBudget? = null,
    val categoryBudgets: List<CategoryBudgetItem> = emptyList(),
    // Editor
    val editorOpen: Boolean = false,
    val editorMode: EditorMode = EditorMode.TOTAL,
    val editorGroupId: Long? = null,
    val draftAmountText: String = "",
    val availableGroups: List<CategoryGroup> = emptyList(),
    val isEditing: Boolean = false,
    val canSave: Boolean = false,
    val loading: Boolean = true,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class BudgetViewModel @Inject constructor(
    private val budgetRepo: BudgetRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val groupRepo: CategoryGroupRepository,
    private val ledgerRepo: LedgerRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {

    val selectedMonth: MutableStateFlow<YearMonth> = MutableStateFlow(YearMonth.now())

    // Editor sub-state — each exposed individually so the UI can update them granularly.
    private val _editorOpen = MutableStateFlow(false)
    private val _editingBudget = MutableStateFlow<Budget?>(null)
    private val _editorMode = MutableStateFlow(EditorMode.TOTAL)
    private val _editorGroupId = MutableStateFlow<Long?>(null)
    private val _draftAmountText = MutableStateFlow("")

    // Combined editor snapshot (avoids 8-arg combine in the main pipeline).
    private val editorSnapshot = combine(
        _editorOpen,
        _editingBudget,
        _editorMode,
        _editorGroupId,
        _draftAmountText,
    ) { open, editing, mode, groupId, text ->
        EditorSnapshot(open, editing, mode, groupId, text)
    }

    // Cached ledger id so save/remove can use it in a coroutine without waiting for flow.
    @Volatile
    private var _cachedLedgerId: Long? = null

    // ---------------------------------------------------------------------------
    // Month navigation
    // ---------------------------------------------------------------------------

    fun prevMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    // ---------------------------------------------------------------------------
    // Editor commands
    // ---------------------------------------------------------------------------

    fun startAddTotal() {
        _editorMode.value = EditorMode.TOTAL
        _editingBudget.value = null
        _editorGroupId.value = null
        _draftAmountText.value = ""
        _editorOpen.value = true
    }

    fun startAddCategory() {
        _editorMode.value = EditorMode.CATEGORY
        _editingBudget.value = null
        _editorGroupId.value = null
        _draftAmountText.value = ""
        _editorOpen.value = true
    }

    fun startEdit(budget: Budget) {
        _editingBudget.value = budget
        _editorMode.value = if (budget.groupId == null) EditorMode.TOTAL else EditorMode.CATEGORY
        _editorGroupId.value = budget.groupId
        // For VND (fractionDigits = 0) the stored amountMinor equals the integer the user typed.
        _draftAmountText.value = budget.amountMinor.toString()
        _editorOpen.value = true
    }

    fun onAmountChange(s: String) {
        _draftAmountText.value = s.filter { it.isDigit() }
    }

    fun selectEditorGroup(id: Long) {
        _editorGroupId.value = id
    }

    fun closeEditor() {
        _editorOpen.value = false
        _editingBudget.value = null
        _editorGroupId.value = null
        _draftAmountText.value = ""
    }

    fun saveEditor() {
        // For VND (fractionDigits = 0): typed integer IS the amount in minor units (đồng).
        val amount = _draftAmountText.value.filter { it.isDigit() }.toLongOrNull() ?: 0L
        val mode = _editorMode.value
        val groupId = if (mode == EditorMode.TOTAL) null else _editorGroupId.value

        if (amount <= 0L) return
        if (mode == EditorMode.CATEGORY && _editingBudget.value == null && groupId == null) return

        val ledgerId = _cachedLedgerId ?: return

        viewModelScope.launch {
            budgetRepo.setBudget(ledgerId, groupId, amount)
            closeEditor()
        }
    }

    fun removeEditor() {
        val toRemove = _editingBudget.value ?: return
        viewModelScope.launch {
            budgetRepo.removeBudget(toRemove)
            closeEditor()
        }
    }

    // ---------------------------------------------------------------------------
    // Main state pipeline
    // ---------------------------------------------------------------------------

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<BudgetUiState> = ledgerRepo.observeAll()
        .flatMapLatest { ledgers ->
            val ledger = ledgers.firstOrNull()
                ?: return@flatMapLatest flowOf(BudgetUiState(loading = false))

            _cachedLedgerId = ledger.id
            val currency = Currency.of(ledger.currency)
            val zone = ZoneId.systemDefault()

            selectedMonth.flatMapLatest { month ->
                val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                // Half-open range [monthStart, monthEnd)
                val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

                val monthTxnsFlow = transactionRepo.observeBetween(ledger.id, monthStart, monthEnd)
                val budgetsFlow = budgetRepo.observeAll(ledger.id)
                val categoriesFlow = categoryRepo.observeAll()
                val groupsFlow = groupRepo.observeByType(CategoryType.EXPENSE)

                // Combine domain data (5 flows) first, then zip with editor snapshot.
                val domainFlow = combine(monthTxnsFlow, budgetsFlow, categoriesFlow, groupsFlow,
                    accountRepo.observeAll()) {
                    monthTxns, budgets, categories, groups, accounts ->
                    DomainData(monthTxns, budgets, categories, groups, accounts)
                }

                combine(domainFlow, editorSnapshot) { (monthTxns, budgets, categories, groups, accounts), editor ->
                    val groupMap = groups.associateBy { it.id }
                    // leafId -> groupId so each EXPENSE tx can be attributed to its parent group.
                    val leafToGroup = categories.associate { it.id to it.groupId }

                    // Spent = EXPENSE only; INCOME + TRANSFER + fund-account txns excluded.
                    val fundAccountIds = accounts.filter { it.isFund }.map { it.id }.toSet()
                    val expenseTxns = monthTxns.filter {
                        it.type == TxType.EXPENSE && it.accountId !in fundAccountIds
                    }
                    val totalSpent = expenseTxns.sumOf { it.amountMinor }

                    // Total (uncategorised) budget
                    val totalBudgetDomain = budgets.firstOrNull { it.groupId == null }
                    val total = totalBudgetDomain?.let { b ->
                        val pct = if (b.amountMinor > 0) totalSpent.toFloat() / b.amountMinor else 0f
                        TotalBudget(
                            budget = b,
                            limitMinor = b.amountMinor,
                            spentMinor = totalSpent,
                            percent = pct,
                        )
                    }

                    // Per-group budgets — sorted descending by percent utilisation.
                    val categoryBudgets = budgets
                        .filter { it.groupId != null }
                        .mapNotNull { b ->
                            val group = groupMap[b.groupId]
                            // Sum EXPENSE txns whose leaf belongs to this budget's group.
                            val groupSpent = expenseTxns
                                .filter { tx -> leafToGroup[tx.categoryId] == b.groupId }
                                .sumOf { it.amountMinor }
                            val pct = if (b.amountMinor > 0) groupSpent.toFloat() / b.amountMinor else 0f
                            CategoryBudgetItem(
                                budget = b,
                                group = group,
                                limitMinor = b.amountMinor,
                                spentMinor = groupSpent,
                                percent = pct,
                            )
                        }
                        .sortedByDescending { it.percent }

                    // EXPENSE groups that are not yet budgeted (for the picker).
                    val budgetedGroupIds = budgets
                        .mapNotNull { it.groupId }
                        .toSet()
                    val availableGroups = groups
                        .filter { it.id !in budgetedGroupIds }

                    // canSave: amount > 0 AND (total mode OR editing OR group selected)
                    val parsedAmount = editor.amountText.filter { it.isDigit() }.toLongOrNull() ?: 0L
                    val canSave = parsedAmount > 0L &&
                        (editor.mode == EditorMode.TOTAL ||
                            editor.editingBudget != null ||
                            editor.groupId != null)

                    BudgetUiState(
                        monthLabel = month,
                        currency = currency,
                        total = total,
                        categoryBudgets = categoryBudgets,
                        editorOpen = editor.open,
                        editorMode = editor.mode,
                        editorGroupId = editor.groupId,
                        draftAmountText = editor.amountText,
                        availableGroups = availableGroups,
                        isEditing = editor.editingBudget != null,
                        canSave = canSave,
                        loading = false,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = BudgetUiState(loading = true),
        )
}
