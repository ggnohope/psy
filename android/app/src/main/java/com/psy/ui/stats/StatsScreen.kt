package com.psy.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.model.Account
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.MonthSelector
import com.psy.ui.components.charts.DonutChart
import com.psy.ui.components.charts.TrendBars
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep
import com.psy.ui.theme.CandySky
import com.psy.ui.theme.CandyViolet

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Thống kê") })
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            // ── Month selector ────────────────────────────────────────────
            MonthSelector(
                month = state.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )

            // ── Account filter chips ──────────────────────────────────────
            if (state.accounts.isNotEmpty()) {
                AccountChipRow(
                    accounts = state.accounts,
                    selectedAccountId = state.selectedAccountId,
                    onSelect = viewModel::selectAccount,
                )
            }

            // ── Summary card ──────────────────────────────────────────────
            SummaryCard(state = state)

            // ── Per-account comparison (only in "Tất cả" mode) ────────────
            if (state.selectedAccountId == null) {
                AccountBreakdownCard(
                    breakdown = state.accountBreakdown,
                    currency = state.currency,
                    onSelect = viewModel::selectAccount,
                )
            }

            // ── Chi / Thu segmented toggle ────────────────────────────────
            PieModeToggle(
                pieMode = state.pieMode,
                onSelect = viewModel::setPieMode,
            )

            // ── Donut chart + legend ──────────────────────────────────────
            val currency = state.currency
            val pieTotal = state.slices.sumOf { it.amountMinor }
            val centerLabel = if (pieTotal > 0L) {
                Money.formatMinor(pieTotal, currency.fractionDigits, currency.symbol)
            } else {
                "—"
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                DonutChart(
                    slices = state.slices,
                    centerLabel = centerLabel,
                    modifier = Modifier.align(Alignment.CenterHorizontally),
                )

                Spacer(modifier = Modifier.height(8.dp))

                if (state.slices.isEmpty()) {
                    Text(
                        text = "Không có dữ liệu cho tháng này",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    // Legend
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.slices.forEach { slice ->
                            val slicePercent = if (pieTotal > 0L) {
                                slice.amountMinor.toFloat() / pieTotal.toFloat()
                            } else 0f
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(Color(slice.color)),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = slice.label,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${(slicePercent * 100).toInt()}%",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                }
            }

            // ── Top chi tiêu / Top thu nhập ───────────────────────────────
            val topTitle = if (state.pieMode == TxType.EXPENSE) "Top chi tiêu" else "Top thu nhập"
            Text(
                text = topTitle,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.top.isEmpty()) {
                Text(
                    text = "Không có danh mục nào trong tháng này",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    state.top.forEach { entry ->
                        TopEntryRow(
                            entry = entry,
                            currency = state.currency,
                        )
                    }
                }
            }

            // ── Trend 6 months ────────────────────────────────────────────
            Text(
                text = "Xu hướng 6 tháng",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.trend.isEmpty()) {
                Text(
                    text = "Không có dữ liệu xu hướng",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                TrendBars(months = state.trend)
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun SummaryCard(state: StatsUiState) {
    val currency = state.currency
    val gradientBrush = Brush.horizontalGradient(listOf(CandyViolet, CandySky))

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(gradientBrush)
            .padding(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryItem(
                    label = "Thu",
                    amount = Money.formatMinor(
                        state.summary.incomeMinor,
                        currency.fractionDigits,
                        currency.symbol,
                    ),
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    label = "Chi",
                    amount = Money.formatMinor(
                        state.summary.expenseMinor,
                        currency.fractionDigits,
                        currency.symbol,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                SummaryItem(
                    label = "Chênh lệch",
                    amount = Money.formatMinor(
                        state.summary.netMinor,
                        currency.fractionDigits,
                        currency.symbol,
                    ),
                    modifier = Modifier.weight(1f),
                )
                SummaryItem(
                    label = "TB ngày",
                    amount = Money.formatMinor(
                        state.summary.avgPerDayMinor,
                        currency.fractionDigits,
                        currency.symbol,
                    ),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun SummaryItem(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.8f),
        )
        Text(
            text = amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = Color.White,
        )
    }
}

@Composable
private fun PieModeToggle(
    pieMode: TxType,
    onSelect: (TxType) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        PieModeToggleItem(
            label = "Chi tiêu",
            selected = pieMode == TxType.EXPENSE,
            onClick = { onSelect(TxType.EXPENSE) },
            modifier = Modifier.weight(1f),
        )
        PieModeToggleItem(
            label = "Thu nhập",
            selected = pieMode == TxType.INCOME,
            onClick = { onSelect(TxType.INCOME) },
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun PieModeToggleItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val bgColor = if (selected) {
        MaterialTheme.colorScheme.primary
    } else {
        Color.Transparent
    }
    val textColor = if (selected) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor),
    ) {
        TextButton(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
            Text(
                text = label,
                color = textColor,
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// ── Account filter chips ───────────────────────────────────────────────────

@Composable
private fun AccountChipRow(
    accounts: List<Account>,
    selectedAccountId: Long?,
    onSelect: (Long?) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        AccountChip(
            label = "Tất cả",
            selected = selectedAccountId == null,
            onClick = { onSelect(null) },
        )
        accounts.forEach { account ->
            AccountChip(
                label = "${account.icon} ${account.name}",
                selected = selectedAccountId == account.id,
                onClick = { onSelect(account.id) },
            )
        }
    }
}

@Composable
private fun AccountChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
    val textColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(bgColor)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    ) {
        Text(
            text = label,
            color = textColor,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
        )
    }
}

// ── Per-account comparison card ─────────────────────────────────────────────

@Composable
private fun AccountBreakdownCard(
    breakdown: List<AccountStat>,
    currency: Currency,
    onSelect: (Long?) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "💜 Theo tài khoản",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            // Legend: thu / chi
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendDot(CandyGreen); Spacer(Modifier.width(4.dp))
                Text("Thu", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.width(10.dp))
                LegendDot(CandyPinkDeep); Spacer(Modifier.width(4.dp))
                Text("Chi", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (breakdown.isEmpty()) {
            Text(
                text = "Kỳ này chưa có giao dịch theo tài khoản",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            // Scale all bars against the largest single income/expense across accounts.
            val maxValue = breakdown.maxOf { maxOf(it.incomeMinor, it.expenseMinor) }.coerceAtLeast(1L)
            breakdown.forEach { stat ->
                AccountBreakdownRow(
                    stat = stat,
                    maxValue = maxValue,
                    currency = currency,
                    onClick = { onSelect(stat.id) },
                )
            }
        }
    }
}

@Composable
private fun AccountBreakdownRow(
    stat: AccountStat,
    maxValue: Long,
    currency: Currency,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(stat.color).copy(alpha = 0.18f)),
        ) {
            Text(text = stat.icon, style = MaterialTheme.typography.bodyMedium)
        }
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stat.name,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = Money.formatMinor(stat.netMinor, currency.fractionDigits, currency.symbol),
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    color = if (stat.netMinor >= 0L) CandyGreen else CandyPinkDeep,
                )
            }
            BarLine(fraction = stat.incomeMinor.toFloat() / maxValue.toFloat(), color = CandyGreen)
            BarLine(fraction = stat.expenseMinor.toFloat() / maxValue.toFloat(), color = CandyPinkDeep)
        }
    }
}

@Composable
private fun BarLine(fraction: Float, color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

@Composable
private fun LegendDot(color: Color) {
    Box(
        modifier = Modifier
            .size(8.dp)
            .clip(CircleShape)
            .background(color),
    )
}

@Composable
private fun TopEntryRow(
    entry: TopEntry,
    currency: Currency,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Category icon + name
            Text(
                text = "${entry.category.icon} ${entry.category.name}",
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.weight(1f),
            )
            // Amount + percent
            Text(
                text = Money.formatMinor(
                    entry.amountMinor,
                    currency.fractionDigits,
                    currency.symbol,
                ),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = "(${(entry.percent * 100).toInt()}%)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        // Proportion bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(50))
                .background(MaterialTheme.colorScheme.surfaceVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = entry.percent.coerceIn(0f, 1f))
                    .height(8.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(entry.category.color)),
            )
        }
    }
}
