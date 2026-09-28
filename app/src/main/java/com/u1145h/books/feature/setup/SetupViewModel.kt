package com.u1145h.books.feature.setup

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.data.local.datastore.Session
import com.u1145h.books.data.remote.auth.SessionManager
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
    val isLoading: Boolean = false,
    val isLoggedIn: Boolean = false,
    val error: String? = null,
)

@HiltViewModel
class SetupViewModel @Inject constructor(
    private val kavitaRepository: KavitaRepository,
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state.asStateFlow()

    val savedServerUrl: String get() = settingsRepository.currentServerUrl

    fun login(serverUrl: String, username: String, password: String) {
        val trimmed = serverUrl.trim().trimEnd('/')
        if (trimmed.isBlank() || username.isBlank() || password.isBlank()) {
            _state.update { it.copy(error = "All fields are required") }
            return
        }
        val normalizedUrl = if (!trimmed.startsWith("http://", ignoreCase = true) && !trimmed.startsWith("https://", ignoreCase = true)) {
            "https://$trimmed"
        } else {
            trimmed
        }
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true, error = null) }

            // 1. Persist the server URL so DynamicBaseUrlInterceptor can use it immediately
            settingsRepository.setServerUrl(normalizedUrl)

            // 2. Attempt login
            val result = kavitaRepository.login(username, password)
            result.fold(
                onSuccess = { user ->
                    if (user.token.isNullOrBlank()) {
                        _state.update {
                            it.copy(isLoading = false, error = "Login failed: no token returned")
                        }
                        return@fold
                    }
                    // 3. Determine API key or provision one automatically
                    var apiKey = user.apiKey ?: user.authKeys.firstOrNull()?.key

                    sessionManager.update(
                        Session(
                            token = user.token,
                            refreshToken = user.refreshToken,
                            username = user.username,
                            apiKey = apiKey,
                        )
                    )

                    // If userDto did not contain an API key, auto-fetch or create one
                    if (apiKey.isNullOrBlank()) {
                        val authKeysResult = kavitaRepository.getAuthKeys()
                        val existingKey = authKeysResult.getOrNull()?.firstOrNull()?.key
                        if (!existingKey.isNullOrBlank()) {
                            apiKey = existingKey
                        } else {
                            val createdResult = kavitaRepository.createAuthKey("KavitaAndroid")
                            apiKey = createdResult.getOrNull()?.key
                        }
                        if (!apiKey.isNullOrBlank()) {
                            sessionManager.updateApiKey(apiKey)
                        }
                    }

                    // Start periodic progress sync
                    ProgressSyncWorker.schedule(context)
                    _state.update { it.copy(isLoading = false, isLoggedIn = true) }
                },
                onFailure = { e ->
                    _state.update {
                        it.copy(
                            isLoading = false,
                            error = "Cannot connect: ${e.message ?: "Unknown error"}",
                        )
                    }
                },
            )
        }
    }
}
