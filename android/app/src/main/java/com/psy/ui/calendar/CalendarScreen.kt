package com.psy.ui.calendar

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.MonthSelector
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep
import com.psy.ui.theme.CandyViolet
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Lịch") })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            // Month selector
            MonthSelector(
                month = uiState.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
                modifier = Modifier.padding(vertical = 4.dp),
            )

            // Weekday header: T2 T3 T4 T5 T6 T7 CN
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
            ) {
                val headers = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                headers.forEach { label ->
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                            textAlign = TextAlign.Center,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Calendar grid
            if (!uiState.loading) {
                Column(modifier = Modifier.padding(horizontal = 4.dp)) {
                    uiState.weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                if (cell == null) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    DayCellBox(
                                        cell = cell,
                                        isSelected = cell.date == uiState.selectedDay,
                                        fractionDigits = uiState.currency.fractionDigits,
                                        symbol = uiState.currency.symbol,
                                        onClick = { viewModel.selectDay(cell.date) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(top = 8.dp))

            // Selected day header + transaction list
            val selDay = uiState.selectedDay
            if (selDay != null) {
                Text(
                    text = "Giao dịch ngày ${selDay.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                )
                if (uiState.dayRows.isEmpty()) {
                    EmptyDayState(message = "Không có giao dịch")
                } else {
                    uiState.dayRows.forEach { row ->
                        CalendarTxRowCard(
                            row = row,
                            fractionDigits = uiState.currency.fractionDigits,
                            symbol = uiState.currency.symbol,
                        )
                    }
                }
            } else {
                EmptyDayState(message = "Chưa chọn ngày")
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Day cell
// ---------------------------------------------------------------------------

@Composable
private fun DayCellBox(
    cell: DayCell,
    isSelected: Boolean,
    fractionDigits: Int,
    symbol: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val todayBg = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val selectedBg = CandyViolet.copy(alpha = 0.18f)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(RoundedCornerShape(8.dp))
            .then(
                if (isSelected) Modifier.border(2.dp, CandyViolet, RoundedCornerShape(8.dp))
                else Modifier
            )
            .background(
                when {
                    isSelected -> selectedBg
                    cell.isToday -> todayBg
                    else -> Color.Transparent
                }
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = cell.date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (cell.isToday) FontWeight.Bold else FontWeight.Normal,
                color = if (cell.isToday) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface,
            )
            // Show tiny colored dots for income/expense
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                if (cell.expenseMinor > 0L) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(CandyPinkDeep),
                    )
                }
                if (cell.incomeMinor > 0L) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(CandyGreen),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction row card (calendar variant — no click handler)
// ---------------------------------------------------------------------------

@Composable
private fun CalendarTxRowCard(
    row: CalendarTxRow,
    fractionDigits: Int,
    symbol: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Emoji icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (row.type) {
                            TxType.EXPENSE -> CandyPinkDeep.copy(alpha = 0.15f)
                            TxType.INCOME -> CandyGreen.copy(alpha = 0.15f)
                            TxType.TRANSFER -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                        }
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(text = row.categoryIcon, fontSize = 22.sp)
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Category/transfer label + note + account
            Column(modifier = Modifier.weight(1f)) {
                if (row.type == TxType.TRANSFER) {
                    Text(
                        text = "${row.accountName} → ${row.toAccountName ?: "—"}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                } else {
                    Text(
                        text = row.categoryName,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (row.note.isNotBlank()) {
                    Text(
                        text = row.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                if (row.type != TxType.TRANSFER) {
                    Text(
                        text = row.accountName,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing amount
            val (sign, amountColor) = when (row.type) {
                TxType.EXPENSE -> "-" to CandyPinkDeep
                TxType.INCOME -> "+" to CandyGreen
                TxType.TRANSFER -> "" to MaterialTheme.colorScheme.onSurface
            }
            Text(
                text = "$sign${Money.formatMinor(row.amountMinor, fractionDigits, symbol)}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Bold,
                color = amountColor,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Empty state
// ---------------------------------------------------------------------------

@Composable
private fun EmptyDayState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
        )
    }
}
