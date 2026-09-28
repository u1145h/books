package com.u1145h.books.feature.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.data.local.datastore.Session
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.AudiobookshelfRepository
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.work.ProgressSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SetupUiState(
    val isKavitaLoading: Boolean = false,
    val isKavitaConnected: Boolean = false,
    val kavitaError: String? = null,
    val kavitaUsername: String = "",

    val isAbsLoading: Boolean = false,
    val isAbsConnected: Boolean = false,
    val absError: String? = null,
    val absUsername: String = "",

    val isLoggedIn: Boolean = false,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val kavitaRepository: KavitaRepository,
    private val absRepository: AudiobookshelfRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(
        SetupUiState(
            isKavitaConnected = sessionManager.isKavitaLoggedIn.value,
            kavitaUsername = sessionManager.session.value.username ?: "",
            isAbsConnected = sessionManager.isAbsLoggedIn.value,
            absUsername = sessionManager.session.value.absUsername ?: "",
            isLoggedIn = sessionManager.isLoggedIn.value,
        )
    )
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    val savedServerUrl: String get() = settingsRepository.currentServerUrl
    val savedAbsServerUrl: String get() = settingsRepository.currentAbsServerUrl

    fun loginKavita(serverUrl: String, username: String, password: String) {
        val trimmed = serverUrl.trim().trimEnd('/')
        if (trimmed.isBlank() || username.isBlank() || password.isBlank()) {
            _state.update { it.copy(kavitaError = "All Kavita fields are required") }
            return
        }
        val normalizedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        viewModelScope.launch {
            _state.update { it.copy(isKavitaLoading = true, kavitaError = null) }
            settingsRepository.setServerUrl(normalizedUrl)

            val result = kavitaRepository.login(username, password)
            result.fold(
                onSuccess = { user ->
                    if (user.token.isNullOrBlank()) {
                        _state.update { it.copy(isKavitaLoading = false, kavitaError = "Login failed: no token returned") }
                        return@fold
                    }

                    var apiKey = user.apiKey ?: user.authKeys.firstOrNull()?.key
                    val currentSession = sessionManager.session.value
                    sessionManager.update(
                        currentSession.copy(
                            token = user.token,
                            refreshToken = user.refreshToken,
                            username = user.username,
                            apiKey = apiKey,
                        )
                    )

                    if (apiKey.isNullOrBlank()) {
                        val authKeysResult = kavitaRepository.getAuthKeys()
                        val existingKey = authKeysResult.getOrNull()?.firstOrNull()?.key
                        apiKey = if (!existingKey.isNullOrBlank()) existingKey else kavitaRepository.createAuthKey("KavitaAndroid").getOrNull()?.key
                        if (!apiKey.isNullOrBlank()) sessionManager.updateApiKey(apiKey)
                    }

                    ProgressSyncWorker.schedule(context)
                    _state.update {
                        it.copy(
                            isKavitaLoading = false,
                            isKavitaConnected = true,
                            kavitaUsername = user.username,
                            isLoggedIn = true,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isKavitaLoading = false,
                            kavitaError = "Cannot connect: ${e.message ?: "Unknown error"}",
                        )
                    }
                }
            )
        }
    }

    fun loginAudiobookshelf(serverUrl: String, username: String, password: String) {
        val trimmed = serverUrl.trim().trimEnd('/')
        if (trimmed.isBlank() || username.isBlank() || password.isBlank()) {
            _state.update { it.copy(absError = "All Audiobookshelf fields are required") }
            return
        }
        val normalizedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }

        viewModelScope.launch {
            _state.update { it.copy(isAbsLoading = true, absError = null) }
            settingsRepository.setAbsServerUrl(normalizedUrl)

            val result = absRepository.login(username, password)
            result.fold(
                onSuccess = { user ->
                    if (user.token.isNullOrBlank()) {
                        _state.update { it.copy(isAbsLoading = false, absError = "Login failed: no token returned") }
                        return@fold
                    }

                    sessionManager.updateAbsSession(
                        serverUrl = normalizedUrl,
                        token = user.token,
                        userId = user.id,
                        username = user.username,
                    )

                    _state.update {
                        it.copy(
                            isAbsLoading = false,
                            isAbsConnected = true,
                            absUsername = user.username,
                            isLoggedIn = true,
                        )
                    }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isAbsLoading = false,
                            absError = "Cannot connect: ${e.message ?: "Unknown error"}",
                        )
                    }
                }
            )
        }
    }

    fun enterApp() {
        if (_state.value.isKavitaConnected || _state.value.isAbsConnected) {
            _state.update { it.copy(isLoggedIn = true) }
        }
    }
}
