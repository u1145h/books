package com.u1145h.books.data.remote.abs.auth

import com.u1145h.books.data.remote.auth.SessionManager
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AbsAuthInterceptor @Inject constructor(
    private val sessionManager: SessionManager,
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        var request = chain.request()
        val absToken = sessionManager.absToken
        val absBaseUrl = sessionManager.absServerUrl

        // Dynamically resolve base URL if request is to Audiobookshelf
        if (!absBaseUrl.isNullOrBlank()) {
            val base = absBaseUrl.trimEnd('/').toHttpUrlOrNull()
            if (base != null && request.url.host == "placeholder-abs.local") {
                val newUrl = request.url.newBuilder()
                    .scheme(base.scheme)
                    .host(base.host)
                    .port(base.port)
                    .build()
                request = request.newBuilder().url(newUrl).build()
            }
        }

        val builder = request.newBuilder()
        if (!absToken.isNullOrBlank() && request.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $absToken")
        }
        return chain.proceed(builder.build())
    }
}
