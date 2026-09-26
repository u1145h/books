package com.u1145h.kavitaandroid.data.remote.sync

import android.util.Log
import com.u1145h.kavitaandroid.core.util.NetworkMonitor
import com.u1145h.kavitaandroid.data.local.db.dao.BookDao
import com.u1145h.kavitaandroid.data.remote.api.KavitaApiService
import com.u1145h.kavitaandroid.data.remote.auth.SessionManager
import com.u1145h.kavitaandroid.data.remote.dto.ProgressDto
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * Monitors network availability and automatically synchronizes offline reading
 * progress with the Kavita server whenever connection is restored.
 */
@Singleton
class ProgressSyncManager @Inject constructor(
    private val networkMonitor: NetworkMonitor,
    private val bookDao: BookDao,
    private val api: KavitaApiService,
    private val sessionManager: SessionManager,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startSyncLoop() {
        scope.launch {
            networkMonitor.isOnline.collect { isOnline ->
                if (isOnline && sessionManager.token != null) {
                    syncDirtyProgress()
                }
            }
        }
    }

    suspend fun syncDirtyProgress() {
        runCatching {
            val dirtyBooks = bookDao.getDirty()
            if (dirtyBooks.isEmpty()) return

            Log.d("ProgressSyncManager", "Found ${dirtyBooks.size} offline progress items to sync")
            dirtyBooks.forEach { book ->
                val chapterId = book.chapterId ?: return@forEach
                val dto = ProgressDto(
                    chapterId = chapterId,
                    pageNum = book.currentPage,
                    seriesId = book.seriesId ?: 0,
                    volumeId = book.volumeId ?: 0,
                    libraryId = book.libraryId ?: 0,
                )

                val resp = api.saveProgress(dto)
                if (resp.isSuccessful) {
                    bookDao.markSynced(book.id, System.currentTimeMillis())
                    Log.d("ProgressSyncManager", "Successfully synced progress for chapter $chapterId (page ${book.currentPage})")
                } else {
                    Log.w("ProgressSyncManager", "Failed to sync progress for chapter $chapterId: HTTP ${resp.code()}")
                }
            }
        }.onFailure {
            Log.e("ProgressSyncManager", "Error during progress sync", it)
        }
    }
}
