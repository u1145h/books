package com.u1145h.books.feature.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.books.data.local.datastore.AppSettings
import com.u1145h.books.domain.model.ThemeMode
import com.u1145h.books.data.local.db.dao.StorageStats
import com.u1145h.books.data.remote.auth.SessionManager
import com.u1145h.books.data.repository.AudiobookshelfRepository
import com.u1145h.books.data.repository.BookRepository
import com.u1145h.books.data.repository.SettingsRepository
import com.u1145h.books.work.ProgressSyncWorker
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val sessionManager: SessionManager,
    private val bookRepository: BookRepository,
    private val absRepository: AudiobookshelfRepository,
    @ApplicationContext private val context: Context,
) : ViewModel() {

    val settings: StateFlow<AppSettings> = settingsRepository.settings
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), AppSettings())

    val storageStats: StateFlow<StorageStats> = bookRepository.observeStats()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), StorageStats(0, 0L))

    val serverUrl: String get() = settingsRepository.currentServerUrl
    val username: String get() = sessionManager.session.value.username ?: ""

    val absServerUrl: String get() = settingsRepository.currentAbsServerUrl
    val absUsername: String get() = sessionManager.absUsername ?: ""
    val isAbsLoggedIn: Boolean get() = sessionManager.isAbsLoggedIn.value

    fun syncNow() {
        viewModelScope.launch {
            ProgressSyncWorker.syncNow(context)
            settingsRepository.setLastSyncAt(System.currentTimeMillis())
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun setReaderRtl(rtl: Boolean) {
        viewModelScope.launch { settingsRepository.setReaderRtl(rtl) }
    }

    fun setReaderWebtoon(webtoon: Boolean) {
        viewModelScope.launch { settingsRepository.setReaderWebtoon(webtoon) }
    }

    fun clearDownloads() {
        viewModelScope.launch { bookRepository.clearAll() }
    }
}
