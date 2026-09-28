package com.u1145h.books.feature.home

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.core.util.CoverUrlBuilder
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.LibraryRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.domain.model.Library
import com.u1145h.books.domain.model.Series
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
    val libraries: List<Library> = emptyList(),
    val onDeck: List<Series> = emptyList(),
    val recentlyAdded: List<Series> = emptyList(),
    val inProgress: List<Series> = emptyList(),
    val username: String = "",
    val apiKey: String? = null,
    val error: String? = null,
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val libraryRepository: LibraryRepository,
    private val kavitaRepository: KavitaRepository,
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
            val currentApiKey = sessionManager.session.value.apiKey

            val librariesDef = async { libraryRepository.getLibraries() }
            val onDeckDef = async { libraryRepository.getOnDeck() }
            val recentDef = async { libraryRepository.getRecentlyAdded() }
            val inProgressDef = async { libraryRepository.getInProgress() }

            val librariesResult = librariesDef.await()
            val onDeckResult = onDeckDef.await()
            val recentResult = recentDef.await()
            val inProgressResult = inProgressDef.await()

            val libraries = librariesResult.getOrElse { emptyList() }
            val onDeck = onDeckResult.getOrElse { emptyList() }
            val recent = recentResult.getOrElse { emptyList() }
            val inProgress = inProgressResult.getOrElse { emptyList() }

            val firstError = librariesResult.exceptionOrNull()
                ?: onDeckResult.exceptionOrNull()
                ?: recentResult.exceptionOrNull()
                ?: inProgressResult.exceptionOrNull()

            if (librariesResult.isSuccess) {
                settingsRepository.setLastSyncAt(System.currentTimeMillis())
            }

            _state.update {
                it.copy(
                    isLoading = false,
                    libraries = libraries,
                    onDeck = onDeck,
                    recentlyAdded = recent,
                    inProgress = inProgress,
                    username = username,
                    apiKey = currentApiKey,
                    error = if (libraries.isEmpty() && onDeck.isEmpty() && recent.isEmpty() && inProgress.isEmpty()) {
                        firstError?.localizedMessage
                    } else null,
                )
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            ProgressSyncWorker.cancel(context)
            sessionManager.clear()
        }
    }
}
