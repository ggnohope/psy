package com.psy.ui.settings

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.composables.icons.lucide.ArrowLeft
import com.composables.icons.lucide.ChevronRight
import com.composables.icons.lucide.Delete
import com.composables.icons.lucide.Lock
import com.composables.icons.lucide.Lucide
import com.psy.ui.components.KeypadButton
import com.psy.ui.components.PinDot
import com.psy.ui.theme.LocalPsyColors
import com.psy.ui.theme.SpaceGrotesk

private const val PIN_LENGTH = 4

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockSettingsScreen(
    onBack: () -> Unit,
    viewModel: LockSettingsViewModel = hiltViewModel(),
) {
    val colors = LocalPsyColors.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val setPinDialogOpen by viewModel.setPinDialogOpen.collectAsStateWithLifecycle()

    Column(
        Modifier.fillMaxSize().background(colors.bg).padding(horizontal = 22.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.surface).clickable(onClick = onBack),
            ) { Icon(Lucide.ArrowLeft, "Quay lại", tint = colors.text, modifier = Modifier.size(20.dp)) }
            Text("Khoá ứng dụng", fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = colors.text)
        }

        // Settings card
        Column(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(colors.surface)
                .border(1.dp, colors.hair, RoundedCornerShape(14.dp)),
        ) {
            // Lock toggle
            ToggleRow(
                iconTileBg = colors.blueSoft, iconTint = colors.blue,
                label = "Khoá ứng dụng",
                checked = settings.lockEnabled,
                onCheckedChange = { enabled -> if (enabled) viewModel.requestEnableLock() else viewModel.disableLock() },
            )
            if (settings.lockEnabled) {
                Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hair))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable { viewModel.openSetPin() }
                        .padding(horizontal = 14.dp, vertical = 14.dp),
                ) {
                    Text("Đổi PIN", color = colors.text, modifier = Modifier.weight(1f))
                    Icon(Lucide.ChevronRight, null, tint = colors.text3, modifier = Modifier.size(20.dp))
                }
            }
            Box(Modifier.fillMaxWidth().height(1.dp).background(colors.hair))
            // Biometric toggle (dimmed when unavailable)
            val bioAvailable = viewModel.isBiometricAvailable
            ToggleRow(
                iconTileBg = colors.tealSoft, iconTint = colors.teal,
                label = "Mở bằng vân tay/khuôn mặt",
                subtitle = if (!bioAvailable) "Thiết bị chưa cài vân tay/khuôn mặt" else null,
                checked = settings.biometricEnabled,
                onCheckedChange = { viewModel.setBiometricEnabled(it) },
                enabled = settings.lockEnabled && bioAvailable,
                dim = !bioAvailable,
            )
        }
    }

    if (setPinDialogOpen) {
        SetPinDialog(
            onSave = { pin -> if (viewModel.trySavePin(pin)) viewModel.closeSetPin() },
            onCancel = { viewModel.closeSetPin() },
        )
    }
}

@Composable
private fun ToggleRow(
    iconTileBg: Color,
    iconTint: Color,
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    subtitle: String? = null,
    enabled: Boolean = true,
    dim: Boolean = false,
) {
    val colors = LocalPsyColors.current
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 12.dp)
            .then(if (dim) Modifier.alpha(0.55f) else Modifier),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)).background(iconTileBg),
        ) { Icon(Lucide.Lock, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
        Column(Modifier.weight(1f)) {
            Text(label, color = colors.text, fontSize = 15.sp)
            if (subtitle != null) Text(subtitle, color = colors.text3, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled,
            colors = SwitchDefaults.colors(checkedTrackColor = colors.blue),
        )
    }
}

/**
 * Keypad sheet to create a 4-digit PIN: enter once, then confirm. Mismatch resets to stage 1.
 * Mirrors the lock screen + iOS SetPinSheet.
 */
@Composable
private fun SetPinDialog(
    onSave: (String) -> Unit,
    onCancel: () -> Unit,
) {
    val colors = LocalPsyColors.current
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
            first = pin; pin = ""
        } else if (pin == first) {
            onSave(first)
        } else {
            error = "Mã PIN không khớp, thử lại"; first = ""; pin = ""
        }
    }

    fun deleteLastDigit() { if (pin.isNotEmpty()) pin = pin.dropLast(1) }

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            modifier = Modifier.fillMaxSize().background(colors.bg).padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)) {
                TextButton(onClick = onCancel) { Text("Huỷ", color = colors.blue) }
            }
            Spacer(modifier = Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(20.dp)).background(colors.blueSoft),
            ) { Icon(Lucide.Lock, null, tint = colors.blue, modifier = Modifier.size(34.dp)) }
            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = if (confirming) "Nhập lại mã PIN" else "Tạo mã PIN 4 số",
                fontFamily = SpaceGrotesk, fontWeight = FontWeight.SemiBold, fontSize = 18.sp, color = colors.text,
            )
            Spacer(modifier = Modifier.height(26.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { index -> PinDot(filled = index < pin.length) }
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (error.isNotEmpty()) {
                Text(error, color = colors.red, style = MaterialTheme.typography.bodyMedium)
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
            Spacer(modifier = Modifier.height(20.dp))
            for (row in 0..2) {
                Row(horizontalArrangement = Arrangement.spacedBy(24.dp)) {
                    for (col in 1..3) {
                        val digit = (row * 3 + col).toString()
                        KeypadButton(label = digit, onClick = { appendDigit(digit) })
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.size(64.dp))
                KeypadButton(label = "0", onClick = { appendDigit("0") })
                IconButton(onClick = { deleteLastDigit() }, modifier = Modifier.size(64.dp)) {
                    Icon(Lucide.Delete, "Xoá", tint = colors.text, modifier = Modifier.size(26.dp))
                }
            }
            Spacer(modifier = Modifier.weight(1f))
        }
    }
}
