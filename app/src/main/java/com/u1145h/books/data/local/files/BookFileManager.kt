package com.u1145h.books.data.local.files

import android.content.Context
import android.os.Environment
import com.u1145h.books.core.util.shortHash
import com.u1145h.books.domain.model.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages application-owned storage for downloaded books, covers and caches.
 * Books and covers live in app-internal storage (no storage permission needed).
 */
@Singleton
class BookFileManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    val booksDir: File
        get() = File(context.filesDir, "books").apply { mkdirs() }

    val coversDir: File
        get() = File(context.filesDir, "covers").apply { mkdirs() }

    /** Public-ish downloads folder where DownloadManager writes files first. */
    val downloadsDir: File
        get() = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS) ?: context.filesDir,
            "downloads",
        ).apply { mkdirs() }

    /** Returns a fresh, collision-safe file for a book in the library. */
    fun bookFile(format: BookFormat, sourceName: String): File {
        val stamp = System.currentTimeMillis()
        return File(booksDir, "$stamp-${sourceName.shortHash()}.${format.extension}")
    }

    fun coverFile(seriesId: Int): File = File(coversDir, "series-$seriesId.jpg")

    fun folderSize(dir: File): Long =
        dir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun totalLibraryBytes(): Long = folderSize(booksDir) + folderSize(coversDir)

    fun deleteTree(dir: File) {
        if (dir.exists()) dir.deleteRecursively()
    }

    fun deleteFile(path: String?) {
        path?.let { runCatching { File(it).delete() } }
    }
}
