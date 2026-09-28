package com.u1145h.books.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.u1145h.books.domain.model.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App preferences: Kavita and ABS server URLs, theming, reader and sync settings.
 */
@Singleton
class SettingsDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.settingsDataStore

    private val keyServerUrl = stringPreferencesKey("server_url")
    private val keyAbsServerUrl = stringPreferencesKey("abs_server_url")
    private val keyThemeMode = stringPreferencesKey("theme_mode")
    private val keyDynamicColor = booleanPreferencesKey("dynamic_color")
    private val keyDeveloperMode = booleanPreferencesKey("developer_mode")
    private val keyVerboseLogging = booleanPreferencesKey("verbose_logging")
    private val keyLastSyncAt = longPreferencesKey("last_sync_at_utc")
    private val keyReaderRtl = booleanPreferencesKey("reader_rtl")
    private val keyReaderWebtoon = booleanPreferencesKey("reader_webtoon")

    val settings: Flow<AppSettings> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            AppSettings(
                serverUrl = prefs[keyServerUrl] ?: "",
                absServerUrl = prefs[keyAbsServerUrl] ?: "",
                themeMode = prefs[keyThemeMode]
                    ?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() }
                    ?: ThemeMode.SYSTEM,
                dynamicColor = prefs[keyDynamicColor] ?: true,
                developerMode = prefs[keyDeveloperMode] ?: false,
                verboseLogging = prefs[keyVerboseLogging] ?: false,
                lastSyncAtUtc = prefs[keyLastSyncAt] ?: 0L,
                readerRtl = prefs[keyReaderRtl] ?: false,
                readerWebtoon = prefs[keyReaderWebtoon] ?: false,
            )
        }

    suspend fun setServerUrl(url: String) {
        dataStore.edit { it[keyServerUrl] = url.trim().trimEnd('/') }
    }

    suspend fun setAbsServerUrl(url: String) {
        dataStore.edit { it[keyAbsServerUrl] = url.trim().trimEnd('/') }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { it[keyThemeMode] = mode.name }
    }

    suspend fun setDynamicColor(enabled: Boolean) {
        dataStore.edit { it[keyDynamicColor] = enabled }
    }

    suspend fun setDeveloperMode(enabled: Boolean) {
        dataStore.edit { it[keyDeveloperMode] = enabled }
    }

    suspend fun setVerboseLogging(enabled: Boolean) {
        dataStore.edit { it[keyVerboseLogging] = enabled }
    }

    suspend fun setLastSyncAt(utcMillis: Long) {
        dataStore.edit { it[keyLastSyncAt] = utcMillis }
    }

    suspend fun setReaderRtl(rtl: Boolean) {
        dataStore.edit { it[keyReaderRtl] = rtl }
    }

    suspend fun setReaderWebtoon(webtoon: Boolean) {
        dataStore.edit { it[keyReaderWebtoon] = webtoon }
    }
}

data class AppSettings(
    val serverUrl: String = "",
    val absServerUrl: String = "",
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val developerMode: Boolean = false,
    val verboseLogging: Boolean = false,
    val lastSyncAtUtc: Long = 0L,
    val readerRtl: Boolean = false,
    val readerWebtoon: Boolean = false,
)
