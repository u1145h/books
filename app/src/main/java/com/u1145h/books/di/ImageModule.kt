package com.u1145h.books.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.u1145h.books.core.config.ServerConfig
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.remote.auth.TokenRefreshAuthenticator
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Provides a Coil [ImageLoader] that injects the Kavita JWT into every
 * image request, so cover images behind authenticated endpoints load
 * correctly without a separate download step.
 */
@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageLoader(
        @ApplicationContext context: Context,
        sessionManager: SessionManager,
        authenticator: TokenRefreshAuthenticator,
    ): ImageLoader {
        val client = OkHttpClient.Builder()
            .connectTimeout(ServerConfig.CONNECT_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(ServerConfig.READ_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .authenticator(authenticator)
            .addInterceptor { chain ->
                var request = chain.request()
                val token = sessionManager.token
                val apiKey = sessionManager.apiKey
                val builder = request.newBuilder()
                if (!token.isNullOrBlank() && request.header("Authorization") == null) {
                    builder.header("Authorization", "Bearer $token")
                }
                if (!apiKey.isNullOrBlank() && request.header("x-api-key") == null) {
                    builder.header("x-api-key", apiKey)
                }
                request = builder.build()
                var response = chain.proceed(request)

                // If redirected (301, 302, 307, 308), re-apply auth on same-host or resolved location
                if (response.isRedirect) {
                    val location = response.header("Location")
                    if (!location.isNullOrBlank()) {
                        val newUrl = request.url.resolve(location)
                        if (newUrl != null) {
                            response.close()
                            val nextReq = request.newBuilder()
                                .url(newUrl)
                                .apply {
                                    if (!token.isNullOrBlank()) header("Authorization", "Bearer $token")
                                    if (!apiKey.isNullOrBlank()) header("x-api-key", apiKey)
                                }
                                .build()
                            response = chain.proceed(nextReq)
                        }
                    }
                }
                response
            }
            .apply {
                if (com.u1145h.books.BuildConfig.DEBUG) {
                    addInterceptor(
                        okhttp3.logging.HttpLoggingInterceptor { msg ->
                            android.util.Log.d("KavitaCoil", msg)
                        }.apply {
                            level = okhttp3.logging.HttpLoggingInterceptor.Level.BASIC
                        }
                    )
                }
            }
            .build()

        return ImageLoader.Builder(context)
            .okHttpClient(client)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(context.cacheDir.resolve("coil_covers"))
                    .maxSizePercent(0.05)
                    .build()
            }
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
    }
}
