package com.psy.ui.stats

import androidx.compose.foundation.background
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
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.MonthSelector
import com.psy.ui.components.charts.DonutChart
import com.psy.ui.components.charts.TrendBars
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

            // ── Summary card ──────────────────────────────────────────────
            SummaryCard(state = state)

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

@Composable
private fun TopEntryRow(
    entry: TopEntry,
    currency: com.psy.domain.model.Currency,
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
