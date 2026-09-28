package com.u1145h.books.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * A book stored in the offline library.
 */
@Entity(
    tableName = "books",
    indices = [
        Index(value = ["chapterId"]),
        Index(value = ["seriesId"]),
        Index(value = ["isDeleted", "progressDirty"]),
    ],
)
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int? = null,
    val seriesId: Int? = null,
    val volumeId: Int? = null,
    val libraryId: Int? = null,
    val title: String,
    val author: String = "",
    val series: String = "",
    /** One of [com.u1145h.books.domain.model.BookFormat].name */
    val format: String,
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
    /** True while local progress has not yet been pushed to the server. */
    val progressDirty: Boolean = false,
    /** Server `lastModifiedUtc` of the last progress push, for conflict checks. */
    val serverProgressLastModifiedUtc: Long? = null,
    val isDeleted: Boolean = false,
)

/**
 * A completed reading session, used to track total time spent reading.
 */
@Entity(tableName = "reading_sessions")
data class ReadingSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val startedAtUtc: Long,
    val endedAtUtc: Long? = null,
    val durationMs: Long = 0,
)

/**
 * A saved reading position bookmark.
 */
@Entity(tableName = "bookmarks")
data class BookmarkEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: Long,
    val page: Int,
    /** Reader-specific location token (EPUB CFI, etc.). */
    val location: String? = null,
    val note: String = "",
    val createdAtUtc: Long = System.currentTimeMillis(),
)

/**
 * A download enqueued through Android DownloadManager, awaiting import.
 */
@Entity(tableName = "download_queue")
data class DownloadQueueEntity(
    @PrimaryKey val downloadId: Long,
    val chapterId: Int? = null,
    val volumeId: Int? = null,
    val seriesId: Int? = null,
    val libraryId: Int? = null,
    val title: String = "",
    val filename: String? = null,
    val status: String = "pending",
    val createdAtUtc: Long = System.currentTimeMillis(),
)
