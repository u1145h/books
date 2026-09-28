package com.u1145h.books.di

import com.u1145h.books.BuildConfig
import com.u1145h.books.core.config.ServerConfig
import com.u1145h.books.data.remote.abs.api.AudiobookshelfApiService
import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.auth.AuthInterceptor
import com.u1145h.books.data.remote.auth.DynamicBaseUrlInterceptor
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.SettingsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    @Provides
    @Singleton
    @Named("apiBaseUrl")
    fun provideApiBaseUrl(): String = "http://127.0.0.1/api/"

    @Provides
    @Singleton
    fun provideOkHttpClient(
        settingsRepository: SettingsRepository,
        sessionManager: SessionManager,
        authenticator: com.u1145h.books.data.remote.auth.TokenRefreshAuthenticator,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ServerConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ServerConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ServerConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .authenticator(authenticator)
            .addInterceptor(DynamicBaseUrlInterceptor { settingsRepository.currentServerUrl })
            .addInterceptor(
                AuthInterceptor(
                    tokenProvider = { sessionManager.token },
                    apiKeyProvider = { sessionManager.apiKey },
                )
            )

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(
        client: OkHttpClient,
        @Named("apiBaseUrl") baseUrl: String,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideKavitaApiService(retrofit: Retrofit): KavitaApiService =
        retrofit.create(KavitaApiService::class.java)

    @Provides
    @Singleton
    @Named("absOkHttpClient")
    fun provideAbsOkHttpClient(
        settingsRepository: SettingsRepository,
        sessionManager: SessionManager,
    ): OkHttpClient {
        val builder = OkHttpClient.Builder()
            .connectTimeout(ServerConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ServerConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .writeTimeout(ServerConfig.WRITE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .addInterceptor(DynamicBaseUrlInterceptor { settingsRepository.currentAbsServerUrl })
            .addInterceptor { chain ->
                val request = chain.request()
                val token = sessionManager.absToken
                val reqBuilder = request.newBuilder()
                if (!token.isNullOrBlank() && request.header("Authorization") == null) {
                    reqBuilder.header("Authorization", "Bearer $token")
                }
                chain.proceed(reqBuilder.build())
            }

        if (BuildConfig.DEBUG) {
            builder.addInterceptor(
                HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC },
            )
        }
        return builder.build()
    }

    @Provides
    @Singleton
    @Named("absRetrofit")
    fun provideAbsRetrofit(
        @Named("absOkHttpClient") client: OkHttpClient,
        @Named("apiBaseUrl") baseUrl: String,
    ): Retrofit = Retrofit.Builder()
        .baseUrl(baseUrl)
        .client(client)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()

    @Provides
    @Singleton
    fun provideAudiobookshelfApiService(@Named("absRetrofit") retrofit: Retrofit): AudiobookshelfApiService =
        retrofit.create(AudiobookshelfApiService::class.java)
}
