package com.psy.ui.budget

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.util.Money
import com.psy.ui.components.BudgetProgress
import com.psy.ui.components.MonthSelector
import com.psy.ui.theme.CandyPinkDeep

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BudgetScreen(viewModel: BudgetViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Ngân sách") })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Month selector
            MonthSelector(
                month = state.monthLabel,
                onPrev = viewModel::prevMonth,
                onNext = viewModel::nextMonth,
            )

            // ----------------------------------------------------------------
            // Total budget area
            // ----------------------------------------------------------------
            if (state.total != null) {
                val total = state.total!!
                val fmt = { minor: Long -> Money.formatMinor(minor, state.currency.fractionDigits, state.currency.symbol) }

                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.startEdit(total.budget) },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Ngân sách tổng",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        BudgetProgress(
                            spentMinor = total.spentMinor,
                            limitMinor = total.limitMinor,
                        )
                        val pct = (total.percent * 100).toInt()
                        Text(
                            text = "Đã chi ${fmt(total.spentMinor)} / ${fmt(total.limitMinor)} ($pct%)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        val remaining = total.limitMinor - total.spentMinor
                        if (total.spentMinor > total.limitMinor) {
                            Text(
                                text = "⚠️ Vượt ${fmt(total.spentMinor - total.limitMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CandyPinkDeep,
                                fontWeight = FontWeight.SemiBold,
                            )
                        } else {
                            Text(
                                text = "Còn lại ${fmt(remaining)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                        }
                    }
                }
            } else {
                OutlinedButton(
                    onClick = viewModel::startAddTotal,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("＋ Đặt ngân sách tổng")
                }
            }

            // ----------------------------------------------------------------
            // Per-category budgets
            // ----------------------------------------------------------------
            Text(
                text = "Ngân sách theo danh mục",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )

            if (state.categoryBudgets.isEmpty() && state.total == null) {
                // Empty state
                Text(
                    text = "Chưa có ngân sách nào. Hãy thêm ngân sách để theo dõi chi tiêu!",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.categoryBudgets.forEach { item ->
                val fmt = { minor: Long -> Money.formatMinor(minor, state.currency.fractionDigits, state.currency.symbol) }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { viewModel.startEdit(item.budget) },
                ) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            Text(
                                text = item.group?.icon ?: "📦",
                                style = MaterialTheme.typography.bodyLarge,
                            )
                            Text(
                                text = item.group?.name ?: "Nhóm",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        BudgetProgress(
                            spentMinor = item.spentMinor,
                            limitMinor = item.limitMinor,
                        )
                        val pct = (item.percent * 100).toInt()
                        Text(
                            text = "Đã chi ${fmt(item.spentMinor)} / ${fmt(item.limitMinor)} ($pct%)",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        if (item.spentMinor > item.limitMinor) {
                            Text(
                                text = "⚠️ Vượt ${fmt(item.spentMinor - item.limitMinor)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CandyPinkDeep,
                                fontWeight = FontWeight.SemiBold,
                            )
                        }
                    }
                }
            }

            // Add group budget button
            if (state.availableGroups.isNotEmpty() || state.categoryBudgets.isNotEmpty()) {
                OutlinedButton(
                    onClick = viewModel::startAddCategory,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = state.availableGroups.isNotEmpty(),
                ) {
                    Text("＋ Thêm ngân sách nhóm")
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
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    // Title
                    Text(
                        text = if (state.editorMode == EditorMode.TOTAL) "Ngân sách tổng" else "Ngân sách nhóm",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )

                    // Group picker (only when adding a new group budget)
                    if (state.editorMode == EditorMode.CATEGORY && !state.isEditing) {
                        Text(
                            text = "Chọn nhóm",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            state.availableGroups.forEach { group ->
                                FilterChip(
                                    selected = state.editorGroupId == group.id,
                                    onClick = { viewModel.selectEditorGroup(group.id) },
                                    label = { Text("${group.icon} ${group.name}") },
                                )
                            }
                        }
                    }

                    // Amount field
                    OutlinedTextField(
                        value = state.draftAmountText,
                        onValueChange = viewModel::onAmountChange,
                        label = { Text("Số tiền (${state.currency.symbol})") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )

                    // Save button
                    Button(
                        onClick = viewModel::saveEditor,
                        enabled = state.canSave,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Lưu")
                    }

                    // Delete button (only when editing an existing budget)
                    if (state.isEditing) {
                        TextButton(
                            onClick = viewModel::removeEditor,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.textButtonColors(contentColor = CandyPinkDeep),
                        ) {
                            Text("Xoá ngân sách này")
                        }
                    }

                    // Cancel
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
