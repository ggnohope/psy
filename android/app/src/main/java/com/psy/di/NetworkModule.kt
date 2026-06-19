package com.psy.di

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import com.psy.R
import com.psy.data.auth.AuthTokenStore
import com.psy.data.remote.AuthApi
import com.psy.data.remote.BackupApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttp(tokenStore: AuthTokenStore): OkHttpClient =
        OkHttpClient.Builder()
            .addInterceptor { chain ->
                val token = tokenStore.currentToken()
                val request = if (token != null) {
                    chain.request().newBuilder()
                        .addHeader("Authorization", "Bearer $token")
                        .build()
                } else {
                    chain.request()
                }
                chain.proceed(request)
            }
            .build()

    @Provides
    @Singleton
    fun provideRetrofit(
        json: Json,
        okHttp: OkHttpClient,
        @ApplicationContext context: Context,
    ): Retrofit =
        Retrofit.Builder()
            .baseUrl(context.getString(R.string.base_url))
            .client(okHttp)
            .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
            .build()

    @Provides
    @Singleton
    fun provideAuthApi(retrofit: Retrofit): AuthApi = retrofit.create(AuthApi::class.java)

    @Provides
    @Singleton
    fun provideBackupApi(retrofit: Retrofit): BackupApi = retrofit.create(BackupApi::class.java)
}
