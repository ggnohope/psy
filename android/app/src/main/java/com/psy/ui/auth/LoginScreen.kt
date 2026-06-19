package com.psy.ui.auth

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.psy.ui.app.AppViewModel
import kotlinx.coroutines.launch

/**
 * Welcome / login gate. Google-only sign-in; no dev/email login.
 * On success [AppViewModel.signInGoogle] is called, which flips `isSignedIn` and opens the gate.
 */
@Composable
fun LoginScreen(viewModel: AppViewModel) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val message by viewModel.uiMessage.collectAsStateWithLifecycle()

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "🌸", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text(
                text = "Psy",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Ghi chép chi tiêu dễ thương",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(48.dp))
            Button(
                onClick = {
                    scope.launch {
                        launchGoogleSignIn(
                            activity = context as Activity,
                            onSuccess = { idToken -> viewModel.signInGoogle(idToken) },
                            onError = { msg -> viewModel.showMessage(msg) },
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Đăng nhập với Google")
            }
            if (message != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = message ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}
