package com.u1145h.kavitaandroid.feature.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.u1145h.kavitaandroid.data.local.db.dao.BookDao
import com.u1145h.kavitaandroid.data.local.db.entity.BookEntity
import com.u1145h.kavitaandroid.data.local.files.BookFileManager
import com.u1145h.kavitaandroid.data.remote.ServerHealthChecker
import com.u1145h.kavitaandroid.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * First-run server setup status.
 */
sealed interface SetupState {
    data object Idle : SetupState
    data object Checking : SetupState
    data class Error(val message: String) : SetupState
    data object Connected : SetupState
}

/**
 * Represents the server URL state when the app launches.
 */
sealed interface ServerUrlState {
    data object Loading : ServerUrlState
    data object Unconfigured : ServerUrlState
    data class Configured(val url: String) : ServerUrlState
}

/**
 * Supplies the embedded web UI with its base URL, native [KavitaBridge], and
 * offline downloaded books stream.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    val bridge: KavitaBridge,
    private val settingsRepository: SettingsRepository,
    private val healthChecker: ServerHealthChecker,
    private val bookFileManager: BookFileManager,
    private val bookDao: BookDao,
) : ViewModel() {
    val downloadedBooks: Flow<List<BookEntity>> = bookDao.observeAll().map { list ->
        list.filter { book ->
            val titleOk = book.title != "Books" && book.title != "Home (Kavita)" && book.title.isNotBlank()
            val sizeOk = book.fileSizeBytes > 50000
            titleOk && sizeOk
        }
    }

    val serverUrlState: StateFlow<ServerUrlState> = settingsRepository.serverUrl
        .map { url ->
            if (url.isBlank()) {
                ServerUrlState.Unconfigured
            } else {
                ServerUrlState.Configured(url)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
            initialValue = ServerUrlState.Loading,
        )

    private val _setupState = MutableStateFlow<SetupState>(SetupState.Idle)
    val setupState: StateFlow<SetupState> = _setupState.asStateFlow()

    fun deleteBook(book: BookEntity) {
        viewModelScope.launch {
            bookFileManager.deleteFile(book.filePath)
            bookFileManager.deleteFile(book.coverPath)
            bookDao.markDeleted(book.id)
        }
    }

    fun clearAllDownloads() {
        viewModelScope.launch {
            val all = bookDao.getAll()
            all.forEach { book ->
                bookFileManager.deleteFile(book.filePath)
                bookFileManager.deleteFile(book.coverPath)
                bookDao.markDeleted(book.id)
            }
        }
    }

    fun submitServerUrl(raw: String) {
        if (_setupState.value == SetupState.Checking) return
        val normalized = normalizeUrl(raw)
        if (normalized == null) {
            _setupState.value = SetupState.Error("That doesn't look like a valid server address.")
            return
        }
        viewModelScope.launch {
            _setupState.value = SetupState.Checking
            healthChecker.check(normalized)
                .onSuccess {
                    settingsRepository.setServerUrl(normalized)
                    _setupState.value = SetupState.Connected
                }
                .onFailure {
                    _setupState.value = SetupState.Error("Couldn't reach that server. Check the address and try again.")
                }
        }
    }

    private fun normalizeUrl(raw: String): String? {
        val trimmed = raw.trim().removeSuffix("/")
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.contains("://")) trimmed else "http://$trimmed"
        val parsed = withScheme.toHttpUrlOrNull() ?: return null
        if (parsed.host.isBlank()) return null
        return parsed.toString().removeSuffix("/")
    }
}

