package com.psy.ui.manage.category

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import com.psy.ui.components.ColorPicker
import com.psy.ui.components.EmojiPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    viewModel: ManageCategoriesViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý danh mục") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Quay lại",
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::startAdd) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm danh mục")
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // ── Tab toggle (Thu / Chi) ─────────────────────────────────────
            TabToggle(
                selected = state.type,
                onSelect = viewModel::selectTab,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            )

            // ── Category list ─────────────────────────────────────────────
            if (state.categories.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Chưa có danh mục nào.\nNhấn + để thêm mới.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(
                        horizontal = 16.dp,
                        vertical = 8.dp,
                    ),
                ) {
                    items(state.categories, key = { it.id }) { category ->
                        CategoryRow(
                            category = category,
                            onEdit = { viewModel.startEdit(category) },
                            onDelete = { viewModel.requestDelete(category) },
                        )
                    }
                }
            }
        }
    }

    // ── Editor ModalBottomSheet ───────────────────────────────────────────────
    if (state.editorOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeEditor,
            sheetState = sheetState,
        ) {
            CategoryEditor(
                state = state,
                onNameChange = viewModel::onNameChange,
                onIconChange = viewModel::onIconChange,
                onColorChange = viewModel::onColorChange,
                onSave = viewModel::saveEditor,
                onCancel = viewModel::closeEditor,
            )
        }
    }

    // ── Delete confirmation AlertDialog ───────────────────────────────────────
    state.pendingDelete?.let { cat ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDelete,
            title = { Text("Xoá danh mục") },
            text = { Text("Xoá danh mục «${cat.name}»?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDelete) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDelete) {
                    Text("Huỷ")
                }
            },
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun TabToggle(
    selected: CategoryType,
    onSelect: (CategoryType) -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        horizontalArrangement = Arrangement.Center,
    ) {
        listOf(CategoryType.EXPENSE to "Chi", CategoryType.INCOME to "Thu").forEach { (type, label) ->
            val isSelected = type == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .padding(4.dp)
                    .clip(RoundedCornerShape(50))
                    .background(
                        if (isSelected) MaterialTheme.colorScheme.primary
                        else Color.Transparent,
                    )
                    .clickable { onSelect(type) }
                    .padding(vertical = 10.dp),
            ) {
                Text(
                    text = label,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CategoryRow(
    category: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .clickable(onClick = onEdit)
            .padding(horizontal = 12.dp, vertical = 10.dp),
    ) {
        // Emoji in tinted circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(category.color).copy(alpha = 0.25f)),
        ) {
            Text(text = category.icon, fontSize = 20.sp)
        }
        Text(
            text = category.name,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Xoá",
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

@Composable
private fun CategoryEditor(
    state: ManageCategoriesUiState,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
    onColorChange: (Long) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (state.editingId == null) "Thêm danh mục" else "Sửa danh mục",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = state.draftName,
            onValueChange = onNameChange,
            label = { Text("Tên danh mục") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Biểu tượng", style = MaterialTheme.typography.labelLarge)
        EmojiPicker(
            selected = state.draftIcon,
            onPick = onIconChange,
        )

        Text(text = "Màu sắc", style = MaterialTheme.typography.labelLarge)
        ColorPicker(
            selected = state.draftColor,
            onPick = onColorChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(
                onClick = onCancel,
                modifier = Modifier.weight(1f),
            ) {
                Text("Huỷ")
            }
            Button(
                onClick = onSave,
                enabled = state.draftName.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Lưu")
            }
        }
    }
}
