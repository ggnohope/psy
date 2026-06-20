package com.psy.ui.settings

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.ui.components.KeypadButton
import com.psy.ui.components.PinDot

private const val PIN_LENGTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSettingsScreen(
    onBack: () -> Unit,
    viewModel: LockSettingsViewModel = hiltViewModel(),
) {
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val setPinDialogOpen by viewModel.setPinDialogOpen.collectAsStateWithLifecycle()

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

    // Keypad-based set-PIN dialog (two-stage: enter → confirm)
    if (setPinDialogOpen) {
        SetPinDialog(
            onSave = { pin ->
                if (viewModel.trySavePin(pin)) viewModel.closeSetPin()
            },
            onCancel = { viewModel.closeSetPin() },
        )
    }
}

/**
 * Candy Pop keypad sheet to create a 4-digit PIN: enter once, then confirm.
 * Mismatch shows an error and resets to stage 1. Mirrors the lock screen + iOS SetPinSheet.
 */
@Composable
private fun SetPinDialog(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    var first by remember { mutableStateOf("") }
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    val confirming = first.length == PIN_LENGTH

    fun appendDigit(digit: String) {
        if (pin.length >= PIN_LENGTH) return
        pin += digit
        error = ""
        if (pin.length < PIN_LENGTH) return
        if (first.isEmpty()) {
            // Captured first entry → move to confirm stage
            first = pin
            pin = ""
        } else if (pin == first) {
            onSave(first)
        } else {
            error = "Mã PIN không khớp, thử lại"
            first = ""
            pin = ""
        }
    }

    fun deleteLastDigit() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    Dialog(
        onDismissRequest = onCancel,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Top bar with cancel
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = onCancel) {
                    Text("Huỷ", color = MaterialTheme.colorScheme.primary)
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Title
            Text(text = "🔐", fontSize = 44.sp)
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (confirming) "Nhập lại mã PIN" else "Tạo mã PIN 4 số",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface,
            )

            Spacer(modifier = Modifier.height(26.dp))

            // PIN dots
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { index ->
                    PinDot(filled = index < pin.length)
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Error message (reserve space so layout doesn't jump)
            if (error.isNotEmpty()) {
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Numeric keypad rows 1–3
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 1..3) {
                        val digit = (row * 3 + col).toString()
                        KeypadButton(label = digit, onClick = { appendDigit(digit) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Bottom row: spacer | 0 | backspace (no biometric when setting a PIN)
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.size(64.dp))

                KeypadButton(label = "0", onClick = { appendDigit("0") })

                IconButton(
                    onClick = { deleteLastDigit() },
                    modifier = Modifier.size(64.dp),
                ) {
                    Text(
                        text = "⌫",
                        fontSize = 24.sp,
                        color = MaterialTheme.colorScheme.onBackground,
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
