package com.psy.ui.auth

import android.app.Activity
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.psy.R

/**
 * Button-triggered "Sign in with Google" flow via Credential Manager.
 *
 * Uses [GetSignInWithGoogleOption] (more robust than GetGoogleIdOption, which is meant for the
 * silent/bottom-sheet one-tap and fails for explicit buttons). Guards against the placeholder
 * client ID and logs failures under the "PsyGoogleSignIn" tag.
 */
suspend fun launchGoogleSignIn(
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
