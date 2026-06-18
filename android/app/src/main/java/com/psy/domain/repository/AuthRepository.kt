package com.psy.domain.repository

import kotlinx.coroutines.flow.Flow

data class AuthState(val signedIn: Boolean, val email: String?)

interface AuthRepository {
    val authState: Flow<AuthState>
    suspend fun signInDev(email: String): Result<Unit>
    suspend fun signInGoogle(idToken: String): Result<Unit>
    suspend fun signOut()
}
