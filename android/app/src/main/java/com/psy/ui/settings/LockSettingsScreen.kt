package com.psy.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSettingsScreen(
    onBack: () -> Unit,
    viewModel: LockSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val setPinDialogOpen by viewModel.setPinDialogOpen.collectAsStateWithLifecycle()
    val pinEntry by viewModel.pinEntry.collectAsStateWithLifecycle()
    val pinConfirm by viewModel.pinConfirm.collectAsStateWithLifecycle()
    val pinError by viewModel.pinError.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Khoá ứng dụng") },
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
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            // Lock switch row
            ListItem(
                headlineContent = { Text("Khoá ứng dụng") },
                leadingContent = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null)
                },
                trailingContent = {
                    Switch(
                        checked = settings.lockEnabled,
                        onCheckedChange = { enabled ->
                            if (enabled) viewModel.requestEnableLock() else viewModel.disableLock()
                        },
                    )
                },
            )

            // Change PIN row (only when lock is enabled)
            if (settings.lockEnabled) {
                ListItem(
                    headlineContent = { Text("Đổi PIN") },
                    trailingContent = {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                        )
                    },
                    modifier = Modifier.clickable { viewModel.openSetPin() },
                )
            }

            // Biometric switch row
            ListItem(
                headlineContent = { Text("Mở bằng vân tay/khuôn mặt") },
                supportingContent = if (!viewModel.isBiometricAvailable) {
                    { Text("Thiết bị chưa cài vân tay/khuôn mặt") }
                } else null,
                trailingContent = {
                    Switch(
                        checked = settings.biometricEnabled,
                        onCheckedChange = { viewModel.setBiometricEnabled(it) },
                        enabled = settings.lockEnabled && viewModel.isBiometricAvailable,
                    )
                },
            )
        }
    }

    // Set-PIN dialog
    if (setPinDialogOpen) {
        AlertDialog(
            onDismissRequest = { viewModel.closeSetPin() },
            title = { Text("Đặt PIN") },
            text = {
                Column {
                    OutlinedTextField(
                        value = pinEntry,
                        onValueChange = { viewModel.onPinEntryChange(it) },
                        label = { Text("PIN (4–6 chữ số)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pinConfirm,
                        onValueChange = { viewModel.onPinConfirmChange(it) },
                        label = { Text("Xác nhận PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (pinError.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = pinError,
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmSetPin() }) {
                    Text("Lưu")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.closeSetPin() }) {
                    Text("Huỷ")
                }
            },
        )
    }
}
