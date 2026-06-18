package com.psy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class DevLoginRequest(val email: String, val name: String? = null)

@Serializable
data class GoogleLoginRequest(val idToken: String)

@Serializable
data class TokenResponse(val token: String)
