package com.u1145h.books.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.u1145h.books.data.local.db.dao.BookDao
import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.dto.ProgressDto
import com.u1145h.books.data.repository.SettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * Periodic WorkManager job that pushes locally-dirty reading progress
 * records to the Kavita server and updates the last-sync timestamp.
 *
 * Runs every 15 minutes when the device has any network connection.
 * Retries with exponential backoff on failure.
 */
@HiltWorker
class ProgressSyncWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val api: KavitaApiService,
    private val bookDao: BookDao,
    private val settingsRepository: SettingsRepository,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        return try {
            val dirty = bookDao.getDirty()
            var failed = 0

            for (book in dirty) {
                if (book.chapterId == null) continue
                val result = runCatching {
                    api.saveProgress(
                        ProgressDto(
                            chapterId = book.chapterId,
                            pageNum = book.currentPage,
                            seriesId = book.seriesId ?: 0,
                            volumeId = book.volumeId ?: 0,
                            libraryId = book.libraryId ?: 0,
                        )
                    )
                }
                if (result.isSuccess) {
                    bookDao.markSynced(book.id, System.currentTimeMillis())
                } else {
                    failed++
                }
            }

            settingsRepository.setLastSyncAt(System.currentTimeMillis())

            if (failed > 0) Result.retry() else Result.success()
        } catch (e: Exception) {
            Result.retry()
        }
    }

    companion object {
        private const val WORK_NAME = "kavita_progress_sync"

        /** Enqueue the periodic sync; replaces any existing schedule. */
        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<ProgressSyncWorker>(
                repeatInterval = 15,
                repeatIntervalTimeUnit = TimeUnit.MINUTES,
            )
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    30,
                    TimeUnit.SECONDS,
                )
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request,
            )
        }

        /** Cancel the periodic sync (e.g. on logout). */
        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }

        /** Trigger a one-time immediate sync. */
        fun syncNow(context: Context) {
            val request = androidx.work.OneTimeWorkRequestBuilder<ProgressSyncWorker>()
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .build()
                )
                .build()
            WorkManager.getInstance(context).enqueue(request)
        }
    }
}
