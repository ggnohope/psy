package com.psy.ui.backup

import android.app.Activity
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.psy.R
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupScreen(
    onBack: () -> Unit,
    viewModel: BackupViewModel = hiltViewModel(),
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val lastSyncAt by viewModel.lastSyncAt.collectAsStateWithLifecycle()
    val autoBackup by viewModel.autoBackup.collectAsStateWithLifecycle()
    val uiMessage by viewModel.uiMessage.collectAsStateWithLifecycle()
    val busy by viewModel.busy.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Show snackbar on uiMessage
    LaunchedEffect(uiMessage) {
        val msg = uiMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(msg)
        viewModel.consumeMessage()
    }

    var showRestoreDialog by remember { mutableStateOf(false) }
    var devEmail by remember { mutableStateOf("") }

    val context = LocalContext.current

    if (showRestoreDialog) {
        AlertDialog(
            onDismissRequest = { showRestoreDialog = false },
            title = { Text("Khôi phục dữ liệu") },
            text = { Text("Ghi đè dữ liệu trên máy bằng bản sao lưu?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRestoreDialog = false
                        viewModel.restore()
                    },
                ) {
                    Text("Xác nhận")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreDialog = false }) {
                    Text("Huỷ")
                }
            },
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Sao lưu & đồng bộ") },
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
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Spacer(Modifier.height(8.dp))

            if (!authState.signedIn) {
                // Google Sign-In button
                Button(
                    onClick = {
                        scope.launch {
                            launchGoogleSignIn(
                                activity = context as Activity,
                                onSuccess = { idToken -> viewModel.signInGoogle(idToken) },
                                onError = { msg ->
                                    scope.launch { snackbarHostState.showSnackbar(msg) }
                                },
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Đăng nhập với Google")
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    HorizontalDivider(modifier = Modifier.weight(1f))
                    Text(
                        text = "hoặc",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(horizontal = 8.dp),
                    )
                    HorizontalDivider(modifier = Modifier.weight(1f))
                }

                // Dev login section
                OutlinedTextField(
                    value = devEmail,
                    onValueChange = { devEmail = it },
                    label = { Text("Email (dev)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Button(
                    onClick = { viewModel.signInDev(devEmail) },
                    enabled = devEmail.isNotBlank() && !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Đăng nhập (dev)")
                }
            } else {
                // Signed-in section
                Text(
                    text = authState.email ?: "Đã đăng nhập",
                    style = MaterialTheme.typography.bodyLarge,
                )
                TextButton(onClick = { viewModel.signOut() }) {
                    Text("Đăng xuất")
                }

                HorizontalDivider()

                // Backup now button
                Box(contentAlignment = Alignment.Center) {
                    Button(
                        onClick = { viewModel.backupNow() },
                        enabled = !busy,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (busy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimary,
                            )
                        } else {
                            Text("Sao lưu ngay")
                        }
                    }
                }

                // Restore button
                Button(
                    onClick = { showRestoreDialog = true },
                    enabled = !busy,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Khôi phục")
                }

                // Last sync time
                val lastSyncText = lastSyncAt?.let {
                    val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                    sdf.format(Date(it))
                } ?: "Chưa sao lưu"
                Text(
                    text = "Lần sao lưu cuối: $lastSyncText",
                    style = MaterialTheme.typography.bodyMedium,
                )

                // Auto-backup switch
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text("Tự động sao lưu")
                    Switch(
                        checked = autoBackup,
                        onCheckedChange = { viewModel.setAutoBackup(it) },
                    )
                }
            }

            Spacer(Modifier.height(16.dp))
        }
    }
}

private suspend fun launchGoogleSignIn(
    activity: Activity,
    onSuccess: (String) -> Unit,
    onError: (String) -> Unit,
) {
    val clientId = activity.getString(R.string.google_web_client_id)
    if (clientId == "REPLACE_WITH_OAUTH_WEB_CLIENT_ID") {
        onError("Chưa cấu hình Google Sign-In (placeholder client ID)")
        return
    }
    try {
        val credentialManager = CredentialManager.create(activity)
        // Button-triggered "Sign in with Google" flow (more robust than GetGoogleIdOption,
        // which is meant for the silent/bottom-sheet one-tap and fails for explicit buttons).
        val signInOption = GetSignInWithGoogleOption.Builder(clientId).build()
        val request = GetCredentialRequest.Builder()
            .addCredentialOption(signInOption)
            .build()
        val result = credentialManager.getCredential(activity, request)
        val googleCredential = GoogleIdTokenCredential.createFrom(result.credential.data)
        onSuccess(googleCredential.idToken)
    } catch (e: GetCredentialException) {
        android.util.Log.e("PsyGoogleSignIn", "GetCredentialException type=${e.type}", e)
        onError("Google: ${e.type} — ${e.message}")
    } catch (e: Exception) {
        android.util.Log.e("PsyGoogleSignIn", "Sign-in error", e)
        onError("Lỗi Google: ${e::class.simpleName} — ${e.message}")
    }
}
