package com.u1145h.books.core.util

import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Builds Coil-loadable image URLs for Kavita cover art.
 * Appends apiKey if present for guaranteed loading across clients.
 */
@Singleton
class CoverUrlBuilder @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
) {
    /** URL for a series cover image. */
    fun series(seriesId: Int): String = build("Image/series-cover?seriesId=$seriesId")

    /** URL for a library cover image. */
    fun library(libraryId: Int): String = build("Image/library-cover?libraryId=$libraryId")

    /** URL for a chapter cover image. */
    fun chapter(chapterId: Int): String = build("Image/chapter-cover?chapterId=$chapterId")

    private fun build(path: String): String {
        val base = settingsRepository.currentServerUrl.trimEnd('/')
        val apiKey = sessionManager.apiKey
        val sep = if (path.contains("?")) "&" else "?"
        val authSuffix = if (!apiKey.isNullOrBlank()) "${sep}apiKey=$apiKey" else ""
        return if (base.isNotBlank()) "$base/api/$path$authSuffix" else ""
    }
}
