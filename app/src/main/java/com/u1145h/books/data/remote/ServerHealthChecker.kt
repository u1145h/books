package com.u1145h.books.data.remote

import com.u1145h.books.core.config.ServerConfig
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import okhttp3.OkHttpClient
import okhttp3.Request

/**
 * Verifies that a candidate server address is reachable by hitting Kavita's
 * public health endpoint. Uses a dedicated short-timeout client so the
 * first-run setup gives fast feedback.
 */
@Singleton
class ServerHealthChecker @Inject constructor() {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .writeTimeout(5, TimeUnit.SECONDS)
        .build()

    suspend fun check(baseUrl: String): Result<Unit> =
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
            runCatching {
                val request = Request.Builder()
                    .url("${baseUrl}${ServerConfig.API_PATH}health")
                    .build()
                client.newCall(request).execute().use { response ->
                    check(response.isSuccessful) { "Server returned HTTP ${response.code}" }
                }
            }
        }
}
