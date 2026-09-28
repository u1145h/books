package com.u1145h.books.feature.reader

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.remote.dto.ProgressDto
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.domain.model.Chapter
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject

enum class ReaderFormat {
    IMAGE, // Manga, Comic, CBZ, CBR
    EPUB,  // EPUB Book
    PDF,   // PDF Document
}

data class ReaderUiState(
    val isLoading: Boolean = true,
    val chapterId: Int = 0,
    val seriesId: Int = 0,
    val libraryId: Int = 0,
    val volumeId: Int = 0,
    val format: ReaderFormat = ReaderFormat.IMAGE,
    val totalPages: Int = 0,
    val currentPage: Int = 0,
    val initialScrollY: Int = 0,
    val currentScrollId: String? = null,
    val currentPageHtml: String? = null,
    val isPageLoading: Boolean = false,
    val title: String = "",
    val chapterNumber: String = "",
    val showControls: Boolean = true,
    val isRtl: Boolean = false,
    val isWebtoon: Boolean = false,
    val error: String? = null,
    // Chapter list & navigation
    val chapters: List<Chapter> = emptyList(),
    val currentChapterIndex: Int = -1,
    val prevChapter: Chapter? = null,
    val nextChapter: Chapter? = null,
    val showChapterList: Boolean = false,
    val showChapterCompletionDialog: Boolean = false,
)

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val api: KavitaApiService,
    private val kavitaRepository: KavitaRepository,
    private val libraryRepository: LibraryRepository,
    private val settingsRepository: SettingsRepository,
    val sessionManager: SessionManager,
    val okHttpClient: OkHttpClient,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(ReaderUiState())
    val state: StateFlow<ReaderUiState> = _state.asStateFlow()

    val serverUrl: String get() = settingsRepository.currentServerUrl

    private val ioScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var controlsJob: Job? = null
    private var progressDebounceJob: Job? = null
    private var pageFetchJob: Job? = null
    private val epubCache = ConcurrentHashMap<Int, String>()

    init {
        val chapterId = savedStateHandle.get<Int>("chapterId") ?: 0
        val seriesId = savedStateHandle.get<Int>("seriesId") ?: 0
        _state.update { it.copy(chapterId = chapterId, seriesId = seriesId) }
        viewModelScope.launch {
            val appSettings = settingsRepository.settings.first()
            _state.update {
                it.copy(
                    isRtl = appSettings.readerRtl,
                    isWebtoon = appSettings.readerWebtoon,
                )
            }
        }
        loadChapter(chapterId)
    }

    fun loadChapter(chapterId: Int) {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, chapterId = chapterId, showChapterCompletionDialog = false) }
            epubCache.clear()

            val info = runCatching { api.getChapterInfo(chapterId).body() }.getOrNull()
            val progress = runCatching { api.getProgress(chapterId).body() }.getOrNull()

            val seriesId = info?.seriesId.takeIf { it != null && it > 0 } ?: _state.value.seriesId
            var allChapters = _state.value.chapters

            // Fetch volumes/chapters if empty or not yet loaded
            if (allChapters.isEmpty() && seriesId > 0) {
                val volumes = libraryRepository.getVolumes(seriesId).getOrDefault(emptyList())
                allChapters = volumes.flatMap { it.chapters }
            }

            val curIndex = allChapters.indexOfFirst { it.id == chapterId }
            val prev = if (curIndex > 0) allChapters[curIndex - 1] else null
            val next = if (curIndex in 0 until allChapters.size - 1) allChapters[curIndex + 1] else null

            val rawFormat = info?.seriesFormat ?: 0
            val isEpub = rawFormat == 3 || info?.fileName?.endsWith(".epub", ignoreCase = true) == true
            val isPdf = rawFormat == 4 || info?.fileName?.endsWith(".pdf", ignoreCase = true) == true
            val format = when {
                isEpub -> ReaderFormat.EPUB
                isPdf -> ReaderFormat.PDF
                else -> ReaderFormat.IMAGE
            }

            val totalPages = info?.pages ?: 0
            val savedPage = progress?.pageNum ?: 0
            val initialPage = if (totalPages > 0 && savedPage >= totalPages) 0 else savedPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))
            val savedScrollId = progress?.bookScrollId
            val initialScroll = savedScrollId?.removePrefix("scroll:")?.toIntOrNull() ?: 0

            _state.update {
                it.copy(
                    isLoading = false,
                    chapterId = chapterId,
                    seriesId = seriesId,
                    format = format,
                    totalPages = totalPages,
                    currentPage = initialPage,
                    initialScrollY = initialScroll,
                    currentScrollId = savedScrollId,
                    isRtl = if (format == ReaderFormat.EPUB) false else it.isRtl,
                    title = info?.title?.ifBlank { null }
                        ?: info?.chapterTitle?.ifBlank { null }
                        ?: if (!info?.chapterNumber.isNullOrBlank()) "Chapter ${info.chapterNumber}"
                        else info?.seriesName ?: "Chapter",
                    chapterNumber = info?.chapterNumber ?: "",
                    volumeId = info?.volumeId ?: 0,
                    libraryId = info?.libraryId ?: 0,
                    chapters = allChapters,
                    currentChapterIndex = curIndex,
                    prevChapter = prev,
                    nextChapter = next,
                    error = if (info == null) "Failed to load chapter" else null,
                )
            }

            if (format == ReaderFormat.EPUB) {
                loadEpubPage(initialPage)
            }

            autoHideControls()
        }
    }

    fun loadEpubPage(page: Int) {
        val chId = _state.value.chapterId
        if (chId <= 0) return

        val cached = epubCache[page]
        if (cached != null) {
            _state.update { it.copy(currentPageHtml = cached, isPageLoading = false) }
            prefetchEpubPage(chId, page + 1)
            prefetchEpubPage(chId, page - 1)
            return
        }

        _state.update { it.copy(isPageLoading = true) }
        pageFetchJob?.cancel()
        pageFetchJob = viewModelScope.launch {
            kavitaRepository.getBookPage(chId, page).fold(
                onSuccess = { html ->
                    epubCache[page] = html
                    _state.update { it.copy(currentPageHtml = html, isPageLoading = false) }
                    prefetchEpubPage(chId, page + 1)
                    prefetchEpubPage(chId, page - 1)
                },
                onFailure = { err ->
                    _state.update { it.copy(isPageLoading = false, error = err.message) }
                },
            )
        }
    }

    private fun prefetchEpubPage(chapterId: Int, page: Int) {
        val total = _state.value.totalPages
        if (page < 0 || (total > 0 && page >= total)) return
        if (epubCache.containsKey(page)) return
        viewModelScope.launch {
            kavitaRepository.getBookPage(chapterId, page).onSuccess { html ->
                epubCache[page] = html
            }
        }
    }

    fun goToPage(page: Int) {
        val total = _state.value.totalPages
        val clamped = page.coerceIn(0, (total - 1).coerceAtLeast(0))
        _state.update { it.copy(currentPage = clamped, showChapterCompletionDialog = false, currentScrollId = null) }
        if (_state.value.format == ReaderFormat.EPUB) {
            loadEpubPage(clamped)
        }
        debouncedSaveProgress(clamped, null)
    }

    fun onScrollPositionChanged(scrollY: Int) {
        val scrollId = "scroll:$scrollY"
        _state.update { it.copy(currentScrollId = scrollId) }
        debouncedSaveProgress(_state.value.currentPage, scrollId)
    }

    fun updatePageSilently(page: Int) {
        val total = _state.value.totalPages
        val clamped = page.coerceIn(0, (total - 1).coerceAtLeast(0))
        if (clamped == _state.value.currentPage) return
        _state.update { it.copy(currentPage = clamped) }
        debouncedSaveProgress(clamped, _state.value.currentScrollId)
    }

    fun nextPage() {
        val cur = _state.value.currentPage
        val total = _state.value.totalPages
        if (cur < total - 1) {
            goToPage(cur + 1)
        } else if (total > 0 && cur == total - 1) {
            _state.update { it.copy(showChapterCompletionDialog = true, showControls = true) }
        }
    }

    fun prevPage() {
        val cur = _state.value.currentPage
        if (cur > 0) {
            goToPage(cur - 1)
        }
    }

    fun dismissCompletionDialog() {
        _state.update { it.copy(showChapterCompletionDialog = false) }
    }

    fun confirmCompleteAndNext() {
        markCurrentChapterRead()
        dismissCompletionDialog()
        val next = _state.value.nextChapter
        if (next != null) {
            loadChapter(next.id)
        }
    }

    fun goToNextChapter() {
        val next = _state.value.nextChapter ?: return
        markCurrentChapterRead()
        loadChapter(next.id)
    }

    fun goToPrevChapter() {
        val prev = _state.value.prevChapter ?: return
        loadChapter(prev.id)
    }

    fun selectChapter(chapterId: Int) {
        _state.update { it.copy(showChapterList = false) }
        if (chapterId != _state.value.chapterId) {
            loadChapter(chapterId)
        }
    }

    fun toggleChapterList() {
        _state.update { it.copy(showChapterList = !it.showChapterList) }
    }

    fun dismissChapterList() {
        _state.update { it.copy(showChapterList = false) }
    }

    fun toggleControls() {
        _state.update { it.copy(showControls = !it.showControls) }
        if (_state.value.showControls) autoHideControls()
    }

    fun toggleRtl() {
        if (_state.value.format == ReaderFormat.EPUB) return
        _state.update { it.copy(isRtl = !it.isRtl) }
    }

    fun toggleWebtoon() {
        if (_state.value.format == ReaderFormat.EPUB) return
        _state.update { it.copy(isWebtoon = !it.isWebtoon) }
    }

    fun markCurrentChapterRead() {
        val s = _state.value
        if (s.chapterId > 0 && s.totalPages > 0) {
            viewModelScope.launch {
                runCatching {
                    kavitaRepository.saveProgress(
                        ProgressDto(
                            chapterId = s.chapterId,
                            pageNum = s.totalPages,
                            seriesId = s.seriesId,
                            volumeId = s.volumeId,
                            libraryId = s.libraryId,
                        )
                    )
                }
                runCatching {
                    kavitaRepository.markChapterRead(s.seriesId, s.chapterId)
                }
            }
        }
    }

    private fun autoHideControls() {
        controlsJob?.cancel()
        controlsJob = viewModelScope.launch {
            delay(3_000)
            _state.update { it.copy(showControls = false) }
        }
    }

    private fun debouncedSaveProgress(page: Int, scrollId: String? = _state.value.currentScrollId) {
        progressDebounceJob?.cancel()
        progressDebounceJob = viewModelScope.launch {
            delay(800)
            saveProgressImmediately(page, scrollId)
        }
    }

    fun saveProgressImmediately(
        page: Int = _state.value.currentPage,
        scrollId: String? = _state.value.currentScrollId,
    ) {
        val s = _state.value
        if (s.chapterId <= 0) return
        progressDebounceJob?.cancel()
        ioScope.launch {
            runCatching {
                kavitaRepository.saveProgress(
                    ProgressDto(
                        chapterId = s.chapterId,
                        pageNum = page,
                        seriesId = s.seriesId,
                        volumeId = s.volumeId,
                        libraryId = s.libraryId,
                        bookScrollId = scrollId,
                    )
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        saveProgressImmediately()
    }

    /** Build the URL for a given page image. */
    fun pageImageUrl(page: Int): String {
        val apiKey = sessionManager.apiKey
        val authSuffix = if (!apiKey.isNullOrBlank()) "&apiKey=$apiKey" else ""
        return "$serverUrl/api/Reader/image?chapterId=${_state.value.chapterId}&page=$page&extractPdf=true$authSuffix"
    }
}
