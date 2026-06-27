package com.psy.ui.stats

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.model.AccountType
import com.psy.domain.model.Currency
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.EmptyState
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.components.HeroCard
import com.psy.ui.components.IconTile
import com.psy.ui.components.MonthSelector
import com.psy.ui.components.SegmentedControl
import com.psy.ui.components.charts.DonutChart
import com.psy.ui.components.charts.TrendBars
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk

@Composable
fun StatsScreen(viewModel: StatsViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalPsyColors.current

    Scaffold(containerColor = colors.bg) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            // ── Title block ───────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                EyebrowLabel("Phân tích")
                Text(
                    text = "Thống kê",
                    style = androidx.compose.material3.MaterialTheme.typography.headlineMedium,
                    color = colors.text,
                )
            }

            // ── Month selector ────────────────────────────────────────────
            MonthSelector(
                month = state.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )

            // ── Account filter (Tất cả / Tiền mặt / Ngân hàng) ────────────
            // Map fixed segments onto the VM's accountId filter without touching
            // its logic: segment 0 -> all, 1 -> first CASH account, 2 -> first BANK.
            if (state.accounts.isNotEmpty()) {
                val firstCash = state.accounts.firstOrNull { it.type == AccountType.CASH }
                val firstBank = state.accounts.firstOrNull { it.type == AccountType.BANK }
                val selectedType = state.accounts
                    .firstOrNull { it.id == state.selectedAccountId }
                    ?.type
                val selectedIndex = when {
                    state.selectedAccountId == null -> 0
                    selectedType == AccountType.CASH -> 1
                    selectedType == AccountType.BANK -> 2
                    else -> 0
                }
                SegmentedControl(
                    options = listOf("Tất cả", "Tiền mặt", "Ngân hàng"),
                    selectedIndex = selectedIndex,
                    onSelect = { index ->
                        when (index) {
                            1 -> viewModel.selectAccount(firstCash?.id)
                            2 -> viewModel.selectAccount(firstBank?.id)
                            else -> viewModel.selectAccount(null)
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            // ── Summary hero card ─────────────────────────────────────────
            SummaryHeroCard(state = state)

            // ── Per-account breakdown (only in "Tất cả" mode) ─────────────
            if (state.selectedAccountId == null && state.accountBreakdown.isNotEmpty()) {
                AccountBreakdownSection(state = state, onSelect = viewModel::selectAccount)
            }

            // ── Chi / Thu segmented toggle ────────────────────────────────
            SegmentedControl(
                options = listOf("Chi tiêu", "Thu nhập"),
                selectedIndex = if (state.pieMode == TxType.EXPENSE) 0 else 1,
                onSelect = { index ->
                    viewModel.setPieMode(if (index == 0) TxType.EXPENSE else TxType.INCOME)
                },
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Donut chart + legend ──────────────────────────────────────
            val currency = state.currency
            val pieTotal = state.slices.sumOf { it.amountMinor }
            val pieIsEmpty = state.slices.isEmpty()
            val centerTitle = if (state.pieMode == TxType.EXPENSE) "Chi tiêu" else "Thu nhập"
            val centerLabel = if (pieTotal > 0L) {
                Money.formatMinor(pieTotal, currency.fractionDigits, currency.symbol)
            } else {
                "—"
            }

            if (pieIsEmpty) {
                EmptyState(
                    iconName = "chart-column",
                    title = "Chưa có dữ liệu",
                    caption = "Thêm giao dịch để xem thống kê.",
                )
            } else {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    DonutChart(
                        slices = state.slices,
                        centerLabel = centerLabel,
                        centerTitle = centerTitle,
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
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
                                        .size(10.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(Color(slice.color)),
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = slice.label,
                                    fontSize = 14.sp,
                                    color = colors.text,
                                    modifier = Modifier.weight(1f),
                                )
                                Text(
                                    text = "${(slicePercent * 100).toInt()}%",
                                    fontFamily = PlexMono,
                                    fontSize = 12.sp,
                                    color = colors.text3,
                                )
                            }
                        }
                    }
                }
            }

            // ── Top chi tiêu / Top thu nhập ───────────────────────────────
            if (state.top.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EyebrowLabel(if (state.pieMode == TxType.EXPENSE) "Top chi tiêu" else "Top thu nhập")
                    val expanded = remember { mutableStateMapOf<Long, Boolean>() }
                    state.top.forEach { group ->
                        TopGroupRow(
                            group = group,
                            currency = state.currency,
                            expanded = expanded[group.groupId] == true,
                            onToggle = {
                                expanded[group.groupId] = expanded[group.groupId] != true
                            },
                        )
                    }
                }
            }

            // ── Trend 6 months ────────────────────────────────────────────
            if (state.trend.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    EyebrowLabel("Xu hướng 6 tháng")
                    TrendBars(months = state.trend)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

// ---------------------------------------------------------------------------
// Sub-composables
// ---------------------------------------------------------------------------

@Composable
private fun SummaryHeroCard(state: StatsUiState) {
    val colors = LocalPsyColors.current
    val currency = state.currency

    fun money(minor: Long) =
        Money.formatMinor(minor, currency.fractionDigits, currency.symbol)

    HeroCard {
        Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroStat("Thu", money(state.summary.incomeMinor), colors.incomeTint, Modifier.weight(1f))
                HeroStat("Chi", money(state.summary.expenseMinor), colors.expenseTint, Modifier.weight(1f))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HeroStat("Chênh lệch", money(state.summary.netMinor), Color.White, Modifier.weight(1f))
                HeroStat("TB ngày", money(state.summary.avgPerDayMinor), Color.White, Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun HeroStat(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label.uppercase(),
            fontFamily = PlexMono,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 1.6.sp,
            color = Color.White.copy(alpha = 0.7f),
        )
        Text(
            text = value,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 22.sp,
            color = valueColor,
        )
    }
}

// ── Per-account breakdown ───────────────────────────────────────────────────

@Composable
private fun AccountBreakdownSection(
    state: StatsUiState,
    onSelect: (Long?) -> Unit,
) {
    val colors = LocalPsyColors.current
    val maxValue = state.accountBreakdown
        .maxOf { maxOf(it.incomeMinor, it.expenseMinor) }
        .coerceAtLeast(1L)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EyebrowLabel("Theo tài khoản")
            Row(verticalAlignment = Alignment.CenterVertically) {
                LegendSwatch(colors.green)
                Spacer(Modifier.width(4.dp))
                Text("Thu", fontFamily = PlexMono, fontSize = 11.sp, color = colors.text3)
                Spacer(Modifier.width(12.dp))
                LegendSwatch(colors.red)
                Spacer(Modifier.width(4.dp))
                Text("Chi", fontFamily = PlexMono, fontSize = 11.sp, color = colors.text3)
            }
        }
        state.accountBreakdown.forEachIndexed { index, stat ->
            AccountBreakdownRow(
                stat = stat,
                maxValue = maxValue,
                currency = state.currency,
                iconTint = if (index % 2 == 0) colors.blue else colors.green,
                onClick = { onSelect(stat.id) },
            )
        }
    }
}

@Composable
private fun AccountBreakdownRow(
    stat: AccountStat,
    maxValue: Long,
    currency: Currency,
    iconTint: Color,
    onClick: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
    ) {
        IconTile(
            iconName = stat.icon,
            tint = iconTint,
            bg = iconTint.copy(alpha = 0.14f),
            size = 42.dp,
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stat.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = colors.text,
                    maxLines = 1,
                    modifier = Modifier.weight(1f),
                )
                if (stat.isFund) {
                    Text(
                        "Quỹ",
                        color = colors.blue,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(percent = 50))
                            .background(colors.blueSoft)
                            .padding(horizontal = 7.dp, vertical = 2.dp),
                    )
                }
                Text(
                    text = Money.formatMinor(stat.netMinor, currency.fractionDigits, currency.symbol),
                    fontFamily = SpaceGrotesk,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = if (stat.netMinor >= 0L) colors.green else colors.red,
                )
            }
            ProgressBar(
                fraction = stat.incomeMinor.toFloat() / maxValue.toFloat(),
                color = colors.green,
                height = 6.dp,
            )
            ProgressBar(
                fraction = stat.expenseMinor.toFloat() / maxValue.toFloat(),
                color = colors.red,
                height = 6.dp,
            )
        }
    }
}

