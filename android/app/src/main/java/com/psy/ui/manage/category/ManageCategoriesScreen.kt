package com.psy.ui.manage.category

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
    val groupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val leafSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

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
            FloatingActionButton(onClick = viewModel::startAddGroup) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm nhóm")
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

            // ── Group + leaf tree ────────────────────────────────────────
            if (state.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Chưa có nhóm nào.\nNhấn + để thêm nhóm mới.",
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                ) {
                    state.groups.forEach { gwl ->
                        item(key = "group-${gwl.group.id}") {
                            GroupCard(
                                gwl = gwl,
                                onEditGroup = { viewModel.startEditGroup(gwl.group) },
                                onDeleteGroup = { viewModel.requestDeleteGroup(gwl.group) },
                                onAddLeaf = { viewModel.startAddLeaf(gwl.group.id) },
                                onEditLeaf = { viewModel.startEditLeaf(it) },
                                onDeleteLeaf = { viewModel.requestDeleteLeaf(it) },
                            )
                        }
                    }
                }
            }
        }
    }

    // ── Group editor ModalBottomSheet ─────────────────────────────────────────
    if (state.groupEditorOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeGroupEditor,
            sheetState = groupSheetState,
        ) {
            GroupEditor(
                state = state,
                onNameChange = viewModel::onGroupNameChange,
                onIconChange = viewModel::onGroupIconChange,
                onColorChange = viewModel::onGroupColorChange,
                onSave = viewModel::saveGroup,
                onCancel = viewModel::closeGroupEditor,
            )
        }
    }

    // ── Leaf editor ModalBottomSheet ──────────────────────────────────────────
    if (state.leafEditorOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeLeafEditor,
            sheetState = leafSheetState,
        ) {
            LeafEditor(
                state = state,
                onNameChange = viewModel::onLeafNameChange,
                onIconChange = viewModel::onLeafIconChange,
                onSave = viewModel::saveLeaf,
                onCancel = viewModel::closeLeafEditor,
            )
        }
    }

    // ── Delete leaf confirmation ──────────────────────────────────────────────
    state.pendingDeleteLeaf?.let { leaf ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteLeaf,
            title = { Text("Xoá mục") },
            text = { Text("Xoá mục «${leaf.name}»?") },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteLeaf) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteLeaf) {
                    Text("Huỷ")
                }
            },
        )
    }

    // ── Delete group confirmation (cascade warning) ───────────────────────────
    state.pendingDeleteGroup?.let { group ->
        AlertDialog(
            onDismissRequest = viewModel::cancelDeleteGroup,
            title = { Text("Xoá nhóm") },
            text = {
                Text(
                    "Xoá nhóm «${group.name}» và tất cả mục con của nó? " +
                        "Các giao dịch cũ thuộc nhóm này sẽ mất danh mục.",
                )
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmDeleteGroup) {
                    Text("Xoá", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::cancelDeleteGroup) {
                    Text("Huỷ")
                }
            },
        )
    }

    // ── Message dialog (e.g. last-leaf guard) ─────────────────────────────────
    state.message?.let { msg ->
        AlertDialog(
            onDismissRequest = viewModel::clearMessage,
            title = { Text("Thông báo") },
            text = { Text(msg) },
            confirmButton = {
                TextButton(onClick = viewModel::clearMessage) {
                    Text("OK")
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
private fun GroupCard(
    gwl: GroupWithLeaves,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAddLeaf: () -> Unit,
    onEditLeaf: (Category) -> Unit,
    onDeleteLeaf: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val group = gwl.group
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .padding(vertical = 8.dp),
    ) {
        // ── Group header row ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditGroup)
                .padding(horizontal = 12.dp, vertical = 6.dp),
        ) {
            // Emoji in tinted circle with color dot overlay
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color(group.color).copy(alpha = 0.25f)),
            ) {
                Text(text = group.icon, fontSize = 20.sp)
            }
            Spacer(modifier = Modifier.size(12.dp))
            // Color dot
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(group.color)),
            )
            Text(
                text = group.name,
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 8.dp),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            IconButton(onClick = onEditGroup) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Sửa nhóm",
                )
            }
            IconButton(onClick = onDeleteGroup) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Xoá nhóm",
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }

        // ── Leaves ──
        gwl.leaves.forEach { leaf ->
            LeafRow(
                leaf = leaf,
                onEdit = { onEditLeaf(leaf) },
                onDelete = { onDeleteLeaf(leaf) },
            )
        }

        // ── Add leaf action ──
        TextButton(
            onClick = onAddLeaf,
            modifier = Modifier.padding(start = 44.dp),
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(4.dp))
            Text("mục")
        }
    }
}

@Composable
private fun LeafRow(
    leaf: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 44.dp, end = 12.dp, top = 4.dp, bottom = 4.dp),
    ) {
        Text(text = leaf.icon, fontSize = 18.sp)
        Text(
            text = leaf.name,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            style = MaterialTheme.typography.bodyLarge,
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = "Sửa mục",
                modifier = Modifier.size(18.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Xoá mục",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

@Composable
private fun GroupEditor(
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
            text = if (state.editingGroupId == null) "Thêm nhóm" else "Sửa nhóm",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = state.groupDraftName,
            onValueChange = onNameChange,
            label = { Text("Tên nhóm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Biểu tượng", style = MaterialTheme.typography.labelLarge)
        EmojiPicker(
            selected = state.groupDraftIcon,
            onPick = onIconChange,
        )

        Text(text = "Màu sắc", style = MaterialTheme.typography.labelLarge)
        ColorPicker(
            selected = state.groupDraftColor,
            onPick = onColorChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Huỷ")
            }
            Button(
                onClick = onSave,
                enabled = state.groupDraftName.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Lưu")
            }
        }
    }
}

@Composable
private fun LeafEditor(
    state: ManageCategoriesUiState,
    onNameChange: (String) -> Unit,
    onIconChange: (String) -> Unit,
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
            text = if (state.editingLeafId == null) "Thêm mục" else "Sửa mục",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = state.leafDraftName,
            onValueChange = onNameChange,
            label = { Text("Tên mục") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Biểu tượng", style = MaterialTheme.typography.labelLarge)
        EmojiPicker(
            selected = state.leafDraftIcon,
            onPick = onIconChange,
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                Text("Huỷ")
            }
            Button(
                onClick = onSave,
                enabled = state.leafDraftName.isNotBlank(),
                modifier = Modifier.weight(1f),
            ) {
                Text("Lưu")
            }
        }
    }
}
