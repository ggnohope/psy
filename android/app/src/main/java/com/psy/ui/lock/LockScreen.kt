package com.psy.ui.lock

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier

/**
 * Temporary stub for LockScreen — Task 5 replaces this with the real PIN/biometric UI.
 * Signature is kept stable so AppRoot compiles now.
 */
@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    biometricEnabled: Boolean,
    verifyPin: suspend (String) -> Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Đã khoá")
        Button(onClick = onUnlock) {
            Text("Mở khoá (tạm)")
        }
    }
}