@Composable
private fun ProgressBar(
    fraction: Float,
    color: Color,
    height: androidx.compose.ui.unit.Dp,
) {
    val colors = LocalPsyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(height)
            .clip(RoundedCornerShape(999.dp))
            .background(colors.sunken),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(fraction = fraction.coerceIn(0f, 1f))
                .height(height)
                .clip(RoundedCornerShape(999.dp))
                .background(color),
        )
    }
}

@Composable
private fun LegendSwatch(color: Color) {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(color),
    )
}

// ── Top groups ──────────────────────────────────────────────────────────────

@Composable
private fun TopGroupRow(
    group: TopGroup,
    currency: Currency,
    expanded: Boolean,
    onToggle: () -> Unit,
) {
    val colors = LocalPsyColors.current
    val groupColor = Color(group.color)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .clickable(onClick = onToggle)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            LegendSwatch(groupColor)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = group.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = colors.text,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Money.formatMinor(group.amountMinor, currency.fractionDigits, currency.symbol),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(group.percentOfTotal * 100).toInt()}% · ${group.count}",
                fontFamily = PlexMono,
                fontSize = 11.sp,
                color = colors.text3,
            )
        }

        ProgressBar(
            fraction = group.percentOfTotal,
            color = groupColor,
            height = 8.dp,
        )

        if (expanded) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, top = 2.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                group.children.forEach { leaf ->
                    TopLeafRow(
                        leaf = leaf,
                        currency = currency,
                        barColor = groupColor.copy(alpha = 0.45f),
                    )
                }
            }
        }
    }
}

@Composable
private fun TopLeafRow(
    leaf: TopLeaf,
    currency: Currency,
    barColor: Color,
) {
    val colors = LocalPsyColors.current
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = leaf.name,
                fontSize = 13.sp,
                color = colors.text2,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = Money.formatMinor(leaf.amountMinor, currency.fractionDigits, currency.symbol),
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.text,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "${(leaf.percentInGroup * 100).toInt()}%",
                fontFamily = PlexMono,
                fontSize = 11.sp,
                color = colors.text3,
            )
        }
        ProgressBar(
            fraction = leaf.percentInGroup,
            color = barColor,
            height = 6.dp,
        )
    }
}
