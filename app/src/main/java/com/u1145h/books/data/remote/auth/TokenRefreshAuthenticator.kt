package com.u1145h.books.data.remote.auth

import com.u1145h.books.data.repository.SettingsRepository
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

@Serializable
private data class TokenRefreshPayload(
    val token: String,
    val refreshToken: String,
)

/**
 * Automatically intercepts HTTP 401 Unauthorized responses and attempts to refresh the
 * Kavita JWT token using the stored refresh token. If successful, updates the [SessionManager]
 * and retries the original request transparently.
 */
@Singleton
class TokenRefreshAuthenticator @Inject constructor(
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
) : Authenticator {

    private val json = Json { ignoreUnknownKeys = true }
    private val rawClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Synchronized
    override fun authenticate(route: Route?, response: Response): Request? {
        // Do not retry more than twice to prevent loops
        if (responseCount(response) >= 3) return null

        // Avoid refreshing on authentication endpoints themselves
        val path = response.request.url.encodedPath
        if (path.contains("Account/login") || path.contains("Account/refresh-token")) {
            return null
        }

        val currentSession = sessionManager.session.value
        val oldToken = currentSession.token ?: return null
        val refreshToken = currentSession.refreshToken ?: return null

        // If another thread has already refreshed the token, retry immediately with that new token
        val authHeader = response.request.header("Authorization")
        if (authHeader != null && authHeader != "Bearer $oldToken") {
            return response.request.newBuilder()
                .header("Authorization", "Bearer $oldToken")
                .build()
        }

        val serverUrl = settingsRepository.currentServerUrl.trimEnd('/')
        if (serverUrl.isBlank()) return null

        val refreshUrl = "$serverUrl/api/Account/refresh-token"
        val payload = json.encodeToString(
            TokenRefreshPayload.serializer(),
            TokenRefreshPayload(token = oldToken, refreshToken = refreshToken),
        )
        val body = payload.toRequestBody("application/json".toMediaType())

        val refreshRequest = Request.Builder()
            .url(refreshUrl)
            .post(body)
            .build()

        return runCatching {
            rawClient.newCall(refreshRequest).execute().use { refreshResponse ->
                if (!refreshResponse.isSuccessful) {
                    return null
                }
                val respBody = refreshResponse.body?.string() ?: return null
                val result = json.decodeFromString(TokenRefreshPayload.serializer(), respBody)

                // Update session synchronously & persist
                runBlocking {
                    sessionManager.update(
                        currentSession.copy(
                            token = result.token,
                            refreshToken = result.refreshToken,
                        )
                    )
                }

                // Retry original request with the new token
                response.request.newBuilder()
                    .header("Authorization", "Bearer ${result.token}")
                    .build()
            }
        }.getOrNull()
    }

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
