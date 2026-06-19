package com.psy.data.repo

import com.psy.data.auth.AuthTokenStore
import com.psy.data.remote.AuthApi
import com.psy.data.remote.dto.GoogleLoginRequest
import com.psy.domain.repository.AuthRepository
import com.psy.domain.repository.AuthState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject

class AuthRepositoryImpl @Inject constructor(
    private val authApi: AuthApi,
    private val tokenStore: AuthTokenStore
) : AuthRepository {

    override val authState: Flow<AuthState> = combine(
        tokenStore.tokenFlow,
        tokenStore.emailFlow
    ) { token, email ->
        AuthState(signedIn = token != null, email = email)
    }

    override suspend fun signInGoogle(idToken: String): Result<Unit> = runCatching {
        val response = authApi.googleLogin(GoogleLoginRequest(idToken = idToken))
        tokenStore.setAuth(response.token, "")
    }

    override suspend fun signOut() {
        tokenStore.clearAuth()
    }
}
