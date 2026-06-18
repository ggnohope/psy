package com.psy.data.remote

import com.psy.data.remote.dto.BackupRequest
import com.psy.data.remote.dto.BackupResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

interface BackupApi {
    @POST("backup")
    suspend fun upload(@Body req: BackupRequest)

    @GET("backup")
    suspend fun download(): Response<BackupResponse>
}
