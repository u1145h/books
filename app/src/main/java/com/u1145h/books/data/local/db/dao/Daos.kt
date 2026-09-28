package com.u1145h.books.data.local.db.dao

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.u1145h.books.data.local.db.entity.BookEntity
import com.u1145h.books.data.local.db.entity.BookmarkEntity
import com.u1145h.books.data.local.db.entity.DownloadQueueEntity
import com.u1145h.books.data.local.db.entity.ReadingSessionEntity
import kotlinx.coroutines.flow.Flow

data class StorageStats(
    @ColumnInfo(name = "bookCount") val bookCount: Int,
    @ColumnInfo(name = "bytes") val totalBytes: Long,
)

@Dao
interface BookDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(book: BookEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(books: List<BookEntity>)

    @Query("SELECT * FROM books WHERE isDeleted = 0 ORDER BY downloadDateUtc DESC")
    fun observeAll(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDeleted = 0")
    suspend fun getAll(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isDeleted = 0 AND id = :id")
    fun observeById(id: Long): Flow<BookEntity?>

    @Query("SELECT * FROM books WHERE isDeleted = 0 AND id = :id")
    suspend fun getById(id: Long): BookEntity?

    @Query("SELECT * FROM books WHERE isDeleted = 0 AND chapterId = :chapterId LIMIT 1")
    suspend fun getByChapterId(chapterId: Int): BookEntity?

    @Query("SELECT chapterId FROM books WHERE isDeleted = 0 AND seriesId = :seriesId AND chapterId IS NOT NULL")
    fun observeDownloadedChapterIds(seriesId: Int): Flow<List<Int>>

    @Query("SELECT chapterId FROM books WHERE isDeleted = 0 AND chapterId IS NOT NULL")
    fun observeAllDownloadedChapterIds(): Flow<List<Int>>

    @Query(
        "SELECT * FROM books WHERE isDeleted = 0 AND progressPercent > 0 AND progressPercent < 100 " +
            "ORDER BY lastOpenedUtc DESC",
    )
    fun observeContinueReading(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDeleted = 0 ORDER BY downloadDateUtc DESC")
    fun observeRecentlyAdded(): Flow<List<BookEntity>>

    @Query("SELECT * FROM books WHERE isDeleted = 0 ORDER BY lastOpenedUtc DESC")
    fun observeRecentlyRead(): Flow<List<BookEntity>>

    @Query(
        "SELECT COUNT(*) AS bookCount, COALESCE(SUM(fileSizeBytes), 0) AS bytes " +
            "FROM books WHERE isDeleted = 0",
    )
    fun observeStats(): Flow<StorageStats>

    @Query("UPDATE books SET currentPage = :page, progressPercent = :percent, lastModifiedUtc = :modified, progressDirty = 1 WHERE id = :id")
    suspend fun updateProgress(id: Long, page: Int, percent: Float, modified: Long)

    @Query("UPDATE books SET timeSpentReadingMs = timeSpentReadingMs + :ms WHERE id = :id")
    suspend fun addReadingTime(id: Long, ms: Long)

    @Query("UPDATE books SET lastOpenedUtc = :ts WHERE id = :id")
    suspend fun markOpened(id: Long, ts: Long)

    @Query("UPDATE books SET serverProgressLastModifiedUtc = :ts, progressDirty = 0 WHERE id = :id")
    suspend fun markSynced(id: Long, ts: Long?)

    @Query("UPDATE books SET isDeleted = 1 WHERE id = :id")
    suspend fun markDeleted(id: Long)

    @Query("SELECT * FROM books WHERE isDeleted = 0 AND progressDirty = 1")
    suspend fun getDirty(): List<BookEntity>

    @Query("SELECT * FROM books WHERE isDeleted = 0 AND coverPath IS NULL")
    suspend fun getMissingCovers(): List<BookEntity>
}

@Dao
interface ReadingSessionDao {

    @Insert
    suspend fun insert(session: ReadingSessionEntity): Long

    @Query("UPDATE reading_sessions SET endedAtUtc = :end, durationMs = :ms WHERE id = :id")
    suspend fun close(id: Long, end: Long, ms: Long)

    @Query("SELECT COALESCE(SUM(durationMs), 0) FROM reading_sessions WHERE bookId = :bookId")
    suspend fun totalDuration(bookId: Long): Long
}

@Dao
interface BookmarkDao {

    @Insert
    suspend fun insert(bookmark: BookmarkEntity): Long

    @Delete
    suspend fun delete(bookmark: BookmarkEntity)

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    suspend fun forBook(bookId: Long): List<BookmarkEntity>

    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY page ASC")
    fun observeForBook(bookId: Long): Flow<List<BookmarkEntity>>
}

@Dao
interface DownloadQueueDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(item: DownloadQueueEntity)

    @Query("SELECT * FROM download_queue WHERE downloadId = :id")
    suspend fun get(id: Long): DownloadQueueEntity?

    @Query("DELETE FROM download_queue WHERE downloadId = :id")
    suspend fun remove(id: Long)

    @Query("SELECT * FROM download_queue WHERE status = 'pending'")
    suspend fun pending(): List<DownloadQueueEntity>
}
