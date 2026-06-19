package com.psy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.CategoryRepository
import com.psy.domain.repository.LedgerRepository
import com.psy.domain.repository.TransactionRepository
import com.psy.ui.components.charts.MonthBars
import com.psy.ui.components.charts.PieSlice
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI state
// ---------------------------------------------------------------------------

data class StatsSummary(
    val incomeMinor: Long = 0L,
    val expenseMinor: Long = 0L,
    val netMinor: Long = 0L,
    val avgPerDayMinor: Long = 0L,
)

data class TopEntry(
    val category: Category,
    val amountMinor: Long,
    val percent: Float,
)

/** Income/expense rollup for a single account in the selected month (transfers excluded). */
data class AccountStat(
    val id: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val netMinor: Long,
)

data class StatsUiState(
    val monthLabel: YearMonth = YearMonth.now(),
    val currency: Currency = Currency.VND,
    val summary: StatsSummary = StatsSummary(),
    val pieMode: TxType = TxType.EXPENSE,
    val slices: List<PieSlice> = emptyList(),
    val top: List<TopEntry> = emptyList(),
    val trend: List<MonthBars> = emptyList(),
    // Account dimension
    val accounts: List<Account> = emptyList(),
    val accountBreakdown: List<AccountStat> = emptyList(),
    val selectedAccountId: Long? = null,
    val loading: Boolean = true,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class StatsViewModel @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {

    val selectedMonth: MutableStateFlow<YearMonth> = MutableStateFlow(YearMonth.now())
    val pieMode: MutableStateFlow<TxType> = MutableStateFlow(TxType.EXPENSE)

    /** null = "Tất cả" (all accounts); otherwise stats are filtered to this account. */
    val accountFilter: MutableStateFlow<Long?> = MutableStateFlow(null)

    fun prevMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
    }

    fun setPieMode(type: TxType) {
        pieMode.value = type
    }

    fun selectAccount(id: Long?) {
        accountFilter.value = id
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<StatsUiState> = ledgerRepo.observeAll()
        .flatMapLatest { ledgers ->
            val ledger = ledgers.firstOrNull()
                ?: return@flatMapLatest flowOf(StatsUiState(loading = false))

            val currency = Currency.of(ledger.currency)
            val zone = ZoneId.systemDefault()

            // React to selectedMonth: switch queries on month change
            selectedMonth.flatMapLatest { month ->
                val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

                // 6-month trend window: [month-5, month+1)
                val trendStart = month.minusMonths(5).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

                // Query 6-month window; derive current month subset from it
                val windowFlow = transactionRepo.observeBetween(ledger.id, trendStart, monthEnd)

                combine(
                    windowFlow,
                    categoryRepo.observeAll(),
                    pieMode,
                    accountRepo.observeAll(),
                    accountFilter,
                ) { windowTxns, categories, currentPieMode, accounts, currentAccountFilter ->
                    val categoryMap = categories.associateBy { it.id }

                    // ── Per-account breakdown (computed from ALL accounts, before filtering) ──
                    // Always reflects the full month so the comparison card can compare accounts.
                    val monthTxnsAll = windowTxns.filter { tx -> tx.date >= monthStart && tx.date < monthEnd }
                    val accountMap = accounts.associateBy { it.id }
                    val byAccount = HashMap<Long, LongArray>() // accountId -> [income, expense]
                    monthTxnsAll.forEach { tx ->
                        when (tx.type) {
                            TxType.INCOME -> byAccount.getOrPut(tx.accountId) { LongArray(2) }[0] += tx.amountMinor
                            TxType.EXPENSE -> byAccount.getOrPut(tx.accountId) { LongArray(2) }[1] += tx.amountMinor
                            TxType.TRANSFER -> Unit // transfers are not income/expense
                        }
                    }
                    val accountBreakdown = byAccount
                        .mapNotNull { (id, sums) ->
                            val acc = accountMap[id] ?: return@mapNotNull null
                            AccountStat(
                                id = acc.id,
                                name = acc.name,
                                icon = acc.icon,
                                color = acc.color,
                                incomeMinor = sums[0],
                                expenseMinor = sums[1],
                                netMinor = sums[0] - sums[1],
                            )
                        }
                        .sortedByDescending { it.incomeMinor + it.expenseMinor }

                    // Drop the filter if the selected account no longer exists.
                    val effectiveFilter = currentAccountFilter?.takeIf { accountMap.containsKey(it) }

                    // Apply account filter to the whole window; summary/pie/trend/top derive from it.
                    val filteredWindow =
                        if (effectiveFilter == null) windowTxns
                        else windowTxns.filter { it.accountId == effectiveFilter }

                    // Separate month txns from the (filtered) window
                    val monthTxns = filteredWindow.filter { tx -> tx.date >= monthStart && tx.date < monthEnd }

                    // ── Summary ──────────────────────────────────────────────
                    var incomeMinor = 0L
                    var expenseMinor = 0L
                    monthTxns.forEach { tx ->
                        when (tx.type) {
                            TxType.INCOME -> incomeMinor += tx.amountMinor
                            TxType.EXPENSE -> expenseMinor += tx.amountMinor
                            TxType.TRANSFER -> Unit
                        }
                    }
                    val today = LocalDate.now(zone)
                    val currentYM = YearMonth.now()
                    val daysToCount = if (month == currentYM) today.dayOfMonth else month.lengthOfMonth()
                    val avgPerDayMinor = expenseMinor / maxOf(1, daysToCount)

                    // ── Pie slices ────────────────────────────────────────────
                    val pieTxns = monthTxns.filter { tx ->
                        tx.type == currentPieMode && tx.categoryId != null
                    }
                    val pieByCategory = pieTxns
                        .groupBy { it.categoryId!! }
                        .mapValues { (_, txList) -> txList.sumOf { it.amountMinor } }

                    // Pie slices get distinct colors from a fixed palette by index, so the
                    // chart is always readable even when categories share the same color.
                    val piePalette = listOf(
                        0xFFA18CFFL, 0xFF7FD8FFL, 0xFFFF8FD6L, 0xFFFF5FA2L, 0xFF22C55EL,
                        0xFFFFB86BL, 0xFF6BCB77L, 0xFF4D96FFL, 0xFFFF6B6BL, 0xFFB088F9L,
                    )
                    val slices = pieByCategory
                        .mapNotNull { (catId, amount) ->
                            val cat = categoryMap[catId] ?: return@mapNotNull null
                            cat.name to amount
                        }
                        .sortedByDescending { it.second }
                        .mapIndexed { index, (name, amount) ->
                            PieSlice(name, amount, piePalette[index % piePalette.size])
                        }

                    // ── Top entries ───────────────────────────────────────────
                    val pieTotal = slices.sumOf { it.amountMinor }
                    val top = pieByCategory
                        .mapNotNull { (catId, amount) ->
                            val cat = categoryMap[catId] ?: return@mapNotNull null
                            val percent = if (pieTotal > 0L) amount.toFloat() / pieTotal.toFloat() else 0f
                            TopEntry(cat, amount, percent)
                        }
                        .sortedByDescending { it.amountMinor }
                        .take(10)

                    // ── Trend (6 months) ──────────────────────────────────────
                    val trendMonths = (5 downTo 0).map { offset -> month.minusMonths(offset.toLong()) }
                    val trend = trendMonths.map { ym ->
                        val ymStart = ym.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        val ymEnd = ym.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                        val ymTxns = filteredWindow.filter { tx -> tx.date >= ymStart && tx.date < ymEnd }

                        var ymIncome = 0L
                        var ymExpense = 0L
                        ymTxns.forEach { tx ->
                            when (tx.type) {
                                TxType.INCOME -> ymIncome += tx.amountMinor
                                TxType.EXPENSE -> ymExpense += tx.amountMinor
                                TxType.TRANSFER -> Unit
                            }
                        }
                        val label = "${ym.monthValue}/${ym.year % 100}"
                        MonthBars(label, ymIncome, ymExpense)
                    }

                    StatsUiState(
                        monthLabel = month,
                        currency = currency,
                        summary = StatsSummary(
                            incomeMinor = incomeMinor,
                            expenseMinor = expenseMinor,
                            netMinor = incomeMinor - expenseMinor,
                            avgPerDayMinor = avgPerDayMinor,
                        ),
                        pieMode = currentPieMode,
                        slices = slices,
                        top = top,
                        trend = trend,
                        accounts = accounts,
                        accountBreakdown = accountBreakdown,
                        selectedAccountId = effectiveFilter,
                        loading = false,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = StatsUiState(loading = true),
        )
}
