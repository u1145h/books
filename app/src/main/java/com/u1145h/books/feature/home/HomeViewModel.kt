package com.u1145h.books.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.core.util.CoverUrlBuilder
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.AudiobookshelfRepository
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.domain.manager.MediaMatchingManager
import com.u1145h.books.domain.model.LibraryType
import com.u1145h.books.domain.model.ServerSource
import com.u1145h.books.domain.model.UnifiedLibrary
import com.u1145h.books.domain.model.UnifiedMediaItem
import com.u1145h.books.domain.model.UnifiedMediaType
import com.u1145h.books.feature.audio.AudioPlayerManager
import com.u1145h.books.work.ProgressSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val libraries: List<UnifiedLibrary> = emptyList(),
    val onDeck: List<UnifiedMediaItem> = emptyList(),
    val recentlyAdded: List<UnifiedMediaItem> = emptyList(),
    val inProgress: List<UnifiedMediaItem> = emptyList(),
    val username: String = "",
    val absUsername: String = "",
    val isKavitaConnected: Boolean = false,
    val isAbsConnected: Boolean = false,
    val apiKey: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val kavitaRepository: KavitaRepository,
    val audiobookshelfRepository: AudiobookshelfRepository,
    val mediaMatchingManager: MediaMatchingManager,
    val playerManager: AudioPlayerManager,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
    val coverUrlBuilder: CoverUrlBuilder,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    val serverUrl: String get() = settingsRepository.currentServerUrl

    init {
        viewModelScope.launch {
            sessionManager.session.collect { session ->
                _state.update {
                    it.copy(
                        username = session.username ?: it.username,
                        absUsername = session.absUsername ?: it.absUsername,
                        isKavitaConnected = session.isKavitaLoggedIn,
                        isAbsConnected = session.isAbsLoggedIn,
                        apiKey = session.apiKey,
                    )
                }
            }
        }
        ensureApiKeyProvisioned()
        load()
    }

    private fun ensureApiKeyProvisioned() {
        if (!sessionManager.apiKey.isNullOrBlank() || sessionManager.token.isNullOrBlank()) return
        viewModelScope.launch {
            val authKeysResult = kavitaRepository.getAuthKeys()
            val existingKey = authKeysResult.getOrNull()?.firstOrNull()?.key
            val key = if (!existingKey.isNullOrBlank()) {
                existingKey
            } else {
                kavitaRepository.createAuthKey("KavitaAndroid").getOrNull()?.key
            }
            if (!key.isNullOrBlank()) {
                sessionManager.updateApiKey(key)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            val username = sessionManager.session.value.username ?: ""
            val absUsername = sessionManager.session.value.absUsername ?: ""
            val isKavita = sessionManager.isKavitaLoggedIn.value
            val isAbs = sessionManager.isAbsLoggedIn.value

            // Kavita async queries
            val librariesDef = async { if (isKavita) libraryRepository.getLibraries() else Result.success(emptyList()) }
            val onDeckDef = async { if (isKavita) libraryRepository.getOnDeck() else Result.success(emptyList()) }
            val recentDef = async { if (isKavita) libraryRepository.getRecentlyAdded() else Result.success(emptyList()) }
            val inProgressDef = async { if (isKavita) libraryRepository.getInProgress() else Result.success(emptyList()) }

            // Audiobookshelf async queries
            val absLibrariesDef = async { if (isAbs) audiobookshelfRepository.getLibraries() else Result.success(emptyList()) }

            val librariesResult = librariesDef.await()
            val onDeckResult = onDeckDef.await()
            val recentResult = recentDef.await()
            val inProgressResult = inProgressDef.await()
            val absLibrariesResult = absLibrariesDef.await()

            val kavitaLibs = librariesResult.getOrElse { emptyList() }
            val kavitaOnDeck = onDeckResult.getOrElse { emptyList() }
            val kavitaRecent = recentResult.getOrElse { emptyList() }
            val kavitaInProgress = inProgressResult.getOrElse { emptyList() }
            val absLibs = absLibrariesResult.getOrElse { emptyList() }

            // Fetch ABS continue listening & recent items for first audio library if available
            val firstAudioLib = absLibs.firstOrNull()
            val absContinueListening = if (isAbs && firstAudioLib != null) {
                audiobookshelfRepository.getContinueListening(firstAudioLib.id).getOrElse { emptyList() }
            } else emptyList()

            val absRecentItems = if (isAbs && firstAudioLib != null) {
                audiobookshelfRepository.getLibraryItems(firstAudioLib.id, limit = 20).getOrElse { emptyList() }
            } else emptyList()

            // Run smart matching
            val allKavita = (kavitaOnDeck + kavitaRecent + kavitaInProgress).distinctBy { it.id }
            val allAbs = (absContinueListening + absRecentItems).distinctBy { it.id }
            mediaMatchingManager.indexMatches(allKavita, allAbs)

            // Convert to Unified Libraries
            val unifiedKavitaLibs = kavitaLibs.map { lib ->
                UnifiedLibrary(
                    id = "kavita:${lib.id}",
                    name = lib.name,
                    source = ServerSource.Kavita,
                    mediaType = when (lib.type) {
                        LibraryType.Book, LibraryType.LightNovel -> UnifiedMediaType.Book
                        LibraryType.Comic, LibraryType.ComicVine -> UnifiedMediaType.Comic
                        LibraryType.Manga, LibraryType.Image -> UnifiedMediaType.Manga
                    },
                    seriesCount = lib.seriesCount,
                    coverUrl = coverUrlBuilder.library(lib.id),
                )
            }

            val unifiedAbsLibs = absLibs.map { lib ->
                UnifiedLibrary(
                    id = "abs:${lib.id}",
                    name = lib.name,
                    source = ServerSource.Audiobookshelf,
                    mediaType = if (lib.mediaType == "podcast") UnifiedMediaType.Podcast else UnifiedMediaType.Audiobook,
                    seriesCount = 0,
                    coverUrl = null,
                )
            }

            val unifiedLibraries = unifiedKavitaLibs + unifiedAbsLibs

            // Convert to Unified On Deck
            val unifiedKavitaOnDeck = kavitaOnDeck.map { series ->
                val companionAbsId = mediaMatchingManager.getCompanionAudiobookId(series.id)
                UnifiedMediaItem(
                    id = "kavita:${series.id}",
                    title = series.name,
                    author = series.libraryName,
                    coverUrl = coverUrlBuilder.series(series.id),
                    source = ServerSource.Kavita,
                    kavitaSeriesId = series.id,
                    readProgressPercent = series.progressPercent,
                    companionAudiobookId = companionAbsId,
                )
            }

            val unifiedAbsOnDeck = absContinueListening.map { item ->
                val companionSeriesId = mediaMatchingManager.getCompanionSeriesId(item.id)
                UnifiedMediaItem(
                    id = "abs:${item.id}",
                    title = item.media?.metadata?.title ?: "Audiobook",
                    author = item.media?.metadata?.authorName ?: "",
                    narrator = item.media?.metadata?.narratorName,
                    coverUrl = audiobookshelfRepository.getCoverUrl(item.id),
                    source = ServerSource.Audiobookshelf,
                    absItemId = item.id,
                    durationSeconds = item.media?.duration,
                    companionTextSeriesId = companionSeriesId,
                )
            }

            val mergedOnDeck = unifiedKavitaOnDeck + unifiedAbsOnDeck

            // Unified Recently Added
            val unifiedKavitaRecent = kavitaRecent.map { series ->
                UnifiedMediaItem(
                    id = "kavita:${series.id}",
                    title = series.name,
                    author = series.libraryName,
                    coverUrl = coverUrlBuilder.series(series.id),
                    source = ServerSource.Kavita,
                    kavitaSeriesId = series.id,
                    companionAudiobookId = mediaMatchingManager.getCompanionAudiobookId(series.id),
                )
            }

            val unifiedAbsRecent = absRecentItems.map { item ->
                UnifiedMediaItem(
                    id = "abs:${item.id}",
                    title = item.media?.metadata?.title ?: "Audiobook",
                    author = item.media?.metadata?.authorName ?: "",
                    narrator = item.media?.metadata?.narratorName,
                    coverUrl = audiobookshelfRepository.getCoverUrl(item.id),
                    source = ServerSource.Audiobookshelf,
                    absItemId = item.id,
                    durationSeconds = item.media?.duration,
                    companionTextSeriesId = mediaMatchingManager.getCompanionSeriesId(item.id),
                )
            }

            val mergedRecent = unifiedKavitaRecent + unifiedAbsRecent

            // Unified In Progress
            val unifiedInProgress = kavitaInProgress.map { series ->
                UnifiedMediaItem(
                    id = "kavita:${series.id}",
                    title = series.name,
                    author = series.libraryName,
                    coverUrl = coverUrlBuilder.series(series.id),
                    source = ServerSource.Kavita,
                    kavitaSeriesId = series.id,
                    readProgressPercent = series.progressPercent,
                    companionAudiobookId = mediaMatchingManager.getCompanionAudiobookId(series.id),
                )
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    libraries = unifiedLibraries,
                    onDeck = mergedOnDeck,
                    recentlyAdded = mergedRecent,
                    inProgress = unifiedInProgress,
                    username = username,
                    absUsername = absUsername,
                    isKavitaConnected = isKavita,
                    isAbsConnected = isAbs,
                )
            }
        }
    }

    fun playAudiobook(item: UnifiedMediaItem) {
        val absId = item.absItemId ?: item.companionAudiobookId ?: return
        playerManager.playAudiobook(
            itemId = absId,
            title = item.title,
            author = item.author ?: item.narrator ?: "",
            coverUrl = item.coverUrl,
            companionSeriesId = item.kavitaSeriesId ?: item.companionTextSeriesId,
        )
    }

    fun logout() {
        viewModelScope.launch {
            ProgressSyncWorker.cancel(context)
            sessionManager.clear()
        }
    }
}
