package com.psy.ui.manage.account

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
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
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Lucide
import com.composables.icons.lucide.Plus
import com.psy.domain.model.Account
import com.psy.domain.model.AccountType
import com.psy.ui.components.ColorPicker
import com.psy.ui.components.EmptyState
import com.psy.ui.components.IconPicker
import com.psy.ui.components.IconTile
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.PlexMono
import com.psy.ui.theme.SpaceGrotesk

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageAccountsScreen(
    onBack: () -> Unit,
    viewModel: ManageAccountsViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    Box(Modifier.fillMaxSize().background(colors.bg)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 22.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 12.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                        .background(colors.surface).clickable(onClick = onBack),
                ) { Icon(Lucide.ArrowLeft, "Quay lại", tint = colors.text, modifier = Modifier.size(20.dp)) }
                Text("Quản lý tài khoản", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.text)
            }

            if (state.accounts.isEmpty()) {
                EmptyState(iconName = "wallet", title = "Chưa có tài khoản", caption = "Thêm tài khoản đầu tiên.")
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = androidx.compose.foundation.layout.PaddingValues(top = 4.dp, bottom = 96.dp),
                ) {
                    items(state.accounts, key = { it.id }) { account ->
                        AccountRow(account = account, onEdit = { viewModel.startEdit(account) })
                    }
                }
            }
        }

        // FAB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomEnd).padding(22.dp)
                .size(56.dp).clip(RoundedCornerShape(16.dp)).background(colors.blue)
                .clickable(onClick = viewModel::startAdd),
        ) { Icon(Lucide.Plus, "Thêm tài khoản", tint = Color.White, modifier = Modifier.size(26.dp)) }
    }

    if (state.editorOpen) {
        ModalBottomSheet(onDismissRequest = viewModel::closeEditor, sheetState = sheetState) {
            AccountEditor(
                state = state,
                onNameChange = viewModel::onNameChange,
                onTypeChange = viewModel::onTypeChange,
                onIconChange = viewModel::onIconChange,
                onColorChange = viewModel::onColorChange,
                onIsFundChange = viewModel::onIsFundChange,
                onSave = viewModel::saveEditor,
                onCancel = viewModel::closeEditor,
            )
        }
    }
}

private fun AccountType.toVietnamese(): String = when (this) {
    AccountType.CASH -> "Tiền mặt"
    AccountType.BANK -> "Ngân hàng"
    AccountType.CREDIT -> "Tín dụng"
    AccountType.ASSET -> "Tài sản"
}

private fun AccountType.code(): String = when (this) {
    AccountType.CASH -> "CASH"
    AccountType.BANK -> "BANK"
    AccountType.CREDIT -> "CREDIT"
    AccountType.ASSET -> "ASSET"
}

@Composable
private fun AccountRow(
    account: Account,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(colors.surface)
            .border(1.dp, colors.hair, RoundedCornerShape(14.dp))
            .clickable(onClick = onEdit)
            .padding(horizontal = 14.dp, vertical = 12.dp),
    ) {
        IconTile(
            iconName = account.icon,
            tint = Color(account.color),
            bg = Color(account.color).copy(alpha = 0.14f),
            size = 48.dp,
        )
        Column(Modifier.weight(1f)) {
            Text(account.name, color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text(account.type.code(), fontFamily = PlexMono, fontSize = 11.sp, color = colors.text3)
        }
        Icon(Lucide.ChevronRight, null, tint = colors.text3, modifier = Modifier.size(20.dp))
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
    onIsFundChange: (Boolean) -> Unit,
    onSave: () -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalPsyColors.current
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).padding(bottom = 32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = if (state.editingId == null) "Thêm tài khoản" else "Sửa tài khoản",
            fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.text,
        )
        OutlinedTextField(
            value = state.draftName, onValueChange = onNameChange,
            label = { Text("Tên tài khoản") }, singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Text(text = "Loại tài khoản", style = MaterialTheme.typography.labelLarge, color = colors.text2)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            AccountType.entries.forEach { type ->
                FilterChip(
                    selected = state.draftType == type,
                    onClick = { onTypeChange(type) },
                    label = { Text(type.toVietnamese()) },
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(Modifier.weight(1f)) {
                Text("Quỹ", color = colors.text, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    "Không tính vào thu/chi & ngân sách",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.text3,
                )
            }
            Switch(checked = state.draftIsFund, onCheckedChange = onIsFundChange)
        }
        Text(text = "Biểu tượng", style = MaterialTheme.typography.labelLarge, color = colors.text2)
        IconPicker(selected = state.draftIcon, onPick = onIconChange)
        Text(text = "Màu sắc", style = MaterialTheme.typography.labelLarge, color = colors.text2)
        ColorPicker(selected = state.draftColor, onPick = onColorChange)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            TextButton(onClick = onCancel, modifier = Modifier.weight(1f)) { Text("Huỷ") }
            Button(
                onClick = onSave,
                enabled = state.draftName.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = colors.blue),
                modifier = Modifier.weight(1f),
            ) { Text("Lưu") }
        }
    }
}
