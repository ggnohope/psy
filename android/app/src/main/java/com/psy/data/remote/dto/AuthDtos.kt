package com.psy.data.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class GoogleLoginRequest(val idToken: String)

@Serializable
data class TokenResponse(val token: String)
