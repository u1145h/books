package com.u1145h.books.data.remote.dto

import kotlinx.serialization.Serializable

// ─── Auth ─────────────────────────────────────────────────────────────────────

@Serializable
data class LoginRequest(
    val username: String = "",
    val password: String = "",
    val apiKey: String? = null,
)

@Serializable
data class TokenRefreshRequest(
    val token: String = "",
    val refreshToken: String = "",
)

@Serializable
data class TokenRefreshResponse(
    val token: String = "",
    val refreshToken: String = "",
)

@Serializable
data class AuthKeyDto(
    val id: Int = 0,
    val name: String = "",
    val key: String = "",
)

@Serializable
data class CreateAuthKeyRequest(
    val name: String = "KavitaAndroid",
)

@Serializable
data class UserDto(
    val id: Int = 0,
    val username: String = "",
    val email: String? = null,
    val token: String? = null,
    val refreshToken: String? = null,
    val apiKey: String? = null,
    val roles: List<String> = emptyList(),
    val authKeys: List<AuthKeyDto> = emptyList(),
)

// ─── Library ──────────────────────────────────────────────────────────────────

@Serializable
data class LibraryDto(
    val id: Int = 0,
    val name: String = "",
    val type: Int = 0,
    val coverImage: String? = null,
    val count: Int = 0,
    val lastModifiedUtc: String? = null,
)

// ─── Series ───────────────────────────────────────────────────────────────────

@Serializable
data class SeriesDto(
    val id: Int = 0,
    val name: String = "",
    val originalName: String? = null,
    val localizedName: String? = null,
    val sortName: String? = null,
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val userRating: Int = 0,
    val hasUserRated: Boolean = false,
    val format: Int = 0,
    val created: String? = null,
    val lastModifiedUtc: String? = null,
    val lastChapterAdded: String? = null,
    val coverImageLocked: Boolean = false,
    val latestReadDate: String? = null,
    val lastReadingProgressUtc: String? = null,
    val ageRating: Int = 0,
    val avgRating: Float = 0f,
)

@Serializable
data class SeriesListDto(
    val series: List<SeriesDto> = emptyList(),
    val totalCount: Int = 0,
    val currentPage: Int = 0,
    val pageSize: Int = 0,
)

@Serializable
data class SeriesDetailDto(
    val id: Int = 0,
    val name: String = "",
    val summary: String? = null,
    val coverImageLocked: Boolean = false,
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val format: Int = 0,
    val volumes: List<VolumeDto> = emptyList(),
    val userRating: Int = 0,
    val hasUserRated: Boolean = false,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val genres: List<TagDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val writers: List<PersonDto> = emptyList(),
    val coverArtists: List<PersonDto> = emptyList(),
    val publishers: List<PersonDto> = emptyList(),
    val characters: List<PersonDto> = emptyList(),
    val ageRating: Int = 0,
    val avgRating: Float = 0f,
)

// ─── Volume / Chapter ─────────────────────────────────────────────────────────

@Serializable
data class VolumeDto(
    val id: Int = 0,
    val number: Int = 0,
    val name: String = "",
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val lastModifiedUtc: String? = null,
    val created: String? = null,
    val seriesId: Int = 0,
    val chapters: List<ChapterDto> = emptyList(),
    val minNumber: Float = 0f,
    val maxNumber: Float = 0f,
)

@Serializable
data class ChapterDto(
    val id: Int = 0,
    val range: String = "",
    val number: String = "",
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val isSpecial: Boolean = false,
    val title: String? = null,
    val volumeId: Int = 0,
    val seriesId: Int = 0,
    val created: String? = null,
    val lastModifiedUtc: String? = null,
    val releaseDate: String? = null,
    val summary: String? = null,
    val ageRating: Int = 0,
)

@Serializable
data class ChapterInfoDto(
    val chapterNumber: String? = null,
    val volumeNumber: String? = null,
    val volumeId: Int = 0,
    val seriesName: String? = null,
    val seriesFormat: Int? = null,
    val seriesId: Int = 0,
    val libraryId: Int = 0,
    val chapterTitle: String? = null,
    val pages: Int = 0,
    val fileName: String? = null,
    val isSpecial: Boolean = false,
    val subtitle: String? = null,
    val title: String? = null,
    val seriesTotalPages: Int = 0,
    val seriesTotalPagesRead: Int = 0,
)

// ─── Progress ─────────────────────────────────────────────────────────────────

/**
 * Reading progress for a single chapter.
 * [lastModifiedUtc] is used for conflict resolution during offline progress sync.
 */
@Serializable
data class ProgressDto(
    val chapterId: Int = 0,
    val pageNum: Int = 0,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val libraryId: Int = 0,
    val bookScrollId: String? = null,
    val lastModifiedUtc: String? = null,
)

// ─── In-Progress / On-Deck ────────────────────────────────────────────────────

