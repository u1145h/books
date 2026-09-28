package com.u1145h.books.data.remote.auth

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Response

/**
 * Rewrites the scheme/host/port of every request to the currently configured
 * server URL. This lets the Retrofit instance stay fixed while the server
 * address (including migrating to HTTPS or a public domain) changes at runtime.
 */
class DynamicBaseUrlInterceptor(
    private val serverUrlProvider: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val serverUrl = serverUrlProvider().trim().trimEnd('/')
        val parsed = runCatching { serverUrl.toHttpUrlOrNull() }.getOrNull()
        val request = chain.request()
        if (parsed == null || serverUrl.isBlank()) {
            return chain.proceed(request)
        }

        val targetPort = if (parsed.port != -1) parsed.port else HttpUrl.defaultPort(parsed.scheme)

        val newUrlBuilder = request.url.newBuilder()
            .scheme(parsed.scheme)
            .host(parsed.host)
            .port(targetPort)

        // Subpath support if the Kavita instance is hosted behind a reverse proxy prefix
        val serverSegments = parsed.pathSegments.filter { it.isNotBlank() }
        if (serverSegments.isNotEmpty()) {
            val existingSegments = request.url.pathSegments.filter { it.isNotBlank() }
            for (i in request.url.pathSegments.indices.reversed()) {
                newUrlBuilder.removePathSegment(i)
            }
            for (seg in serverSegments) {
                newUrlBuilder.addPathSegment(seg)
            }
            for (seg in existingSegments) {
                newUrlBuilder.addPathSegment(seg)
            }
        }

        val finalUrl = newUrlBuilder.build()
        return chain.proceed(request.newBuilder().url(finalUrl).build())
    }
}
