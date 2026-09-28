package com.u1145h.books.data.repository

import com.u1145h.books.data.local.datastore.AppSettings
import com.u1145h.books.data.local.datastore.Session
import com.u1145h.books.data.local.datastore.SessionDataStore
import com.u1145h.books.data.local.datastore.SettingsDataStore
import com.u1145h.books.domain.model.ThemeMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SettingsRepository @Inject constructor(
    private val settingsDataStore: SettingsDataStore,
    private val sessionDataStore: SessionDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    val settings: Flow<AppSettings> = settingsDataStore.settings

    val serverUrl: Flow<String> = settings
        .map { it.serverUrl }
        .distinctUntilChanged()

    val absServerUrl: Flow<String> = settings
        .map { it.absServerUrl }
        .distinctUntilChanged()

    @Volatile
    private var _cachedServerUrl: String = ""

    @Volatile
    private var _cachedAbsServerUrl: String = ""

    val currentServerUrl: String
        get() {
            if (_cachedServerUrl.isNotBlank()) return _cachedServerUrl
            val sync = runCatching {
                kotlinx.coroutines.runBlocking { serverUrl.first() }
            }.getOrDefault("")
            if (sync.isNotBlank()) _cachedServerUrl = sync
            return _cachedServerUrl
        }

    val currentAbsServerUrl: String
        get() {
            if (_cachedAbsServerUrl.isNotBlank()) return _cachedAbsServerUrl
            val sync = runCatching {
                kotlinx.coroutines.runBlocking { absServerUrl.first() }
            }.getOrDefault("")
            if (sync.isNotBlank()) _cachedAbsServerUrl = sync
            return _cachedAbsServerUrl
        }

    val session: Flow<Session> = sessionDataStore.session

    init {
        scope.launch {
            serverUrl.collect { _cachedServerUrl = it }
        }
        scope.launch {
            absServerUrl.collect { _cachedAbsServerUrl = it }
        }
    }

    suspend fun setServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        _cachedServerUrl = trimmed
        settingsDataStore.setServerUrl(trimmed)
    }

    suspend fun setAbsServerUrl(url: String) {
        val trimmed = url.trim().trimEnd('/')
        _cachedAbsServerUrl = trimmed
        settingsDataStore.setAbsServerUrl(trimmed)
    }

    suspend fun setThemeMode(mode: ThemeMode) = settingsDataStore.setThemeMode(mode)
    suspend fun setDynamicColor(enabled: Boolean) = settingsDataStore.setDynamicColor(enabled)
    suspend fun setDeveloperMode(enabled: Boolean) = settingsDataStore.setDeveloperMode(enabled)
    suspend fun setVerboseLogging(enabled: Boolean) = settingsDataStore.setVerboseLogging(enabled)
    suspend fun setLastSyncAt(utcMillis: Long) = settingsDataStore.setLastSyncAt(utcMillis)
    suspend fun setReaderRtl(rtl: Boolean) = settingsDataStore.setReaderRtl(rtl)
    suspend fun setReaderWebtoon(webtoon: Boolean) = settingsDataStore.setReaderWebtoon(webtoon)
    suspend fun updateSession(session: Session) = sessionDataStore.update(session)
    suspend fun clearSession() = sessionDataStore.clear()
}