@Serializable
data class SeriesWithProgressDto(
    val seriesId: Int = 0,
    val seriesName: String = "",
    val libraryId: Int = 0,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val latestReadDate: String? = null,
    val lastReadingProgressUtc: String? = null,
    val format: Int = 0,
    val chapterId: Int = 0,
    val chapterNumber: String? = null,
    val volumeId: Int = 0,
)

// ─── Book / EPUB ──────────────────────────────────────────────────────────────

@Serializable
data class BookInfoDto(
    val bookTitle: String? = null,
    val seriesId: Int = 0,
    val volumeId: Int = 0,
    val seriesName: String? = null,
    val chapterNumber: String? = null,
    val volumeNumber: String? = null,
    val libraryId: Int = 0,
    val pages: Int = 0,
    val isSpecial: Boolean = false,
    val chapterTitle: String? = null,
    val seriesFormat: Int = 3,
)

// ─── Search ───────────────────────────────────────────────────────────────────

@Serializable
data class SearchResultDto(
    val seriesId: Int = 0,
    val name: String = "",
    val originalName: String? = null,
    val sortName: String? = null,
    val localizedName: String? = null,
    val format: Int = 0,
    val libraryName: String? = null,
    val libraryId: Int = 0,
    val releaseYear: Int = 0,
    val volumeCount: Int = 0,
    val chapterCount: Int = 0,
)

@Serializable
data class SearchResultGroupDto(
    val series: List<SearchResultDto> = emptyList(),
    val collections: List<CollectionDto> = emptyList(),
    val readingLists: List<ReadingListDto> = emptyList(),
    val persons: List<PersonDto> = emptyList(),
    val genres: List<TagDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val files: List<SearchFileDto> = emptyList(),
    val libraries: List<LibraryDto> = emptyList(),
    val chapters: List<ChapterDto> = emptyList(),
)

@Serializable
data class SearchFileDto(
    val seriesId: Int = 0,
    val seriesName: String = "",
    val chapterId: Int = 0,
    val fileName: String = "",
)

@Serializable
data class MarkChapterReadDto(
    val seriesId: Int = 0,
    val chapterId: Int = 0,
    val generateReadingSession: Boolean = true,
)

// ─── Collections / Reading Lists ──────────────────────────────────────────────

@Serializable
data class CollectionDto(
    val id: Int = 0,
    val title: String = "",
    val summary: String? = null,
    val itemCount: Int = 0,
    val coverImageLocked: Boolean = false,
    val lastModifiedUtc: String? = null,
)

@Serializable
data class ReadingListDto(
    val id: Int = 0,
    val title: String = "",
    val summary: String? = null,
    val itemCount: Int = 0,
    val promoted: Boolean = false,
    val coverImageLocked: Boolean = false,
    val lastModifiedUtc: String? = null,
)

// ─── Metadata helpers ─────────────────────────────────────────────────────────

@Serializable
data class TagDto(
    val id: Int = 0,
    val title: String = "",
)

@Serializable
data class PersonDto(
    val id: Int = 0,
    val name: String = "",
    val role: Int = 0,
)

// ─── Server ───────────────────────────────────────────────────────────────────

@Serializable
data class ServerInfoDto(
    val version: String = "",
    val os: String? = null,
    val dotnetVersion: String? = null,
    val isDocker: Boolean = false,
    val starrRatingCount: Int = 0,
    val numberOfLibraries: Int = 0,
    val numberOfSeries: Int = 0,
    val numberOfUsers: Int = 0,
)

// ─── Pagination helper ────────────────────────────────────────────────────────

@Serializable
data class PageRequestDto(
    val pageNumber: Int = 0,
    val pageSize: Int = 30,
    val libraryId: Int = 0,
    val isAscending: Boolean = true,
    val sortField: Int = 1, // 1 = SortName
)

// ─── Filter V2 ────────────────────────────────────────────────────────────────

@Serializable
data class SeriesFilterV2Dto(
    val id: Int = 0,
    val name: String? = null,
    val statements: List<SeriesFilterStatementDto> = emptyList(),
    val combination: Int = 0,
    val limitTo: Int = 0,
)

@Serializable
data class SeriesFilterStatementDto(
    val comparison: Int = 0, // 0 = Equal
    val field: Int = 19,     // 19 = Libraries
    val value: String? = null,
)

// ─── Series Metadata ──────────────────────────────────────────────────────────

@Serializable
data class SeriesMetadataDto(
    val id: Int = 0,
    val seriesId: Int = 0,
    val summary: String? = null,
    val genres: List<TagDto> = emptyList(),
    val tags: List<TagDto> = emptyList(),
    val writers: List<PersonDto> = emptyList(),
    val coverArtists: List<PersonDto> = emptyList(),
    val publishers: List<PersonDto> = emptyList(),
    val characters: List<PersonDto> = emptyList(),
    val ageRating: Int = 0,
    val releaseYear: Int = 0,
)


