package com.u1145h.kavitaandroid.feature.home

import android.util.Log
import android.webkit.WebView
import com.u1145h.kavitaandroid.data.local.db.dao.BookDao
import com.u1145h.kavitaandroid.data.local.datastore.Session
import com.u1145h.kavitaandroid.data.local.files.OfflineDownloadManager
import com.u1145h.kavitaandroid.data.remote.auth.SessionManager
import com.u1145h.kavitaandroid.data.remote.sync.ProgressSyncManager
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Native side of the WebView bridge (`window.KavitaAndroid`). Receives the
 * Kavita auth token captured from the web UI, tracks body color, and bridges
 * offline downloading & progress syncing.
 */
@Singleton
class KavitaBridge @Inject constructor(
    private val sessionManager: SessionManager,
    private val offlineDownloadManager: OfflineDownloadManager,
    private val progressSyncManager: ProgressSyncManager,
    private val bookDao: BookDao,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    init {
        progressSyncManager.startSyncLoop()
    }

    private val _bodyColor = MutableStateFlow(DEFAULT_BODY_COLOR)
    val bodyColor: StateFlow<Long> = _bodyColor

    private var activeWebView: WebView? = null

    fun attachWebView(webView: WebView) {
        activeWebView = webView
        scope.launch {
            offlineDownloadManager.statusEvents.collect { event ->
                val script = "if (window.onOfflineStatusChanged) { window.onOfflineStatusChanged(${event.chapterId}, '${event.status}'); }"
                activeWebView?.post {
                    activeWebView?.evaluateJavascript(script, null)
                }
            }
        }
    }

    fun getOfflineResource(url: android.net.Uri): android.webkit.WebResourceResponse? {
        val path = url.path ?: ""
        if (path.contains("/api/Reader/chapter-info")) {
            val chapterId = url.getQueryParameter("chapterId")?.toIntOrNull() ?: 0
            val book = runBlocking { bookDao.getByChapterId(chapterId) }
            if (book != null) {
                val json = """
                    {
                        "chapterNumber": "${book.chapterId}",
                        "volumeId": ${book.volumeId ?: 0},
                        "seriesName": "${book.series}",
                        "seriesId": ${book.seriesId ?: 0},
                        "libraryId": ${book.libraryId ?: 0},
                        "chapterTitle": "${book.title}",
                        "pages": ${book.totalPages},
                        "title": "${book.title}"
                    }
                """.trimIndent()
                return android.webkit.WebResourceResponse("application/json", "UTF-8", json.byteInputStream())
            }
        }

        if (path.contains("/api/Reader/get-progress")) {
            val chapterId = url.getQueryParameter("chapterId")?.toIntOrNull() ?: 0
            val book = runBlocking { bookDao.getByChapterId(chapterId) }
            if (book != null) {
                val json = """
                    {
                        "chapterId": ${book.chapterId},
                        "pageNum": ${book.currentPage},
                        "seriesId": ${book.seriesId ?: 0},
                        "volumeId": ${book.volumeId ?: 0},
                        "libraryId": ${book.libraryId ?: 0}
                    }
                """.trimIndent()
                return android.webkit.WebResourceResponse("application/json", "UTF-8", json.byteInputStream())
            }
        }
        return null
    }

    private val _isManualOffline = MutableStateFlow(false)
    val isManualOffline: StateFlow<Boolean> = _isManualOffline

    private val _showAppSettings = MutableStateFlow(false)
    val showAppSettings: StateFlow<Boolean> = _showAppSettings

    fun setManualOffline(offline: Boolean) {
        _isManualOffline.value = offline
    }

    fun setShowAppSettings(show: Boolean) {
        _showAppSettings.value = show
    }

    @android.webkit.JavascriptInterface
    fun openOfflineMode() {
        _isManualOffline.value = true
    }

    @android.webkit.JavascriptInterface
    fun openAppSettings() {
        _showAppSettings.value = true
    }

    @android.webkit.JavascriptInterface
    fun getToken(): String = sessionManager.token ?: ""

    @android.webkit.JavascriptInterface
    fun getApiKey(): String = sessionManager.apiKey ?: ""

    @android.webkit.JavascriptInterface
    fun getUsername(): String = sessionManager.session.value.username ?: ""

    @android.webkit.JavascriptInterface
    fun saveOfflineProgress(chapterId: Int, pageNum: Int, totalPages: Int) {
        scope.launch {
            val existing = bookDao.getByChapterId(chapterId)
            if (existing != null) {
                val percent = if (totalPages > 0) (pageNum.toFloat() / totalPages.toFloat()) * 100f else 0f
                bookDao.updateProgress(existing.id, pageNum, percent, System.currentTimeMillis())
                progressSyncManager.syncDirtyProgress()
            }
        }
    }

    @android.webkit.JavascriptInterface
    fun isChapterDownloaded(chapterId: Int): Boolean {
        return runBlocking { bookDao.getByChapterId(chapterId) != null }
    }

    @android.webkit.JavascriptInterface
    fun downloadOffline(payloadJson: String) {
        offlineDownloadManager.enqueueDownload(payloadJson)
    }

    @android.webkit.JavascriptInterface
    fun removeOffline(chapterId: Int) {
        scope.launch { offlineDownloadManager.removeOffline(chapterId) }
    }

    @android.webkit.JavascriptInterface
    fun onSession(json: String) {
        scope.launch {
            runCatching {
                val obj = Json.parseToJsonElement(json).jsonObject
                val token = obj["token"]?.jsonPrimitive?.contentOrNull
                val username = obj["username"]?.jsonPrimitive?.contentOrNull
                if (!token.isNullOrBlank()) {
                    sessionManager.update(
                        Session(
                            token = token,
                            apiKey = sessionManager.apiKey,
                            username = username ?: sessionManager.session.value.username,
                        ),
                    )
                }
            }.onFailure { Log.w("KavitaBridge", "Bad session payload", it) }
        }
    }

    @android.webkit.JavascriptInterface
    fun onBodyColor(color: String) {
        parseCssColor(color)?.let { _bodyColor.value = it }
    }

    @android.webkit.JavascriptInterface
    fun log(message: String) {
        Log.d("KavitaBridge", message)
    }

    private fun parseCssColor(color: String): Long? {
        val match = Regex("""rgba?\(\s*(\d+)\s*,\s*(\d+)\s*,\s*(\d+)\s*(?:,\s*([\d.]+)\s*)?\)""")
            .find(color.trim())
            ?: return null
        val channels = match.groupValues.drop(1).take(3)
            .map { it.toIntOrNull()?.coerceIn(0, 255) }
        val r = channels[0] ?: return null
        val g = channels[1] ?: return null
        val b = channels[2] ?: return null
        val alpha = match.groupValues.getOrNull(4)?.toFloatOrNull()?.coerceIn(0f, 1f) ?: 1f
        val a = (alpha * 255).toInt()
        return (a.toLong() shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    companion object {
        /** Kavita's dark-mode body background, used until the page reports its color. */
        private const val DEFAULT_BODY_COLOR = 0xFF0E0E0E
    }
}
