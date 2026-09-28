package com.u1145h.books.di

import androidx.room.Room
import android.content.Context
import com.u1145h.books.data.local.db.KavitaDatabase
import com.u1145h.books.data.local.db.dao.BookDao
import com.u1145h.books.data.local.db.dao.BookmarkDao
import com.u1145h.books.data.local.db.dao.DownloadQueueDao
import com.u1145h.books.data.local.db.dao.ReadingSessionDao
import com.u1145h.books.data.repository.BookRepository
import com.u1145h.books.data.repository.BookRepositoryImpl
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.KavitaRepositoryImpl
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.LibraryRepositoryImpl
import com.u1145h.books.data.repository.SearchRepository
import com.u1145h.books.data.repository.SearchRepositoryImpl
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): KavitaDatabase =
        Room.databaseBuilder(context, KavitaDatabase::class.java, KavitaDatabase.NAME)
            .addMigrations(*KavitaDatabase.MIGRATIONS)
            .build()

    @Provides fun provideBookDao(db: KavitaDatabase): BookDao = db.bookDao()
    @Provides fun provideReadingSessionDao(db: KavitaDatabase): ReadingSessionDao = db.readingSessionDao()
    @Provides fun provideBookmarkDao(db: KavitaDatabase): BookmarkDao = db.bookmarkDao()
    @Provides fun provideDownloadQueueDao(db: KavitaDatabase): DownloadQueueDao = db.downloadQueueDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds @Singleton
    abstract fun bindBookRepository(impl: BookRepositoryImpl): BookRepository

    @Binds @Singleton
    abstract fun bindKavitaRepository(impl: KavitaRepositoryImpl): KavitaRepository

    @Binds @Singleton
    abstract fun bindLibraryRepository(impl: LibraryRepositoryImpl): LibraryRepository

    @Binds @Singleton
    abstract fun bindSearchRepository(impl: SearchRepositoryImpl): SearchRepository
}
