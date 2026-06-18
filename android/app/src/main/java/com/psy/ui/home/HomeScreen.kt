package com.psy.ui.home

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.theme.CandyGreen
import com.psy.ui.theme.CandyPinkDeep
import com.psy.ui.theme.CandySky
import com.psy.ui.theme.CandyViolet

@Composable
fun HomeScreen(
    onAddClick: () -> Unit,
    onTxClick: (Long) -> Unit,
    viewModel: HomeViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = onAddClick,
                containerColor = MaterialTheme.colorScheme.primary,
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Thêm giao dịch",
                    tint = Color.White,
                )
            }
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Balance card
            item {
                BalanceCard(
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
                    EmptyState()
                }
            } else {
                uiState.days.forEach { dayGroup ->
                    item {
                        Text(
                            text = dayGroup.dateLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        )
                    }
                    items(dayGroup.items, key = { it.id }) { row ->
                        TxRowCard(
                            row = row,
                            fractionDigits = uiState.currency.fractionDigits,
                            symbol = uiState.currency.symbol,
                            onClick = { onTxClick(row.id) },
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }
}

// ---------------------------------------------------------------------------
// Balance card
// ---------------------------------------------------------------------------

@Composable
private fun BalanceCard(
    monthLabel: String,
    netMinor: Long,
    incomeMinor: Long,
    expenseMinor: Long,
    currencyFractionDigits: Int,
    currencySymbol: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(
                Brush.linearGradient(
                    colors = listOf(CandyViolet, CandySky),
                ),
            )
            .padding(20.dp),
    ) {
        Column {
            Text(
                text = monthLabel,
                color = Color.White.copy(alpha = 0.85f),
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium,
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = Money.formatMinor(netMinor, currencyFractionDigits, currencySymbol),
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
            ) {
                Column {
                    Text(
                        text = "Thu nhập",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "+${Money.formatMinor(incomeMinor, currencyFractionDigits, currencySymbol)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Column {
                    Text(
                        text = "Chi tiêu",
                        color = Color.White.copy(alpha = 0.75f),
                        fontSize = 12.sp,
                    )
                    Text(
                        text = "-${Money.formatMinor(expenseMinor, currencyFractionDigits, currencySymbol)}",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        }
    }
}

// ---------------------------------------------------------------------------
// Transaction row card
// ---------------------------------------------------------------------------

@Composable
private fun TxRowCard(
    row: TxRow,
    fractionDigits: Int,
    symbol: String,
    onClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clickable(onClick = onClick),
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
            // Tinted emoji icon circle
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(
                        when (row.type) {
                            TxType.EXPENSE -> CandyPinkDeep.copy(alpha = 0.15f)
                            TxType.INCOME -> CandyGreen.copy(alpha = 0.15f)
                        },
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = row.categoryIcon,
                    fontSize = 22.sp,
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Category + note + account
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = row.categoryName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                if (row.note.isNotBlank()) {
                    Text(
                        text = row.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                    )
                }
                Text(
                    text = row.accountName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                )
            }

            // Trailing amount with sign + color
            val (sign, amountColor) = when (row.type) {
                TxType.EXPENSE -> "-" to CandyPinkDeep
                TxType.INCOME -> "+" to CandyGreen
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
private fun EmptyState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 64.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(text = "🌸", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Chưa có giao dịch nào tháng này",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Nhấn + để thêm giao dịch đầu tiên!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f),
        )
    }
}
