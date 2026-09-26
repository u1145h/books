package com.u1145h.kavitaandroid.data.local.files

import android.content.Context
import android.util.Log
import com.u1145h.kavitaandroid.data.local.db.dao.BookDao
import com.u1145h.kavitaandroid.data.local.db.dao.DownloadQueueDao
import com.u1145h.kavitaandroid.data.local.db.entity.BookEntity
import com.u1145h.kavitaandroid.data.local.db.entity.DownloadQueueEntity
import com.u1145h.kavitaandroid.data.remote.auth.SessionManager
import com.u1145h.kavitaandroid.data.repository.SettingsRepository
import com.u1145h.kavitaandroid.domain.model.BookFormat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

data class DownloadStatusEvent(
    val chapterId: Int?,
    val seriesId: Int?,
    val status: String, // "downloading", "downloaded", "error", "deleted"
)

@Singleton
class OfflineDownloadManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val okHttpClient: OkHttpClient,
    private val bookFileManager: BookFileManager,
    private val bookDao: BookDao,
    private val downloadQueueDao: DownloadQueueDao,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val jsonParser = Json { ignoreUnknownKeys = true }

    private val _statusEvents = MutableSharedFlow<DownloadStatusEvent>(extraBufferCapacity = 64)
    val statusEvents: SharedFlow<DownloadStatusEvent> = _statusEvents.asSharedFlow()

    suspend fun isChapterDownloaded(chapterId: Int): Boolean {
        return bookDao.getByChapterId(chapterId) != null
    }

    fun enqueueDownload(jsonPayload: String) {
        scope.launch {
            runCatching {
                val json = jsonParser.parseToJsonElement(jsonPayload).jsonObject
                val chapterId = json["chapterId"]?.jsonPrimitive?.intOrNull
                val seriesId = json["seriesId"]?.jsonPrimitive?.intOrNull
                val volumeId = json["volumeId"]?.jsonPrimitive?.intOrNull
                val title = json["title"]?.jsonPrimitive?.content ?: "Downloaded Book"
                val seriesName = json["series"]?.jsonPrimitive?.content ?: ""
                val formatStr = json["format"]?.jsonPrimitive?.content ?: "EPUB"
                val relDownloadUrl = json["downloadUrl"]?.jsonPrimitive?.content

                val serverUrl = settingsRepository.currentServerUrl.trimEnd('/')
                val downloadPath = when {
                    !relDownloadUrl.isNullOrBlank() -> relDownloadUrl
                    chapterId != null -> "/api/Download/download-chapter?chapterId=$chapterId"
                    seriesId != null -> "/api/Download/download-series?seriesId=$seriesId"
                    volumeId != null -> "/api/Download/download-volume?volumeId=$volumeId"
                    else -> null
                } ?: return@launch

                val fullUrl = if (downloadPath.startsWith("http")) downloadPath else "$serverUrl$downloadPath"

                _statusEvents.emit(DownloadStatusEvent(chapterId, seriesId, "downloading"))

                val token = sessionManager.token
                val requestBuilder = Request.Builder().url(fullUrl)
                if (!token.isNullOrBlank()) {
                    requestBuilder.header("Authorization", "Bearer $token")
                }

                val response = okHttpClient.newCall(requestBuilder.build()).execute()
                if (!response.isSuccessful) {
                    Log.e("OfflineDownloadManager", "Download failed with HTTP ${response.code}")
                    _statusEvents.emit(DownloadStatusEvent(chapterId, seriesId, "error"))
                    return@launch
                }

                val body = response.body ?: run {
                    _statusEvents.emit(DownloadStatusEvent(chapterId, seriesId, "error"))
                    return@launch
                }

                val contentType = response.header("Content-Type") ?: ""
                if (contentType.contains("text/html") || contentType.contains("application/json")) {
                    Log.e("OfflineDownloadManager", "Download URL returned Content-Type $contentType instead of book file. Aborting.")
                    _statusEvents.emit(DownloadStatusEvent(chapterId, seriesId, "error"))
                    return@launch
                }

                val format = BookFormat.fromFileName("file.$formatStr") ?: BookFormat.EPUB
                val targetFile = bookFileManager.bookFile(format, title)
                
                body.byteStream().use { input ->
                    targetFile.outputStream().use { output ->
                        input.copyTo(output)
                    }
                }

                if (targetFile.length() < 1024) {
                    Log.e("OfflineDownloadManager", "Downloaded file size too small (${targetFile.length()} bytes). Likely invalid.")
                    targetFile.delete()
                    _statusEvents.emit(DownloadStatusEvent(chapterId, seriesId, "error"))
                    return@launch
                }

                val coverFile = if (seriesId != null) fetchCover(serverUrl, token, seriesId) else null

                // Pre-fetch Chapter Info Metadata
                var totalPages = 0
                var fetchedVolumeId = volumeId
                var fetchedSeriesId = seriesId
                var fetchedLibraryId = 0
                var fetchedTitle = title

                if (chapterId != null) {
                    runCatching {
                        val infoReqUrl = "$serverUrl/api/Reader/chapter-info?chapterId=$chapterId"
                        val req = Request.Builder().url(infoReqUrl)
                        if (!token.isNullOrBlank()) req.header("Authorization", "Bearer $token")
                        val resp = okHttpClient.newCall(req.build()).execute()
                        if (resp.isSuccessful) {
                            resp.body?.string()?.let { str ->
                                val obj = jsonParser.parseToJsonElement(str).jsonObject
                                totalPages = obj["pages"]?.jsonPrimitive?.intOrNull ?: 0
                                fetchedVolumeId = obj["volumeId"]?.jsonPrimitive?.intOrNull ?: volumeId
                                fetchedSeriesId = obj["seriesId"]?.jsonPrimitive?.intOrNull ?: seriesId
                                fetchedLibraryId = obj["libraryId"]?.jsonPrimitive?.intOrNull ?: 0
                                fetchedTitle = obj["chapterTitle"]?.jsonPrimitive?.content ?: obj["title"]?.jsonPrimitive?.content ?: title
                            }
                        }
                    }
                }

                val bookEntity = BookEntity(
                    chapterId = chapterId,
                    seriesId = fetchedSeriesId,
                    volumeId = fetchedVolumeId,
                    libraryId = fetchedLibraryId,
                    title = fetchedTitle,
                    series = seriesName,
                    format = format.name,
                    filePath = targetFile.absolutePath,
                    coverPath = coverFile?.absolutePath,
                    fileSizeBytes = targetFile.length(),
                    totalPages = totalPages,
                    downloadDateUtc = System.currentTimeMillis(),
                )

                bookDao.upsert(bookEntity)
                Log.d("OfflineDownloadManager", "Successfully downloaded: $fetchedTitle to ${targetFile.absolutePath}")
                _statusEvents.emit(DownloadStatusEvent(chapterId, fetchedSeriesId, "downloaded"))
            }.onFailure {
                Log.e("OfflineDownloadManager", "Error downloading book", it)
                scope.launch { _statusEvents.emit(DownloadStatusEvent(null, null, "error")) }
            }
        }
    }

    suspend fun removeOffline(chapterId: Int) {
        val existing = bookDao.getByChapterId(chapterId) ?: return
        bookFileManager.deleteFile(existing.filePath)
        bookFileManager.deleteFile(existing.coverPath)
        bookDao.markDeleted(existing.id)
        _statusEvents.emit(DownloadStatusEvent(chapterId, existing.seriesId, "deleted"))
    }

    private fun fetchCover(serverUrl: String, token: String?, seriesId: Int): File? {
        return runCatching {
            val coverUrl = "$serverUrl/api/Image/series-cover?seriesId=$seriesId"
            val req = Request.Builder().url(coverUrl)
            if (!token.isNullOrBlank()) req.header("Authorization", "Bearer $token")
            val resp = okHttpClient.newCall(req.build()).execute()
            if (resp.isSuccessful) {
                val file = bookFileManager.coverFile(seriesId)
                resp.body?.byteStream()?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                file
            } else null
        }.getOrNull()
    }
}
