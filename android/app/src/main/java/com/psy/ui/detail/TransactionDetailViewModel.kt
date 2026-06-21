package com.psy.ui.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.repository.AccountRepository
import com.psy.domain.repository.CategoryGroupRepository
import com.psy.domain.repository.CategoryRepository
import com.psy.domain.repository.LedgerRepository
import com.psy.domain.repository.TransactionRepository
import com.psy.ui.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import javax.inject.Inject

// ---------------------------------------------------------------------------
// UI state (read-only)
// ---------------------------------------------------------------------------

data class TransactionDetailUiState(
    val loading: Boolean = true,
    val found: Boolean = false,
    val icon: String = "",
    val title: String = "",          // leaf name (or account name for TRANSFER)
    val ledgerName: String = "",
    val dateLabel: String = "",      // yyyy-MM-dd
    val timeLabel: String = "",      // HH:mm
    val accountName: String = "",
    val toAccountName: String? = null,   // TRANSFER only
    val categoryLabel: String = "",  // "${group.name}(${type})" e.g. "Vận tải(Chi)"; empty for TRANSFER
    val amountMinor: Long = 0L,
    val type: TxType = TxType.EXPENSE,
    val currency: Currency = Currency.VND,
    val note: String = "",           // raw note
    val photoUri: String? = null,
)

// ---------------------------------------------------------------------------
// ViewModel
// ---------------------------------------------------------------------------

@HiltViewModel
class TransactionDetailViewModel @Inject constructor(
    private val txRepo: TransactionRepository,
    private val categoryRepo: CategoryRepository,
    private val groupRepo: CategoryGroupRepository,
    private val accountRepo: AccountRepository,
    private val ledgerRepo: LedgerRepository,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val txId: Long = savedStateHandle[Routes.ARG_TX_ID] ?: -1L

    // Reactive state: recomputes whenever the tx (or any referenced entity) changes,
    // so returning from Edit shows fresh data instead of a one-shot init() snapshot.
    val uiState: StateFlow<TransactionDetailUiState> = combine(
        txRepo.observeById(txId),
        categoryRepo.observeAll(),
        groupRepo.observeAll(),
        accountRepo.observeAll(),
        ledgerRepo.observeAll(),
    ) { tx, categories, groups, accountList, ledgers ->
        if (tx == null) {
            return@combine TransactionDetailUiState(loading = false, found = false)
        }

        val ledger = ledgers.firstOrNull()
        val currency = if (ledger != null) Currency.of(ledger.currency) else Currency.VND

        val accounts = accountList.associateBy { it.id }
        val account = accounts[tx.accountId]
        val toAccount = tx.toAccountId?.let { accounts[it] }

        val zone = ZoneId.systemDefault()
        val dateTime = Instant.ofEpochMilli(tx.date).atZone(zone)
        val dateLabel = dateTime.format(DATE_FMT)
        val timeLabel = dateTime.format(TIME_FMT)

        if (tx.type == TxType.TRANSFER) {
            return@combine TransactionDetailUiState(
                loading = false,
                found = true,
                icon = "🔁",
                title = account?.name ?: "Chuyển khoản",
                ledgerName = ledger?.name ?: "",
                dateLabel = dateLabel,
                timeLabel = timeLabel,
                accountName = account?.name ?: "—",
                toAccountName = toAccount?.name,
                categoryLabel = "",
                amountMinor = tx.amountMinor,
                type = TxType.TRANSFER,
                currency = currency,
                note = tx.note,
                photoUri = tx.photoUri,
            )
        }

        // INCOME / EXPENSE: resolve leaf + its parent group.
        val leaf = tx.categoryId?.let { id -> categories.firstOrNull { it.id == id } }
        val group = leaf?.let { l -> groups.firstOrNull { it.id == l.groupId } }
        val typeText = typeLabel(tx.type)
        val categoryLabel = if (group != null) "${group.name}($typeText)" else ""

        TransactionDetailUiState(
            loading = false,
            found = true,
            icon = leaf?.icon ?: group?.icon ?: "💸",
            title = leaf?.name ?: "Giao dịch",
            ledgerName = ledger?.name ?: "",
            dateLabel = dateLabel,
            timeLabel = timeLabel,
            accountName = account?.name ?: "—",
            toAccountName = null,
            categoryLabel = categoryLabel,
            amountMinor = tx.amountMinor,
            type = tx.type,
            currency = currency,
            note = tx.note,
            photoUri = tx.photoUri,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = TransactionDetailUiState(),
    )

    // One-shot done signal after a delete; consumed by a LaunchedEffect on the screen.
    private val _doneChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val doneEvent = _doneChannel.receiveAsFlow()

    fun delete() {
        if (txId == -1L) return
        viewModelScope.launch {
            val tx = txRepo.getById(txId) ?: return@launch
            txRepo.delete(tx)
            _doneChannel.send(Unit)
        }
    }

    private fun typeLabel(type: TxType): String = when (type) {
        TxType.EXPENSE -> "Chi"
        TxType.INCOME -> "Thu"
        TxType.TRANSFER -> "Chuyển khoản"
    }

    private companion object {
        val DATE_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val TIME_FMT: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    }
}
