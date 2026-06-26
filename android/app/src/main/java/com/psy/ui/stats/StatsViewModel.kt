package com.psy.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Account
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryGroup
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.CategoryGroupRepository
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

data class TopLeaf(
    val name: String,
    val icon: String,
    val amountMinor: Long,
    val percentInGroup: Float,
    val count: Int,
)

data class TopGroup(
    val groupId: Long,
    val name: String,
    val icon: String,
    val color: Long,
    val amountMinor: Long,
    val percentOfTotal: Float,
    val count: Int,
    val children: List<TopLeaf>,
)

/** Categories (leaves) + their parent groups, bundled to fit the 5-flow combine limit. */
private data class CategoryData(
    val categories: List<Category>,
    val groups: List<CategoryGroup>,
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
    val isFund: Boolean = false,
)

data class StatsUiState(
    val monthLabel: YearMonth = YearMonth.now(),
    val currency: Currency = Currency.VND,
    val summary: StatsSummary = StatsSummary(),
    val pieMode: TxType = TxType.EXPENSE,
    val slices: List<PieSlice> = emptyList(),
    val top: List<TopGroup> = emptyList(),
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
    private val groupRepo: CategoryGroupRepository,
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

                // Bundle leaves + groups into one flow so the main combine stays at 5 slots.
                val categoryDataFlow = combine(
                    categoryRepo.observeAll(),
                    groupRepo.observeAll(),
                ) { categories, groups -> CategoryData(categories, groups) }

                combine(
                    windowFlow,
                    categoryDataFlow,
                    pieMode,
                    accountRepo.observeAll(),
                    accountFilter,
                ) { windowTxns, categoryData, currentPieMode, accounts, currentAccountFilter ->
                    val categories = categoryData.categories
                    val groups = categoryData.groups
                    val categoryMap = categories.associateBy { it.id }
                    val groupMap = groups.associateBy { it.id }

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
                                isFund = acc.isFund,
                            )
                        }
                        .sortedByDescending { it.incomeMinor + it.expenseMinor }

                    // Drop the filter if the selected account no longer exists.
                    val effectiveFilter = currentAccountFilter?.takeIf { accountMap.containsKey(it) }

                    // Fund accounts: excluded from the "Tất cả" view; an explicit account
                    // filter (even a fund) still shows that account's real numbers.
                    val fundAccountIds = accounts.filter { it.isFund }.map { it.id }.toSet()
                    val filteredWindow =
                        if (effectiveFilter == null) windowTxns.filter { it.accountId !in fundAccountIds }
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

                    // ── Pie slices + top groups (2-level) ─────────────────────
                    val pieTxns = monthTxns.filter { tx ->
                        tx.type == currentPieMode && tx.categoryId != null
                    }

                    // Group pieTxns by their leaf's parent group; skip txns whose leaf is gone.
                    // groupId -> (leafId -> list of txns)
                    val byGroup = HashMap<Long, HashMap<Long, MutableList<Long>>>()
                    pieTxns.forEach { tx ->
                        val leaf = categoryMap[tx.categoryId] ?: return@forEach
                        byGroup
                            .getOrPut(leaf.groupId) { HashMap() }
                            .getOrPut(leaf.id) { mutableListOf() }
                            .add(tx.amountMinor)
                    }

                    // Pie slices get distinct colors from a fixed palette by index, so the
                    // chart is always readable even when groups share the same color.
                    val piePalette = listOf(
                        0xFFA18CFFL, 0xFF7FD8FFL, 0xFFFF8FD6L, 0xFFFF5FA2L, 0xFF22C55EL,
                        0xFFFFB86BL, 0xFF6BCB77L, 0xFF4D96FFL, 0xFFFF6B6BL, 0xFFB088F9L,
                    )

                    // Group totals, sorted desc — drives both pie slices and the top list,
                    // so palette index (=> color) matches between pie and list rows.
                    data class GroupAgg(val groupId: Long, val amount: Long, val leaves: Map<Long, List<Long>>)
                    val sortedGroups = byGroup
                        .map { (gid, leafMap) ->
                            val amount = leafMap.values.sumOf { amounts -> amounts.sum() }
                            GroupAgg(gid, amount, leafMap)
                        }
                        .sortedByDescending { it.amount }

                    val pieTotal = sortedGroups.sumOf { it.amount }

                    val slices = sortedGroups.mapIndexedNotNull { index, agg ->
                        val group = groupMap[agg.groupId] ?: return@mapIndexedNotNull null
                        PieSlice(group.name, agg.amount, piePalette[index % piePalette.size])
                    }

                    val top = sortedGroups.mapIndexedNotNull { index, agg ->
                        val group = groupMap[agg.groupId] ?: return@mapIndexedNotNull null
                        val color = piePalette[index % piePalette.size]
                        val groupAmount = agg.amount
                        val groupCount = agg.leaves.values.sumOf { it.size }
                        val children = agg.leaves
                            .mapNotNull { (leafId, amounts) ->
                                val leaf = categoryMap[leafId] ?: return@mapNotNull null
                                val leafAmount = amounts.sum()
                                TopLeaf(
                                    name = leaf.name,
                                    icon = leaf.icon,
                                    amountMinor = leafAmount,
                                    percentInGroup = if (groupAmount > 0L) {
                                        leafAmount.toFloat() / groupAmount.toFloat()
                                    } else 0f,
                                    count = amounts.size,
                                )
                            }
                            .sortedByDescending { it.amountMinor }
                        TopGroup(
                            groupId = agg.groupId,
                            name = group.name,
                            icon = group.icon,
                            color = color,
                            amountMinor = groupAmount,
                            percentOfTotal = if (pieTotal > 0L) {
                                groupAmount.toFloat() / pieTotal.toFloat()
                            } else 0f,
                            count = groupCount,
                            children = children,
                        )
                    }

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
