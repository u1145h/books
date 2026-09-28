package com.u1145h.books.data.repository

import com.u1145h.books.data.local.db.dao.StorageStats
import com.u1145h.books.data.local.files.BookFileManager
import com.u1145h.books.data.local.db.dao.BookDao
import com.u1145h.books.domain.model.Book
import com.u1145h.books.domain.model.BookMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

interface BookRepository {
    fun observeBooks(): Flow<List<Book>>
    fun observeBook(id: Long): Flow<Book?>
    fun observeContinueReading(): Flow<List<Book>>
    fun observeRecentlyAdded(): Flow<List<Book>>
    fun observeRecentlyRead(): Flow<List<Book>>
    fun observeStats(): Flow<StorageStats>
    suspend fun getBook(id: Long): Book?
    suspend fun getByChapterId(chapterId: Int): Book?
    suspend fun saveBook(book: Book): Long
    suspend fun updateProgress(id: Long, page: Int, percent: Float)
    suspend fun addReadingTime(id: Long, ms: Long)
    suspend fun markOpened(id: Long)
    suspend fun getDirtyBooks(): List<Book>
    suspend fun getMissingCovers(): List<Book>
    suspend fun markSynced(id: Long, serverLastModified: Long?)
    suspend fun deleteBook(id: Long): Result<Unit>
    suspend fun clearAll(): Result<Unit>
}

@Singleton
class BookRepositoryImpl @Inject constructor(
    private val bookDao: BookDao,
    private val fileManager: BookFileManager,
) : BookRepository {

    override fun observeBooks(): Flow<List<Book>> =
        bookDao.observeAll().map { list -> list.map(BookMapper::toDomain) }

    override fun observeBook(id: Long): Flow<Book?> =
        bookDao.observeById(id).map { it?.let(BookMapper::toDomain) }

    override fun observeContinueReading(): Flow<List<Book>> =
        bookDao.observeContinueReading().map { list -> list.map(BookMapper::toDomain) }

    override fun observeRecentlyAdded(): Flow<List<Book>> =
        bookDao.observeRecentlyAdded().map { list -> list.map(BookMapper::toDomain) }

    override fun observeRecentlyRead(): Flow<List<Book>> =
        bookDao.observeRecentlyRead().map { list -> list.map(BookMapper::toDomain) }

    override fun observeStats(): Flow<StorageStats> = bookDao.observeStats()

    override suspend fun getBook(id: Long): Book? = bookDao.getById(id)?.let(BookMapper::toDomain)

    override suspend fun getByChapterId(chapterId: Int): Book? =
        bookDao.getByChapterId(chapterId)?.let(BookMapper::toDomain)

    override suspend fun saveBook(book: Book): Long = bookDao.upsert(BookMapper.toEntity(book))

    override suspend fun updateProgress(id: Long, page: Int, percent: Float) =
        bookDao.updateProgress(id, page, percent.coerceIn(0f, 100f), System.currentTimeMillis())

    override suspend fun addReadingTime(id: Long, ms: Long) = bookDao.addReadingTime(id, ms)

    override suspend fun markOpened(id: Long) = bookDao.markOpened(id, System.currentTimeMillis())

    override suspend fun getDirtyBooks(): List<Book> = bookDao.getDirty().map(BookMapper::toDomain)

    override suspend fun getMissingCovers(): List<Book> =
        bookDao.getMissingCovers().map(BookMapper::toDomain)

    override suspend fun markSynced(id: Long, serverLastModified: Long?) =
        bookDao.markSynced(id, serverLastModified)

    override suspend fun deleteBook(id: Long): Result<Unit> = runCatching {
        val book = bookDao.getById(id) ?: return Result.success(Unit)
        fileManager.deleteFile(book.filePath)
        fileManager.deleteFile(book.coverPath)
        bookDao.markDeleted(id)
    }

    override suspend fun clearAll(): Result<Unit> = runCatching {
        val all = bookDao.getAll()
        all.forEach { book ->
            fileManager.deleteFile(book.filePath)
            fileManager.deleteFile(book.coverPath)
            bookDao.markDeleted(book.id)
        }
    }
}
