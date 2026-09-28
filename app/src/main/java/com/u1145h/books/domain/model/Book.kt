package com.u1145h.books.domain.model

import com.u1145h.books.data.local.db.entity.BookEntity

/**
 * A book in the offline library (domain model).
 */
data class Book(
    val id: Long = 0,
    val chapterId: Int? = null,
    val seriesId: Int? = null,
    val volumeId: Int? = null,
    val libraryId: Int? = null,
    val title: String,
    val author: String = "",
    val series: String = "",
    val format: BookFormat,
    val filePath: String,
    val coverPath: String? = null,
    val coverUrl: String? = null,
    val fileSizeBytes: Long = 0,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val progressPercent: Float = 0f,
    val bookScrollId: String? = null,
    val timeSpentReadingMs: Long = 0,
    val downloadDateUtc: Long = System.currentTimeMillis(),
    val lastOpenedUtc: Long? = null,
    val lastModifiedUtc: Long = System.currentTimeMillis(),
    val progressDirty: Boolean = false,
    val serverProgressLastModifiedUtc: Long? = null,
)

object BookMapper {

    fun toDomain(entity: BookEntity): Book = Book(
        id = entity.id,
        chapterId = entity.chapterId,
        seriesId = entity.seriesId,
        volumeId = entity.volumeId,
        libraryId = entity.libraryId,
        title = entity.title,
        author = entity.author,
        series = entity.series,
        format = runCatching { BookFormat.valueOf(entity.format) }.getOrDefault(BookFormat.EPUB),
        filePath = entity.filePath,
        coverPath = entity.coverPath,
        coverUrl = entity.coverUrl,
        fileSizeBytes = entity.fileSizeBytes,
        totalPages = entity.totalPages,
        currentPage = entity.currentPage,
        progressPercent = entity.progressPercent,
        bookScrollId = entity.bookScrollId,
        timeSpentReadingMs = entity.timeSpentReadingMs,
        downloadDateUtc = entity.downloadDateUtc,
        lastOpenedUtc = entity.lastOpenedUtc,
        lastModifiedUtc = entity.lastModifiedUtc,
        progressDirty = entity.progressDirty,
        serverProgressLastModifiedUtc = entity.serverProgressLastModifiedUtc,
    )

    fun toEntity(book: Book): BookEntity = BookEntity(
        id = book.id,
        chapterId = book.chapterId,
        seriesId = book.seriesId,
        volumeId = book.volumeId,
        libraryId = book.libraryId,
        title = book.title,
        author = book.author,
        series = book.series,
        format = book.format.name,
        filePath = book.filePath,
        coverPath = book.coverPath,
        coverUrl = book.coverUrl,
        fileSizeBytes = book.fileSizeBytes,
        totalPages = book.totalPages,
        currentPage = book.currentPage,
        progressPercent = book.progressPercent,
        bookScrollId = book.bookScrollId,
        timeSpentReadingMs = book.timeSpentReadingMs,
        downloadDateUtc = book.downloadDateUtc,
        lastOpenedUtc = book.lastOpenedUtc,
        lastModifiedUtc = book.lastModifiedUtc,
        progressDirty = book.progressDirty,
        serverProgressLastModifiedUtc = book.serverProgressLastModifiedUtc,
    )
}
