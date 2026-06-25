package com.psy.ui.detail

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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Trash2
import com.psy.domain.model.TxType
import com.psy.domain.util.Money
import com.psy.ui.components.HeroCard
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk
import kotlinx.coroutines.flow.collectLatest

@Composable
fun TransactionDetailScreen(
    onBack: () -> Unit,
    onEdit: () -> Unit,
    onDeleted: () -> Unit,
    viewModel: TransactionDetailViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDeleteDialog by remember { mutableStateOf(false) }

    // One-shot done event after delete.
    LaunchedEffect(Unit) {
        viewModel.doneEvent.collectLatest { onDeleted() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
            .padding(horizontal = 22.dp),
    ) {
        // ── In-page header ───────────────────────────────────────────────
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Lucide.ArrowLeft,
                    contentDescription = "Quay lại",
                    tint = colors.text,
                    modifier = Modifier.size(22.dp),
                )
            }
            Text(
                text = "Chi tiết",
                fontFamily = SpaceGrotesk,
                fontWeight = FontWeight.SemiBold,
                fontSize = 20.sp,
                color = colors.text,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
            )
            if (state.found) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onEdit),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Pencil,
                        contentDescription = "Sửa",
                        tint = colors.text2,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.redSoft)
                        .clickable(onClick = { showDeleteDialog = true }),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Trash2,
                        contentDescription = "Xoá",
                        tint = colors.red,
                        modifier = Modifier.size(20.dp),
                    )
                }
            }
        }

        when {
            state.loading -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = colors.blue)
                }
            }

            !state.found -> {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Không tìm thấy giao dịch",
                        fontSize = 14.sp,
                        color = colors.text3,
                    )
                }
            }

            else -> {
                val currency = state.currency
                val isIncome = state.type == TxType.INCOME
                val amountTint = if (isIncome) colors.incomeTint else colors.expenseTint
                val sign = when (state.type) {
                    TxType.EXPENSE -> "-"
                    TxType.INCOME -> "+"
                    TxType.TRANSFER -> ""
                }
                // Mono category eyebrow e.g. "Vận tải · Chi". Derived from the existing
                // categoryLabel "Group(Chi)" so no ViewModel change is needed.
                val typeWord = when (state.type) {
                    TxType.EXPENSE -> "Chi"
                    TxType.INCOME -> "Thu"
                    TxType.TRANSFER -> "Chuyển khoản"
                }
                val groupName = state.categoryLabel.substringBefore("(", "").trim()
                val monoLabel = if (groupName.isNotBlank()) "$groupName · $typeWord" else typeWord

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(top = 16.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // ── Hero card ─────────────────────────────────────────
                    HeroCard {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            LucideIcon(
                                name = state.icon,
                                tint = Color.White,
                                size = 26.dp,
                            )
                        }
                        Spacer(modifier = Modifier.height(14.dp))
                        Text(
                            text = state.title,
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = Color.White,
                        )
                        Text(
                            text = monoLabel,
                            fontFamily = PlexMono,
                            fontWeight = FontWeight.Light,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.7f),
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "$sign${Money.formatMinor(state.amountMinor, currency.fractionDigits, currency.symbol)}",
                            fontFamily = SpaceGrotesk,
                            fontWeight = FontWeight.Bold,
                            fontSize = 32.sp,
                            color = amountTint,
                        )
                    }

                    // ── Detail list card ──────────────────────────────────
                    val accountValue = if (state.type == TxType.TRANSFER && state.toAccountName != null) {
                        "${state.accountName} → ${state.toAccountName}"
                    } else {
                        state.accountName
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(colors.surface)
                            .border(1.dp, colors.hair, RoundedCornerShape(14.dp)),
                    ) {
                        DetailRow(label = "Sổ", value = state.ledgerName.ifBlank { "—" })
                        Hairline()
                        DetailRow(label = "Ngày", value = state.dateLabel, mono = true)
                        Hairline()
                        DetailRow(label = "Giờ", value = state.timeLabel, mono = true)
                        Hairline()
                        DetailRow(label = "Tài khoản", value = accountValue)
                        Hairline()
                        DetailRow(
                            label = "Ghi chú",
                            value = state.note.ifBlank { "Không có ghi chú" },
                        )
                    }

                    // ── Photo ─────────────────────────────────────────────
                    if (state.photoUri != null) {
                        AsyncImage(
                            model = state.photoUri,
                            contentDescription = "Ảnh đính kèm",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp)),
                        )
                    }
                }
            }
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            containerColor = colors.surface,
            title = { Text("Xoá giao dịch?", color = colors.text) },
            text = { Text("Hành động này không thể hoàn tác.", color = colors.text3) },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        viewModel.delete()
                    },
                ) {
                    Text("Xoá", color = colors.red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Huỷ", color = colors.text2)
                }
            },
        )
    }
}

@Composable
private fun Hairline() {
    val colors = LocalPsyColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(colors.hair),
    )
}

@Composable
private fun DetailRow(label: String, value: String, mono: Boolean = false) {
    val colors = LocalPsyColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            fontSize = 14.sp,
            color = colors.text3,
            modifier = Modifier.width(96.dp),
        )
        Text(
            text = value,
            fontFamily = if (mono) PlexMono else null,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = colors.text,
            modifier = Modifier.weight(1f),
        )
    }
}
