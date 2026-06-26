package com.psy.ui.manage.category

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Pencil
import com.composables.icons.lucide.Plus
import com.composables.icons.lucide.Trash2
import com.psy.domain.model.Category
import com.psy.domain.model.CategoryType
import com.psy.ui.components.ColorPicker
import com.psy.ui.components.EmptyState
import com.psy.ui.components.IconPicker
import com.psy.ui.components.IconTile
import com.psy.ui.components.SegmentedControl
import com.psy.ui.components.clearFocusOnTap
import com.psy.ui.icons.LucideIcon
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PsyTypography

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageCategoriesScreen(
    onBack: () -> Unit,
    viewModel: ManageCategoriesViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val groupSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val leafSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val tabOptions = listOf("Chi", "Thu")
    val selectedIndex = if (state.type == CategoryType.INCOME) 1 else 0

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // ── In-page header ──────────────────────────────────────────────
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp)
                    .padding(top = 22.dp, bottom = 12.dp),
            ) {
                IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                    Icon(Lucide.ArrowLeft, contentDescription = "Quay lại", tint = colors.text)
                }
                Text(
                    text = "Quản lý danh mục",
                    style = PsyTypography.titleLarge,
                    color = colors.text,
                )
            }

            // ── Chi / Thu toggle ────────────────────────────────────────────
            SegmentedControl(
                options = tabOptions,
                selectedIndex = selectedIndex,
                onSelect = { i ->
                    viewModel.selectTab(if (i == 1) CategoryType.INCOME else CategoryType.EXPENSE)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 8.dp),
            )

            // ── Group tree ──────────────────────────────────────────────────
            if (state.groups.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        iconName = "list",
                        title = "Chưa có danh mục",
                        caption = "Thêm nhóm danh mục đầu tiên.",
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(
                        start = 22.dp,
                        end = 22.dp,
                        top = 8.dp,
                        bottom = 96.dp,
                    ),
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

        // ── Add-group FAB (pinned) ──────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(22.dp)
                .size(56.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(colors.blue)
                .clickable(onClick = viewModel::startAddGroup),
        ) {
            Icon(Lucide.Plus, contentDescription = "Thêm nhóm", tint = Color.White)
        }
    }

    // ── Group editor ModalBottomSheet ─────────────────────────────────────────
    if (state.groupEditorOpen) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeGroupEditor,
            sheetState = groupSheetState,
            containerColor = colors.surface,
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
            containerColor = colors.surface,
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
                    Text("Xoá", color = colors.red)
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
                    Text("Xoá", color = colors.red)
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
private fun GroupCard(
    gwl: GroupWithLeaves,
    onEditGroup: () -> Unit,
    onDeleteGroup: () -> Unit,
    onAddLeaf: () -> Unit,
    onEditLeaf: (Category) -> Unit,
    onDeleteLeaf: (Category) -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    val group = gwl.group
    val groupColor = Color(group.color)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(16.dp))
            .padding(vertical = 6.dp),
    ) {
        // ── Group header ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onEditGroup)
                .padding(horizontal = 14.dp, vertical = 8.dp),
        ) {
            IconTile(
                iconName = group.icon,
                tint = groupColor,
                bg = groupColor.copy(alpha = 0.14f),
                size = 42.dp,
            )
            Text(
                text = group.name,
                modifier = Modifier.weight(1f),
                style = PsyTypography.bodyLarge,
                fontWeight = FontWeight.SemiBold,
                color = colors.text,
            )
            IconButton(onClick = onEditGroup, modifier = Modifier.size(36.dp)) {
                Icon(
                    Lucide.Pencil,
                    contentDescription = "Sửa nhóm",
                    tint = colors.text3,
                    modifier = Modifier.size(18.dp),
                )
            }
            IconButton(onClick = onDeleteGroup, modifier = Modifier.size(36.dp)) {
                Icon(
                    Lucide.Trash2,
                    contentDescription = "Xoá nhóm",
                    tint = colors.red,
                    modifier = Modifier.size(18.dp),
                )
            }
        }

        // ── Hairline ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp)
                .height(1.dp)
                .background(colors.hair),
        )

        // ── Leaves ──
        gwl.leaves.forEach { leaf ->
            LeafRow(
                leaf = leaf,
                onEdit = { onEditLeaf(leaf) },
                onDelete = { onDeleteLeaf(leaf) },
            )
        }

        // ── Add leaf ──
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier
                .clickable(onClick = onAddLeaf)
                .padding(horizontal = 14.dp, vertical = 10.dp),
        ) {
            Icon(
                Lucide.Plus,
                contentDescription = null,
                tint = colors.blue,
                modifier = Modifier.size(18.dp),
            )
            Text(
                text = "Thêm mục",
                color = colors.blue,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
            )
        }
    }
}

@Composable
private fun LeafRow(
    leaf: Category,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onEdit)
            .padding(start = 14.dp, end = 14.dp, top = 6.dp, bottom = 6.dp),
    ) {
        IconTile(
            iconName = leaf.icon,
            tint = colors.text2,
            bg = colors.sunken,
            size = 34.dp,
        )
        Text(
            text = leaf.name,
            modifier = Modifier.weight(1f),
            style = PsyTypography.bodyMedium,
            color = colors.text,
        )
        IconButton(onClick = onEdit, modifier = Modifier.size(34.dp)) {
            Icon(
                Lucide.Pencil,
                contentDescription = "Sửa mục",
                tint = colors.text3,
                modifier = Modifier.size(16.dp),
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(34.dp)) {
            Icon(
                Lucide.Trash2,
                contentDescription = "Xoá mục",
                tint = colors.red,
                modifier = Modifier.size(16.dp),
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
    val colors = LocalPsyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnTap()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (state.editingGroupId == null) "Thêm nhóm" else "Sửa nhóm",
            style = PsyTypography.titleLarge,
            color = colors.text,
        )

        OutlinedTextField(
            value = state.groupDraftName,
            onValueChange = onNameChange,
            label = { Text("Tên nhóm") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Biểu tượng", style = PsyTypography.labelLarge, color = colors.text2)
        IconPicker(
            selected = state.groupDraftIcon,
            onPick = onIconChange,
        )

        Text(text = "Màu sắc", style = PsyTypography.labelLarge, color = colors.text2)
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
    val colors = LocalPsyColors.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clearFocusOnTap()
            .padding(horizontal = 22.dp)
            .padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (state.editingLeafId == null) "Thêm mục" else "Sửa mục",
            style = PsyTypography.titleLarge,
            color = colors.text,
        )

        OutlinedTextField(
            value = state.leafDraftName,
            onValueChange = onNameChange,
            label = { Text("Tên mục") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Biểu tượng", style = PsyTypography.labelLarge, color = colors.text2)
        IconPicker(
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
