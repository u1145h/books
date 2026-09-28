package com.u1145h.books.domain.manager

import com.u1145h.books.data.remote.abs.dto.AbsLibraryItemDto
import com.u1145h.books.domain.model.Series
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Pairs books between Kavita (eBooks) and Audiobookshelf (Audiobooks)
 * using metadata matching (normalized title and author).
 */
@Singleton
class MediaMatchingManager @Inject constructor() {

    private val kavitaToAbsMap = ConcurrentHashMap<Int, String>()
    private val absToKavitaMap = ConcurrentHashMap<String, Int>()

    fun indexMatches(kavitaSeries: List<Series>, absItems: List<AbsLibraryItemDto>) {
        for (series in kavitaSeries) {
            val normTitle = normalizeTitle(series.name)
            val match = absItems.firstOrNull { absItem ->
                val absTitle = normalizeTitle(absItem.media?.metadata?.title ?: "")
                if (absTitle.isNotBlank() && normTitle.isNotBlank()) {
                    absTitle == normTitle || absTitle.contains(normTitle) || normTitle.contains(absTitle)
                } else false
            }
            if (match != null) {
                kavitaToAbsMap[series.id] = match.id
                absToKavitaMap[match.id] = series.id
            }
        }
    }

    fun getCompanionAudiobookId(seriesId: Int): String? = kavitaToAbsMap[seriesId]

    fun getCompanionSeriesId(absItemId: String): Int? = absToKavitaMap[absItemId]

    fun setManualMatch(seriesId: Int, absItemId: String) {
        kavitaToAbsMap[seriesId] = absItemId
        absToKavitaMap[absItemId] = seriesId
    }

    private fun normalizeTitle(raw: String): String {
        return raw.lowercase()
            .replace(Regex("^(the|a|an)\\s+"), "")
            .replace(Regex("[:\\-_].*"), "") // strip subtitle after colon, dash or underscore
            .replace(Regex("[^a-z0-9\\s]"), "")
            .trim()
    }
}
