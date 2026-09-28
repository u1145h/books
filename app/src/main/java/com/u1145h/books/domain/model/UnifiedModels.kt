package com.u1145h.books.domain.model

enum class ServerSource {
    Kavita,
    Audiobookshelf,
}

enum class UnifiedMediaType {
    Book,
    Comic,
    Manga,
    Audiobook,
    Podcast,
}

data class UnifiedLibrary(
    val id: String,
    val name: String,
    val source: ServerSource,
    val mediaType: UnifiedMediaType,
    val seriesCount: Int = 0,
    val coverUrl: String? = null,
)

data class UnifiedMediaItem(
    val id: String,
    val title: String,
    val author: String? = null,
    val narrator: String? = null,
    val description: String? = null,
    val coverUrl: String = "",
    val source: ServerSource,
    val kavitaSeriesId: Int? = null,
    val absItemId: String? = null,
    val readProgressPercent: Float? = null,
    val listenProgressPercent: Float? = null,
    val durationSeconds: Double? = null,
    val currentPositionSeconds: Double? = null,
    val companionAudiobookId: String? = null,
    val companionTextSeriesId: Int? = null,
) {
    val hasText: Boolean get() = kavitaSeriesId != null || companionTextSeriesId != null
    val hasAudio: Boolean get() = absItemId != null || companionAudiobookId != null
    val isDualFormat: Boolean get() = hasText && hasAudio
}
