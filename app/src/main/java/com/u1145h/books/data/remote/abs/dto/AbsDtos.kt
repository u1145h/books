package com.u1145h.books.data.remote.abs.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// — Auth —

@Serializable
data class AbsLoginRequest(
    val username: String = "",
    val password: String = "",
)

@Serializable
data class AbsLoginResponse(
    val user: AbsUserDto? = null,
    val userDefaultLibraryId: String? = null,
)

@Serializable
data class AbsUserDto(
    val id: String = "",
    val username: String = "",
    val type: String? = null,
    val token: String? = null,
)

// — Libraries —

@Serializable
data class AbsLibrariesResponse(
    val libraries: List<AbsLibraryDto> = emptyList(),
)

@Serializable
data class AbsLibraryDto(
    val id: String = "",
    val name: String = "",
    val mediaType: String = "book", // "book" or "podcast"
    val icon: String? = null,
    val displayOrder: Int = 0,
)

// — Library Items (Books) —

@Serializable
data class AbsLibraryItemsResponse(
    val results: List<AbsLibraryItemDto> = emptyList(),
    val total: Int = 0,
    val limit: Int = 50,
    val page: Int = 0,
)

@Serializable
data class AbsLibraryItemDto(
    val id: String = "",
    val libraryId: String? = null,
    val mediaType: String? = "book",
    val media: AbsMediaDto? = null,
    val numFiles: Int? = null,
    val size: Long? = null,
)

@Serializable
data class AbsMediaDto(
    val metadata: AbsMetadataDto = AbsMetadataDto(),
    val coverPath: String? = null,
    val duration: Double? = null,
    val numTracks: Int? = null,
    val numAudioFiles: Int? = null,
    val numChapters: Int? = null,
    val chapters: List<AbsChapterDto> = emptyList(),
    val tracks: List<AbsTrackDto> = emptyList(),
)

@Serializable
data class AbsMetadataDto(
    val title: String? = "",
    val subtitle: String? = null,
    val authorName: String? = "",
    val authors: List<AbsAuthorDto> = emptyList(),
    val narratorName: String? = null,
    val series: List<AbsSeriesDto> = emptyList(),
    val genres: List<String> = emptyList(),
    val publishedYear: String? = null,
    val description: String? = null,
    val isbn: String? = null,
    val asin: String? = null,
)

@Serializable
data class AbsAuthorDto(
    val id: String? = null,
    val name: String = "",
)

@Serializable
data class AbsSeriesDto(
    val id: String? = null,
    val name: String = "",
    val sequence: String? = null,
)

@Serializable
data class AbsChapterDto(
    val id: Int = 0,
    val start: Double = 0.0,
    val end: Double = 0.0,
    val title: String = "",
)

@Serializable
data class AbsTrackDto(
    val index: Int = 0,
    val startOffset: Double = 0.0,
    val duration: Double = 0.0,
    val title: String? = null,
    val contentUrl: String? = null,
)

// — Personalized Sections (Continue Listening) —

@Serializable
data class AbsPersonalizedSectionDto(
    val id: String = "",
    val label: String = "",
    val type: String = "",
    val entities: List<AbsLibraryItemDto> = emptyList(),
)

// — Playback Session —

@Serializable
data class AbsPlaySessionResponse(
    val id: String = "",
    val userId: String? = null,
    val libraryItemId: String = "",
    val mediaType: String? = "book",
    val displayTitle: String? = null,
    val displayAuthor: String? = null,
    val coverPath: String? = null,
    val duration: Double = 0.0,
    val currentTime: Double = 0.0,
    val audioTracks: List<AbsAudioTrackDto> = emptyList(),
    val chapters: List<AbsChapterDto> = emptyList(),
)

@Serializable
data class AbsAudioTrackDto(
    val index: Int = 0,
    val startOffset: Double = 0.0,
    val duration: Double = 0.0,
    val title: String? = null,
    val contentUrl: String = "",
    val mimeType: String? = "audio/mpeg",
)

// — Progress Sync —

@Serializable
data class AbsProgressRequest(
    val currentTime: Double = 0.0,
    val timeListened: Double = 0.0,
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    val isFinished: Boolean = false,
)

@Serializable
data class AbsProgressResponse(
    val id: String = "",
    val libraryItemId: String = "",
    val currentTime: Double = 0.0,
    val duration: Double = 0.0,
    val progress: Double = 0.0,
    val isFinished: Boolean = false,
    val lastUpdate: Long? = null,
)
