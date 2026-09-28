package com.u1145h.books.feature.series

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.core.util.CoverUrlBuilder
import com.u1145h.books.data.local.files.OfflineDownloadManager
import com.u1145h.books.data.remote.abs.dto.AbsLibraryItemDto
import com.u1145h.books.data.repository.AudiobookshelfRepository
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.domain.manager.MediaMatchingManager
import com.u1145h.books.domain.model.Chapter
import com.u1145h.books.domain.model.SeriesDetail
import com.u1145h.books.feature.audio.AudioPlayerManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SeriesUiState(
    val isLoading: Boolean = true,
    val series: SeriesDetail? = null,
    val downloadedChapterIds: Set<Int> = emptySet(),
    val downloadingChapterIds: Set<Int> = emptySet(),
    val error: String? = null,
    val companionAudiobookId: String? = null,
    val companionAudiobook: AbsLibraryItemDto? = null,
)

@HiltViewModel
class SeriesDetailViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val absRepository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository,
    private val mediaMatchingManager: MediaMatchingManager,
    val audioPlayerManager: AudioPlayerManager,
    val coverUrlBuilder: CoverUrlBuilder,
    private val offlineDownloadManager: OfflineDownloadManager,
    savedStateHandle: SavedStateHandle,
) : ViewModel() {

    val seriesId: Int = savedStateHandle["seriesId"] ?: 0

    private val _state = MutableStateFlow(SeriesUiState())
    val state: StateFlow<SeriesUiState> = _state.asStateFlow()

    val serverUrl: String get() = settingsRepository.currentServerUrl

    init {
        load()
        observeOfflineStatus()
    }

    private fun observeOfflineStatus() {
        viewModelScope.launch {
            offlineDownloadManager.observeDownloadedChapterIds(seriesId).collect { ids ->
                _state.update { it.copy(downloadedChapterIds = ids.toSet()) }
            }
        }
        viewModelScope.launch {
            offlineDownloadManager.statusEvents.collect { event ->
                val chId = event.chapterId ?: return@collect
                when (event.status) {
                    "downloading" -> {
                        _state.update { it.copy(downloadingChapterIds = it.downloadingChapterIds + chId) }
                    }
                    "downloaded", "error", "deleted" -> {
                        _state.update { it.copy(downloadingChapterIds = it.downloadingChapterIds - chId) }
                    }
                }
            }
        }
    }

    fun downloadChapter(chapter: Chapter) {
        val seriesName = _state.value.series?.name ?: ""
        val chapterTitle = chapter.title?.ifBlank { null } ?: "Chapter ${chapter.number}"
        offlineDownloadManager.downloadChapter(
            chapterId = chapter.id,
            seriesId = seriesId,
            seriesName = seriesName,
            title = chapterTitle,
        )
    }

    fun removeChapterDownload(chapterId: Int) {
        viewModelScope.launch {
            offlineDownloadManager.removeOffline(chapterId)
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }
            libraryRepository.getSeries(seriesId).fold(
                onSuccess = { detail ->
                    _state.update { it.copy(isLoading = false, series = detail) }
                    checkCompanionAudiobook(detail)
                },
                onFailure = { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                },
            )
        }
    }

    private fun checkCompanionAudiobook(detail: SeriesDetail) {
        viewModelScope.launch {
            val companionId = mediaMatchingManager.getCompanionAudiobookId(seriesId)
            if (companionId != null) {
                _state.update { it.copy(companionAudiobookId = companionId) }
                absRepository.getItem(companionId).onSuccess { item ->
                    _state.update { it.copy(companionAudiobook = item) }
                }
            } else if (settingsRepository.currentAbsServerUrl.isNotBlank()) {
                absRepository.getLibraries().onSuccess { libraries ->
                    for (lib in libraries) {
                        absRepository.getLibraryItems(lib.id, limit = 100).onSuccess { items ->
                            val normDetail = detail.name.lowercase().replace(Regex("[^a-z0-9]"), "")
                            val match = items.firstOrNull { itm ->
                                val itmTitle = (itm.media?.metadata?.title ?: "").lowercase().replace(Regex("[^a-z0-9]"), "")
                                itmTitle.isNotBlank() && (itmTitle == normDetail || itmTitle.contains(normDetail) || normDetail.contains(itmTitle))
                            }
                            if (match != null) {
                                mediaMatchingManager.setManualMatch(seriesId, match.id)
                                _state.update { it.copy(companionAudiobookId = match.id, companionAudiobook = match) }
                                return@onSuccess
                            }
                        }
                    }
                }
            }
        }
    }

    fun playCompanionAudiobook() {
        val companion = _state.value.companionAudiobook ?: return
        val title = companion.media?.metadata?.title ?: _state.value.series?.name ?: "Audiobook"
        val author = companion.media?.metadata?.authorName ?: ""
        val coverUrl = absRepository.getCoverUrl(companion.id)
        audioPlayerManager.playAudiobook(
            itemId = companion.id,
            title = title,
            author = author,
            coverUrl = coverUrl,
            companionSeriesId = seriesId,
        )
    }

    /** Returns the first unread chapter to "continue" from. */
    fun continueChapter(): Chapter? {
        val s = _state.value.series ?: return null
        return s.allChapters.firstOrNull { it.pagesRead < it.pages && it.pages > 0 }
            ?: s.allChapters.firstOrNull()
    }
}
