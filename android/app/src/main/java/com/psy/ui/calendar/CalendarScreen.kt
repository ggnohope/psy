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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.psy.ui.components.EmptyState
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.components.MonthSelector
import com.psy.ui.components.TransactionRow
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalPsyColors.current

    Scaffold(containerColor = colors.bg) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp),
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // ── Title block ──────────────────────────────────────────────
            EyebrowLabel(text = "Dòng thời gian")
            Text(
                text = "Lịch",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.text,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Month switcher ───────────────────────────────────────────
            MonthSelector(
                month = uiState.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )

            Spacer(modifier = Modifier.height(14.dp))

            // ── Calendar card ────────────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.hair, RoundedCornerShape(16.dp))
                    .padding(12.dp),
            ) {
                // Weekday header: T2 T3 T4 T5 T6 T7 CN
                Row(modifier = Modifier.fillMaxWidth()) {
                    val headers = listOf("T2", "T3", "T4", "T5", "T6", "T7", "CN")
                    headers.forEach { label ->
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = label,
                                fontFamily = PlexMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp,
                                color = colors.text3,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Day grid
                if (!uiState.loading) {
                    uiState.weeks.forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth()) {
                            week.forEach { cell ->
                                if (cell == null) {
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f))
                                } else {
                                    DayCellBox(
                                        cell = cell,
                                        isSelected = cell.date == uiState.selectedDay,
                                        onClick = { viewModel.selectDay(cell.date) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // ── Selected-day divider + transaction list ──────────────────
            val selDay = uiState.selectedDay
            if (selDay != null) {
                Text(
                    text = "Giao dịch · ${selDay.format(DateTimeFormatter.ofPattern("dd/MM"))}",
                    fontFamily = PlexMono,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = colors.text3,
                )
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider(color = colors.hair, thickness = 1.dp)
                Spacer(modifier = Modifier.height(12.dp))

                if (uiState.dayRows.isEmpty()) {
                    EmptyState(
                        iconName = "calendar",
                        title = "Không có giao dịch",
                        caption = "Chọn ngày khác hoặc thêm giao dịch.",
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        uiState.dayRows.forEach { row ->
                            CalendarTxRow(
                                row = row,
                                fractionDigits = uiState.currency.fractionDigits,
                                symbol = uiState.currency.symbol,
                            )
                        }
                    }
                }
            } else {
                EmptyState(
                    iconName = "calendar",
                    title = "Không có giao dịch",
                    caption = "Chọn ngày khác hoặc thêm giao dịch.",
                )
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    val shape = RoundedCornerShape(9.dp)

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .padding(2.dp)
            .clip(shape)
            .background(if (isSelected) colors.blueSoft else Color.Transparent)
            .then(
                if (isSelected) Modifier.border(1.5.dp, colors.blue, shape) else Modifier
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
                fontFamily = SpaceGrotesk,
                fontWeight = if (cell.isToday || isSelected) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                color = if (isSelected) colors.blue else colors.text,
            )
            // Tiny dots: expense (red) / income (green)
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                if (cell.expenseMinor > 0L) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.red),
                    )
                }
                if (cell.incomeMinor > 0L) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(colors.green),
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction row (shared TransactionRow component)
// ---------------------------------------------------------------------------

@Composable
private fun CalendarTxRow(
    row: CalendarTxRow,
    fractionDigits: Int,
    symbol: String,
) {
    val colors = LocalPsyColors.current
    val isIncome = row.type == TxType.INCOME
    val sign = when (row.type) {
        TxType.EXPENSE -> "-"
        TxType.INCOME -> "+"
        TxType.TRANSFER -> ""
    }
    val name = if (row.type == TxType.TRANSFER) {
        "${row.accountName} → ${row.toAccountName ?: "—"}"
    } else {
        row.categoryName
    }
    val meta = if (row.groupName.isNotBlank()) {
        "${row.groupName} · ${row.timeLabel}"
    } else {
        row.timeLabel
    }

    TransactionRow(
        iconName = row.categoryIcon,
        iconTint = colors.blue,
        iconBg = colors.blue.copy(alpha = 0.14f),
        name = name,
        meta = meta,
        amount = "$sign${Money.formatMinor(row.amountMinor, fractionDigits, symbol)}",
        isIncome = isIncome,
        account = if (row.type == TxType.TRANSFER) "" else row.accountName,
        onClick = {},
    )
}
