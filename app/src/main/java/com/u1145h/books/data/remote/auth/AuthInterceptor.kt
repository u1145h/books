package com.u1145h.books.data.remote.auth

import okhttp3.Interceptor
import okhttp3.Response

/**
 * Attaches the Kavita JWT as a Bearer token and/or API Key when a session is active.
 */
class AuthInterceptor(
    private val tokenProvider: () -> String?,
    private val apiKeyProvider: () -> String? = { null },
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = tokenProvider()
        val apiKey = apiKeyProvider()
        val builder = chain.request().newBuilder()

        if (!token.isNullOrBlank()) {
            builder.header("Authorization", "Bearer $token")
        }
        if (!apiKey.isNullOrBlank()) {
            builder.header("x-api-key", apiKey)
        }

        return chain.proceed(builder.build())
    }
}
