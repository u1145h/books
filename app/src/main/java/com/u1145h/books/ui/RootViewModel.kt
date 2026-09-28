package com.u1145h.books.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.data.local.datastore.AppSettings
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.KavitaRepository
import com.u1145h.books.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * App-level ViewModel: provides theme settings, login state, and splash-screen
 * readiness to the root [KavitaApp] composable.
 */
@HiltViewModel
class RootViewModel @Inject constructor(
    settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val kavitaRepository: KavitaRepository,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.Eagerly, AppSettings())

    val isLoggedIn: StateFlow<Boolean> = sessionManager.isLoggedIn

    /** True once the DataStore has emitted its first value - used to dismiss splash. */
    val isReady: StateFlow<Boolean> = settingsRepository.settings
        .map { true }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    init {
        viewModelScope.launch {
            if (!sessionManager.token.isNullOrBlank() && sessionManager.apiKey.isNullOrBlank()) {
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
    }
}
