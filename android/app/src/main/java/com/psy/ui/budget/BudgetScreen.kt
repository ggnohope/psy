package com.psy.ui.budget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.TriangleAlert
import com.psy.domain.util.Money
import com.psy.ui.components.BudgetProgress
import com.psy.ui.components.EmptyState
import com.psy.ui.components.EyebrowLabel
import com.psy.ui.components.clearFocusOnTap
import com.psy.ui.components.IconTile
import com.psy.ui.components.MonthSelector
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val colors = LocalPsyColors.current
    val fmt = { minor: Long -> Money.formatMinor(minor, state.currency.fractionDigits, state.currency.symbol) }

    Scaffold { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            // Title block
            EyebrowLabel("Hạn mức")
            Text(
                text = "Ngân sách",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.text,
            )

            // Month switcher
            MonthSelector(
                month = state.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )

            // ----------------------------------------------------------------
            // Total budget card
            // ----------------------------------------------------------------
            if (state.total != null) {
                val total = state.total!!
                val over = total.spentMinor > total.limitMinor
                val pct = (total.percent * 100).toInt()
                val redAccent = colors.red

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(colors.surface)
                        .border(BorderStroke(1.dp, colors.hair), RoundedCornerShape(16.dp))
                        // 3px left accent border when over budget
                        .then(
                            if (over) {
                                Modifier.drawBehind {
                                    drawRect(
                                        color = redAccent,
                                        size = androidx.compose.ui.geometry.Size(3.dp.toPx(), size.height),
                                    )
                                }
                            } else {
                                Modifier
                            },
                        )
                        .clickable { viewModel.startEdit(total.budget) },
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = "Ngân sách tổng",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.text,
                            )
                            PercentPill(percent = pct, over = over)
                        }
                        BudgetProgress(
                            spentMinor = total.spentMinor,
                            limitMinor = total.limitMinor,
                            height = 10.dp,
                        )
                        Text(
                            text = "Đã chi ${fmt(total.spentMinor)} / ${fmt(total.limitMinor)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text3,
                        )
                        if (over) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(
                                    imageVector = Lucide.TriangleAlert,
                                    contentDescription = null,
                                    tint = colors.red,
                                    modifier = Modifier.size(16.dp),
                                )
                                Text(
                                    text = "Vượt ${fmt(total.spentMinor - total.limitMinor)}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colors.red,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                        }
                    }
                }
            }

            // ----------------------------------------------------------------
            // Per-category budgets
            // ----------------------------------------------------------------
            if (state.categoryBudgets.isEmpty() && state.total == null) {
                EmptyState(
                    iconName = "wallet",
                    title = "Chưa có ngân sách",
                    caption = "Đặt hạn mức cho nhóm chi tiêu.",
                )
            }

            state.categoryBudgets.forEach { item ->
                val catColor = item.group?.color?.let { Color(it) } ?: colors.blue
                val over = item.spentMinor > item.limitMinor
                val barColor = if (over) colors.red else catColor
                val pct = (item.percent * 100).toInt()

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.surface)
                        .border(BorderStroke(1.dp, colors.hair), RoundedCornerShape(14.dp))
                        .clickable { viewModel.startEdit(item.budget) },
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            IconTile(
                                iconName = item.group?.icon ?: "package",
                                tint = catColor,
                                bg = catColor.copy(alpha = 0.14f),
                                size = 36.dp,
                            )
                            Text(
                                text = item.group?.name ?: "Nhóm",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.text,
                                modifier = Modifier.weight(1f),
                            )
                            Text(
                                text = "$pct%",
                                fontFamily = PlexMono,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = barColor,
                            )
                        }
                        BudgetProgress(
                            spentMinor = item.spentMinor,
                            limitMinor = item.limitMinor,
                            height = 8.dp,
                            fillColor = catColor,
                        )
                        Text(
                            text = "Đã chi ${fmt(item.spentMinor)} / ${fmt(item.limitMinor)}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.text3,
                        )
                    }
                }
            }

            // Add group budget button — dashed-style blue border on blueSoft bg
            if (state.availableGroups.isNotEmpty() || state.categoryBudgets.isNotEmpty()) {
                val enabled = state.availableGroups.isNotEmpty()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.blueSoft)
                        .border(BorderStroke(1.5.dp, colors.blue), RoundedCornerShape(14.dp))
                        .let { if (enabled) it.clickable { viewModel.startAddCategory() } else it }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = null,
                        tint = colors.blue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Thêm ngân sách nhóm",
                        color = colors.blue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            // Allow setting the total budget when none exists yet.
            if (state.total == null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.blueSoft)
                        .border(BorderStroke(1.5.dp, colors.blue), RoundedCornerShape(14.dp))
                        .clickable { viewModel.startAddTotal() }
                        .padding(vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = Lucide.Plus,
                        contentDescription = null,
                        tint = colors.blue,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Đặt ngân sách tổng",
                        color = colors.blue,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 15.sp,
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // ----------------------------------------------------------------
        // Editor ModalBottomSheet
        // ----------------------------------------------------------------
        if (state.editorOpen) {
            val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

            ModalBottomSheet(
                onDismissRequest = viewModel::closeEditor,
                sheetState = sheetState,
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clearFocusOnTap()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 22.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = if (state.editorMode == EditorMode.TOTAL) "Ngân sách tổng" else "Ngân sách nhóm",
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.text,
                    )

                    if (state.editorMode == EditorMode.CATEGORY && !state.isEditing) {
                        EyebrowLabel("Chọn nhóm")
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.availableGroups.forEach { group ->
                                FilterChip(
                                    selected = state.editorGroupId == group.id,
                                    onClick = { viewModel.selectEditorGroup(group.id) },
                                    label = { Text(group.name) },
                                    leadingIcon = {
                                        IconTile(
                                            iconName = group.icon,
                                            tint = Color(group.color),
                                            bg = Color(group.color).copy(alpha = 0.14f),
                                            size = 22.dp,
                                        )
                                    },
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = state.draftAmountText,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Số tiền (${state.currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    Button(
                        onClick = viewModel::saveEditor,
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Lưu")
                    }

                    if (state.isEditing) {
                        TextButton(
                            onClick = viewModel::removeEditor,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = colors.red),
                        ) {
                            Text("Xoá ngân sách này")
                        }
                    }

                    TextButton(
                        onClick = viewModel::closeEditor,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Huỷ")
                    }
                }
            }
        }
    }
}

@Composable
private fun PercentPill(percent: Int, over: Boolean) {
    val colors = LocalPsyColors.current
    val fg = if (over) colors.red else colors.blue
    val bg = if (over) colors.redSoft else colors.blueSoft
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(bg)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = "$percent%",
            fontFamily = PlexMono,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = fg,
        )
    }
}
