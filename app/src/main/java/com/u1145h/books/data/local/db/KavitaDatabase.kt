package com.u1145h.books.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import com.u1145h.books.data.local.db.dao.BookDao
import com.u1145h.books.data.local.db.dao.BookmarkDao
import com.u1145h.books.data.local.db.dao.DownloadQueueDao
import com.u1145h.books.data.local.db.dao.ReadingSessionDao
import com.u1145h.books.data.local.db.entity.BookEntity
import com.u1145h.books.data.local.db.entity.BookmarkEntity
import com.u1145h.books.data.local.db.entity.DownloadQueueEntity
import com.u1145h.books.data.local.db.entity.ReadingSessionEntity

/**
 * Local database for the offline library.
 *
 * When the schema changes, bump [VERSION] and add a [Migration] to
 * [MIGRATIONS] (see `app/schemas` for generated schema dumps).
 */
@Database(
    entities = [
        BookEntity::class,
        ReadingSessionEntity::class,
        BookmarkEntity::class,
        DownloadQueueEntity::class,
    ],
    version = 1,
    exportSchema = true,
)
abstract class KavitaDatabase : RoomDatabase() {

    abstract fun bookDao(): BookDao
    abstract fun readingSessionDao(): ReadingSessionDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun downloadQueueDao(): DownloadQueueDao

    companion object {
        const val NAME = "kavita.db"

        /** Ordered list of migrations; append new migrations here. */
        val MIGRATIONS: Array<Migration> = arrayOf()
    }
}
