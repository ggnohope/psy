package com.psy.ui.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
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
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneId
import javax.inject.Inject

// ---------------------------------------------------------------------------
// Data classes
// ---------------------------------------------------------------------------

data class DayCell(
    val date: LocalDate,
    val incomeMinor: Long,
    val expenseMinor: Long,
    val isToday: Boolean,
)

data class CalendarTxRow(
    val id: Long,
    val categoryName: String,
    val categoryIcon: String,
    val accountName: String,
    val toAccountName: String? = null,
    val type: TxType,
    val amountMinor: Long,
    val note: String,
    val photoUri: String? = null,
)

data class CalendarUiState(
    val monthLabel: YearMonth = YearMonth.now(),
    val currency: Currency = Currency.VND,
    val weeks: List<List<DayCell?>> = emptyList(),
    val selectedDay: LocalDate? = null,
    val dayRows: List<CalendarTxRow> = emptyList(),
    val loading: Boolean = true,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {

    val selectedMonth: MutableStateFlow<YearMonth> = MutableStateFlow(YearMonth.now())
    val selectedDay: MutableStateFlow<LocalDate?> = MutableStateFlow(null)

    fun prevMonth() {
        selectedMonth.value = selectedMonth.value.minusMonths(1)
        selectedDay.value = null
    }

    fun nextMonth() {
        selectedMonth.value = selectedMonth.value.plusMonths(1)
        selectedDay.value = null
    }

    fun selectDay(date: LocalDate) {
        selectedDay.value = date
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<CalendarUiState> = ledgerRepo.observeAll()
        .flatMapLatest { ledgers ->
            val ledger = ledgers.firstOrNull()
                ?: return@flatMapLatest flowOf(CalendarUiState(loading = false))

            val currency = Currency.of(ledger.currency)
            val zone = ZoneId.systemDefault()

            selectedMonth.flatMapLatest { month ->
                val monthStart = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
                val monthEnd = month.plusMonths(1).atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()

                combine(
                    transactionRepo.observeBetween(ledger.id, monthStart, monthEnd),
                    categoryRepo.observeAll(),
                    accountRepo.observeAll(),
                    selectedDay,
                ) { transactions, categories, accounts, selDay ->
                    val categoryMap = categories.associateBy { it.id }
                    val accountMap = accounts.associateBy { it.id }
                    val today = LocalDate.now(zone)

                    // ── Build day totals map (TRANSFER excluded from income/expense) ──
                    val dayTotalsMap = mutableMapOf<LocalDate, Pair<Long, Long>>() // date → (income, expense)
                    transactions.forEach { tx ->
                        val date = Instant.ofEpochMilli(tx.date).atZone(zone).toLocalDate()
                        val (inc, exp) = dayTotalsMap.getOrDefault(date, 0L to 0L)
                        when (tx.type) {
                            TxType.INCOME -> dayTotalsMap[date] = (inc + tx.amountMinor) to exp
                            TxType.EXPENSE -> dayTotalsMap[date] = inc to (exp + tx.amountMinor)
                            TxType.TRANSFER -> Unit // excluded from totals
                        }
                    }

                    // ── Build Monday-start grid ──────────────────────────────────
                    val firstOfMonth = month.atDay(1)
                    // Monday=1, Tuesday=2, ..., Sunday=7
                    // Leading nulls = dayOfWeek.value - 1 (MON → 0, SUN → 6)
                    val lead = firstOfMonth.dayOfWeek.value - 1
                    val daysInMonth = month.lengthOfMonth()

                    val cells = mutableListOf<DayCell?>()
                    // Leading nulls
                    repeat(lead) { cells.add(null) }
                    // Day cells
                    for (day in 1..daysInMonth) {
                        val date = month.atDay(day)
                        val (inc, exp) = dayTotalsMap.getOrDefault(date, 0L to 0L)
                        cells.add(
                            DayCell(
                                date = date,
                                incomeMinor = inc,
                                expenseMinor = exp,
                                isToday = date == today,
                            )
                        )
                    }
                    // Trailing nulls to complete the last week
                    val remainder = cells.size % 7
                    if (remainder != 0) {
                        repeat(7 - remainder) { cells.add(null) }
                    }

                    val weeks = cells.chunked(7)

                    // ── Selected day transaction rows ────────────────────────────
                    val dayRows: List<CalendarTxRow> = if (selDay != null) {
                        transactions
                            .filter { tx ->
                                Instant.ofEpochMilli(tx.date).atZone(zone).toLocalDate() == selDay
                            }
                            .map { tx ->
                                val cat = tx.categoryId?.let { categoryMap[it] }
                                val acc = accountMap[tx.accountId]
                                if (tx.type == TxType.TRANSFER) {
                                    val toAcc = tx.toAccountId?.let { accountMap[it] }
                                    CalendarTxRow(
                                        id = tx.id,
                                        categoryName = acc?.name ?: "—",
                                        categoryIcon = "🔄",
                                        accountName = acc?.name ?: "—",
                                        toAccountName = toAcc?.name ?: "—",
                                        type = tx.type,
                                        amountMinor = tx.amountMinor,
                                        note = tx.note,
                                        photoUri = tx.photoUri,
                                    )
                                } else {
                                    CalendarTxRow(
                                        id = tx.id,
                                        categoryName = cat?.name ?: "—",
                                        categoryIcon = cat?.icon ?: "📦",
                                        accountName = acc?.name ?: "—",
                                        toAccountName = null,
                                        type = tx.type,
                                        amountMinor = tx.amountMinor,
                                        note = tx.note,
                                        photoUri = tx.photoUri,
                                    )
                                }
                            }
                    } else {
                        emptyList()
                    }

                    CalendarUiState(
                        monthLabel = month,
                        currency = currency,
                        weeks = weeks,
                        selectedDay = selDay,
                        dayRows = dayRows,
                        loading = false,
                    )
                }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CalendarUiState(loading = true),
        )
}
