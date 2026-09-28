package com.u1145h.books.domain.model

/** A Kavita library (manga, comics, books, etc.). */
data class Library(
    val id: Int,
    val name: String,
    val type: LibraryType,
    val seriesCount: Int = 0,
)

enum class LibraryType(val value: Int) {
    Manga(0),
    Comic(1),
    Book(2),
    Image(3),
    LightNovel(4),
    ComicVine(5);

    companion object {
        fun fromValue(v: Int) = entries.firstOrNull { it.value == v } ?: Manga
    }
}

/** A series inside a library. */
data class Series(
    val id: Int,
    val name: String,
    val sortName: String = "",
    val localizedName: String? = null,
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val format: Int = 0,
    val pagesRead: Int = 0,
    val pages: Int = 0,
    val userRating: Int = 0,
    val avgRating: Float = 0f,
    val latestReadDate: String? = null,
    val lastReadingProgressUtc: String? = null,
    val created: String? = null,
) {
    val progressPercent: Float
        get() = if (pages > 0) pagesRead.toFloat() / pages else 0f
}

/** A volume inside a series. */
data class Volume(
    val id: Int,
    val number: Int,
    val name: String,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val seriesId: Int = 0,
    val chapters: List<Chapter> = emptyList(),
)

/** A chapter inside a volume. */
data class Chapter(
    val id: Int,
    val number: String,
    val range: String = "",
    val title: String? = null,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val isSpecial: Boolean = false,
    val volumeId: Int = 0,
    val seriesId: Int = 0,
)

/** Full series metadata with volumes and chapters. */
data class SeriesDetail(
    val id: Int,
    val name: String,
    val summary: String? = null,
    val libraryId: Int = 0,
    val libraryName: String? = null,
    val format: Int = 0,
    val pages: Int = 0,
    val pagesRead: Int = 0,
    val volumes: List<Volume> = emptyList(),
    val genres: List<String> = emptyList(),
    val tags: List<String> = emptyList(),
    val writers: List<String> = emptyList(),
    val publishers: List<String> = emptyList(),
    val ageRating: Int = 0,
    val avgRating: Float = 0f,
) {
    val progressPercent: Float
        get() = if (pages > 0) pagesRead.toFloat() / pages else 0f

    val allChapters: List<Chapter>
        get() = volumes.flatMap { it.chapters }
}

/** A collection of series. */
data class Collection(
    val id: Int,
    val title: String,
    val summary: String? = null,
    val itemCount: Int = 0,
)

/** Search results. */
data class SearchResult(
    val series: List<Series> = emptyList(),
    val collections: List<Collection> = emptyList(),
)
