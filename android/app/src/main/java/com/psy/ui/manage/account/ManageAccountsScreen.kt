package com.psy.ui.manage.account

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
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
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
import com.psy.domain.model.Account
import com.psy.domain.model.AccountType
import com.psy.ui.components.ColorPicker
import com.psy.ui.components.IconPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onBack: () -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quản lý tài khoản") },
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
                Icon(imageVector = Icons.Default.Add, contentDescription = "Thêm tài khoản")
            }
        },
    ) { paddingValues ->
        if (state.accounts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Chưa có tài khoản nào.\nNhấn + để thêm mới.",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                verticalArrangement = Arrangement.spacedBy(4.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(
                    horizontal = 16.dp,
                    vertical = 8.dp,
                ),
            ) {
                items(state.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        onEdit = { viewModel.startEdit(account) },
                    )
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
            AccountEditor(
                state = state,
                onNameChange = viewModel::onNameChange,
                onTypeChange = viewModel::onTypeChange,
                onIconChange = viewModel::onIconChange,
                onColorChange = viewModel::onColorChange,
                onSave = viewModel::saveEditor,
                onCancel = viewModel::closeEditor,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Private composables
// ─────────────────────────────────────────────────────────────────────────────

private fun AccountType.toVietnamese(): String = when (this) {
    AccountType.CASH -> "Tiền mặt"
    AccountType.BANK -> "Ngân hàng"
    AccountType.CREDIT -> "Tín dụng"
    AccountType.ASSET -> "Tài sản"
}

@Composable
private fun AccountRow(
    account: Account,
    onEdit: () -> Unit,
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
        // Emoji in tinted circle using account color
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(Color(account.color).copy(alpha = 0.25f)),
        ) {
            Text(text = account.icon, fontSize = 22.sp)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
        ) {
            Text(
                text = account.name,
                style = MaterialTheme.typography.bodyLarge,
            )
            Text(
                text = account.type.toVietnamese(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccountEditor(
    state: ManageAccountsUiState,
    onNameChange: (String) -> Unit,
    onTypeChange: (AccountType) -> Unit,
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
            text = if (state.editingId == null) "Thêm tài khoản" else "Sửa tài khoản",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
        )

        OutlinedTextField(
            value = state.draftName,
            onValueChange = onNameChange,
            label = { Text("Tên tài khoản") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(text = "Loại tài khoản", style = MaterialTheme.typography.labelLarge)
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            AccountType.entries.forEach { type ->
                FilterChip(
                    selected = state.draftType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.toVietnamese()) },
                )
            }
        }

        Text(text = "Biểu tượng", style = MaterialTheme.typography.labelLarge)
        IconPicker(
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
