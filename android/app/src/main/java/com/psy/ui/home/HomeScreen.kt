package com.psy.ui.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowDownRight
import com.composables.icons.lucide.ArrowUpRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Settings
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.EmptyState
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.components.HeroCard
import com.psy.ui.components.TransactionRow
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onTxClick: (Long) -> Unit,
    onSettingsClick: () -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 22.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Header: eyebrow + wordmark + settings button
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        EyebrowLabel(text = "Tổng quan")
                        Text(
                            text = "Psy",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 28.sp,
                            color = colors.text,
                        )
                    }
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.hair, RoundedCornerShape(12.dp))
                            .clickable(onClick = onSettingsClick),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Lucide.Settings,
                            contentDescription = "Cài đặt",
                            tint = colors.text2,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // Balance hero card
            item {
                BalanceHero(
                    monthLabel = uiState.monthLabel,
                    netMinor = uiState.netMinor,
                    incomeMinor = uiState.incomeMinor,
                    expenseMinor = uiState.expenseMinor,
                    currencyFractionDigits = uiState.currency.fractionDigits,
                    currencySymbol = uiState.currency.symbol,
                )
            }

            if (uiState.days.isEmpty() && !uiState.loading) {
                item {
                    EmptyState(
                        iconName = "receipt",
                        title = "Chưa có giao dịch",
                        caption = "Thêm giao dịch đầu tiên của bạn hôm nay.",
                    )
                }
            } else {
                uiState.days.forEach { dayGroup ->
                    item {
                        EyebrowLabel(
                            text = dayGroup.dateLabel,
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                    items(dayGroup.items, key = { it.id }) { row ->
                        val isIncome = row.type == TxType.INCOME
                        val sign = when (row.type) {
                            TxType.INCOME -> "+"
                            TxType.EXPENSE -> "-"
                            TxType.TRANSFER -> ""
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
                            name = row.categoryName,
                            meta = meta,
                            amount = "$sign${
                                Money.formatMinor(
                                    row.amountMinor,
                                    uiState.currency.fractionDigits,
                                    uiState.currency.symbol,
                                )
                            }",
                            isIncome = isIncome,
                            account = row.accountName,
                            isFund = row.isFund,
                            onClick = { onTxClick(row.id) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(96.dp)) }
        }

        // Floating action button
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 22.dp, bottom = 24.dp)
                .size(60.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(colors.blue)
                .clickable(onClick = onAddClick),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = Lucide.Plus,
                contentDescription = "Thêm giao dịch",
                tint = Color.White,
                modifier = Modifier.size(26.dp),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Balance hero card
// ---------------------------------------------------------------------------

@Composable
private fun BalanceHero(
    monthLabel: String,
    netMinor: Long,
    incomeMinor: Long,
    expenseMinor: Long,
    currencyFractionDigits: Int,
    currencySymbol: String,
) {
    val colors = LocalPsyColors.current
    val monoLabelColor = Color(0xFFAEC4DA)
    val teal = Color(0xFF19E3E0)

    HeroCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Số dư · $monthLabel",
                fontFamily = PlexMono,
                fontSize = 12.sp,
                color = monoLabelColor,
                modifier = Modifier.weight(1f),
            )
            // LIVE pill
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(Color.White.copy(alpha = 0.08f))
                    .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(999.dp))
                    .padding(horizontal = 9.dp, vertical = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(teal),
                )
                Text(
                    text = "LIVE",
                    fontFamily = PlexMono,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 1.4.sp,
                    color = teal,
                )
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = Money.formatMinor(netMinor, currencyFractionDigits, currencySymbol),
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.Bold,
            fontSize = 40.sp,
            color = Color.White,
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Thu nhập",
                amount = Money.formatMinor(incomeMinor, currencyFractionDigits, currencySymbol),
                tint = colors.incomeTint,
                income = true,
            )
            StatTile(
                modifier = Modifier.weight(1f),
                label = "Chi tiêu",
                amount = Money.formatMinor(expenseMinor, currencyFractionDigits, currencySymbol),
                tint = colors.expenseTint,
                income = false,
            )
        }
    }
}

@Composable
private fun StatTile(
    label: String,
    amount: String,
    tint: Color,
    income: Boolean,
    modifier: Modifier = Modifier,
) {
    val monoLabelColor = Color(0xFFAEC4DA)
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.08f))
            .border(1.dp, Color.White.copy(alpha = 0.12f), RoundedCornerShape(12.dp))
            .padding(horizontal = 12.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Icon(
                imageVector = if (income) Lucide.ArrowUpRight else Lucide.ArrowDownRight,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(16.dp),
            )
            Text(
                text = label,
                fontFamily = PlexMono,
                fontSize = 11.sp,
                color = monoLabelColor,
            )
        }
        Text(
            text = amount,
            fontFamily = SpaceGrotesk,
            fontWeight = FontWeight.SemiBold,
            fontSize = 16.sp,
            color = tint,
        )
    }
}
