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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
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

    private val _uiState = MutableStateFlow(TransactionDetailUiState())
    val uiState: StateFlow<TransactionDetailUiState> = _uiState.asStateFlow()

    // One-shot done signal after a delete; consumed by a LaunchedEffect on the screen.
    private val _doneChannel = Channel<Unit>(capacity = Channel.BUFFERED)
    val doneEvent = _doneChannel.receiveAsFlow()

    init {
        viewModelScope.launch {
            val tx = if (txId != -1L) txRepo.getById(txId) else null
            if (tx == null) {
                _uiState.update { it.copy(loading = false, found = false) }
                return@launch
            }

            val ledger = ledgerRepo.firstOrNull()
            val currency = if (ledger != null) Currency.of(ledger.currency) else Currency.VND

            val accounts = accountRepo.observeAll().first().associateBy { it.id }
            val account = accounts[tx.accountId]
            val toAccount = tx.toAccountId?.let { accounts[it] }

            val zone = ZoneId.systemDefault()
            val dateTime = Instant.ofEpochMilli(tx.date).atZone(zone)
            val dateLabel = dateTime.format(DATE_FMT)
            val timeLabel = dateTime.format(TIME_FMT)

            if (tx.type == TxType.TRANSFER) {
                _uiState.update {
                    TransactionDetailUiState(
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
                return@launch
            }

            // INCOME / EXPENSE: resolve leaf + its parent group.
            val leaf = tx.categoryId?.let { id ->
                categoryRepo.observeAll().first().firstOrNull { it.id == id }
            }
            val group = leaf?.let { l ->
                groupRepo.observeAll().first().firstOrNull { it.id == l.groupId }
            }
            val typeText = typeLabel(tx.type)
            val categoryLabel = if (group != null) "${group.name}($typeText)" else ""

            _uiState.update {
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
            }
        }
    }

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
