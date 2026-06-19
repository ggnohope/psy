package com.psy.data.remote

import com.psy.data.remote.dto.GoogleLoginRequest
import com.psy.data.remote.dto.TokenResponse
import retrofit2.http.Body
import retrofit2.http.POST

interface AuthApi {
    @POST("auth/google")
    suspend fun googleLogin(@Body req: GoogleLoginRequest): TokenResponse
}
