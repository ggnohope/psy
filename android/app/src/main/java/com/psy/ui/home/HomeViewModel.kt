package com.psy.ui.home

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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI state data classes
// ---------------------------------------------------------------------------

data class TxRow(
    val id: Long,
    val categoryName: String,
    val categoryIcon: String,
    val accountName: String,
    val type: TxType,
    val amountMinor: Long,
    val note: String,
)

data class DayGroup(
    val dateLabel: String,
    val items: List<TxRow>,
)

data class HomeUiState(
    val monthLabel: String = "",
    val incomeMinor: Long = 0L,
    val expenseMinor: Long = 0L,
    val netMinor: Long = 0L,
    val currency: Currency = Currency.VND,
    val days: List<DayGroup> = emptyList(),
    val loading: Boolean = true,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val ledgerRepo: LedgerRepository,
    private val transactionRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val accountRepo: AccountRepository,
) : ViewModel() {

    // Current-month half-open range [start, nextMonthStart) in epoch millis.
    // This matches the DAO: date >= start AND date < end.
    private val monthRange: Pair<Long, Long> = run {
        val zone = ZoneId.systemDefault()
        val today = LocalDate.now(zone)
        val firstOfMonth = today.withDayOfMonth(1)
        val firstOfNextMonth = firstOfMonth.plusMonths(1)

        val start = firstOfMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        val end = firstOfNextMonth.atStartOfDay(zone).toInstant().toEpochMilli()
        start to end
    }

    private val monthLabelString: String = run {
        val today = LocalDate.now(ZoneId.systemDefault())
        val formatter = DateTimeFormatter.ofPattern("MM/yyyy")
        today.format(formatter)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<HomeUiState> = ledgerRepo.observeAll()
        .flatMapLatest { ledgers ->
            val ledger = ledgers.firstOrNull()
                ?: return@flatMapLatest flowOf(HomeUiState(loading = false))

            val currency = Currency.of(ledger.currency)
            val (start, end) = monthRange

            combine(
                transactionRepo.observeBetween(ledger.id, start, end),
                categoryRepo.observeAll(),
                accountRepo.observeAll(),
            ) { transactions, categories, accounts ->
                val categoryMap = categories.associateBy { it.id }
                val accountMap = accounts.associateBy { it.id }

                var incomeMinor = 0L
                var expenseMinor = 0L

                // Group transactions by calendar day (epoch millis → LocalDate)
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val yesterday = today.minusDays(1)

                val grouped = transactions
                    .groupBy { tx ->
                        java.time.Instant.ofEpochMilli(tx.date)
                            .atZone(zone)
                            .toLocalDate()
                    }
                    .entries
                    .sortedByDescending { it.key }

                val days = grouped.map { (date, txList) ->
                    val label = when (date) {
                        today -> "Hôm nay"
                        yesterday -> "Hôm qua"
                        else -> date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    }

                    val rows = txList.map { tx ->
                        val cat = tx.categoryId?.let { categoryMap[it] }
                        val acc = accountMap[tx.accountId]
                        TxRow(
                            id = tx.id,
                            categoryName = cat?.name ?: "—",
                            categoryIcon = cat?.icon ?: "📦",
                            accountName = acc?.name ?: "—",
                            type = tx.type,
                            amountMinor = tx.amountMinor,
                            note = tx.note,
                        )
                    }

                    DayGroup(dateLabel = label, items = rows)
                }

                // Sum income and expense
                transactions.forEach { tx ->
                    when (tx.type) {
                        TxType.INCOME -> incomeMinor += tx.amountMinor
                        TxType.EXPENSE -> expenseMinor += tx.amountMinor
                        TxType.TRANSFER -> Unit // excluded from income/expense sums
                    }
                }

                HomeUiState(
                    monthLabel = monthLabelString,
                    incomeMinor = incomeMinor,
                    expenseMinor = expenseMinor,
                    netMinor = incomeMinor - expenseMinor,
                    currency = currency,
                    days = days,
                    loading = false,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = HomeUiState(loading = true),
        )
}
