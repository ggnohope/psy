package com.psy.ui.lock

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.launch

private const val PIN_MAX_LENGTH = 6

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    biometricEnabled: Boolean,
    verifyPin: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    // Block system back while screen is showing
    BackHandler(enabled = true) { /* no-op — back is blocked while locked */ }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var pin by remember { mutableStateOf("") }
    var error by remember { mutableStateOf("") }

    // Auto-launch biometric prompt on first composition when biometric is enabled
    LaunchedEffect(Unit) {
        if (biometricEnabled) {
            (context as? FragmentActivity)?.let { activity ->
                BiometricAuthenticator.authenticate(
                    activity = activity,
                    onSuccess = onUnlock,
                    onError = { /* dismissed — fall through to PIN */ },
                )
            }
        }
    }

    fun appendDigit(digit: String) {
        if (pin.length < PIN_MAX_LENGTH) {
            pin += digit
            error = ""
            // Auto-submit when 6 digits entered
            if (pin.length == PIN_MAX_LENGTH) {
                val currentPin = pin
                scope.launch {
                    if (verifyPin(currentPin)) onUnlock()
                    else {
                        error = "Sai PIN, thử lại"
                        pin = ""
                    }
                }
            }
        }
    }

    fun deleteLastDigit() {
        if (pin.isNotEmpty()) pin = pin.dropLast(1)
    }

    fun submitPin() {
        if (pin.length in 4..PIN_MAX_LENGTH) {
            val currentPin = pin
            scope.launch {
                if (verifyPin(currentPin)) onUnlock()
                else {
                    error = "Sai PIN, thử lại"
                    pin = ""
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(vertical = 48.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Title
        Text(
            text = "🔒",
            fontSize = 48.sp,
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Psy",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary,
        )

        Spacer(modifier = Modifier.height(32.dp))

        // PIN dots
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            repeat(PIN_MAX_LENGTH) { index ->
                PinDot(filled = index < pin.length)
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Error message
        if (error.isNotEmpty()) {
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        } else {
            Spacer(modifier = Modifier.height(20.dp))
        }

        Spacer(modifier = Modifier.height(8.dp))

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

        // Bottom row: biometric | 0 | backspace
        Row(
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Biometric button or spacer
            if (biometricEnabled) {
                IconButton(
                    onClick = {
                        (context as? FragmentActivity)?.let { activity ->
                            BiometricAuthenticator.authenticate(
                                activity = activity,
                                onSuccess = onUnlock,
                                onError = { /* dismissed */ },
                            )
                        }
                    },
                    modifier = Modifier.size(64.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Fingerprint,
                        contentDescription = "Vân tay / Khuôn mặt",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp),
                    )
                }
            } else {
                Spacer(modifier = Modifier.size(64.dp))
            }

            KeypadButton(label = "0", onClick = { appendDigit("0") })

            // Backspace
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

        Spacer(modifier = Modifier.height(16.dp))

        // Confirm button when 4–5 digits entered (6 digits auto-submits)
        if (pin.length in 4 until PIN_MAX_LENGTH) {
            FilledTonalButton(onClick = { submitPin() }) {
                Text("Mở khoá")
            }
        }

        // Biometric prompt shortcut button
        if (biometricEnabled) {
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
                onClick = {
                    (context as? FragmentActivity)?.let { activity ->
                        BiometricAuthenticator.authenticate(
                            activity = activity,
                            onSuccess = onUnlock,
                            onError = { /* dismissed */ },
                        )
                    }
                },
            ) {
                Text("Vân tay / Khuôn mặt")
            }
        }
    }
}

@Composable
private fun PinDot(filled: Boolean) {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(
                if (filled) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.outline,
            ),
    )
}

@Composable
private fun KeypadButton(label: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(64.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .then(
                Modifier.padding(0.dp), // tappable area handled by clip+background
            ),
    ) {
        TextButton(
            onClick = onClick,
            modifier = Modifier.size(64.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
