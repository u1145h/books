package com.u1145h.books.data.local.datastore

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Persisted authentication session extracted from the embedded Kavita web UI
 * (or a native login). Keeps the user logged in across app restarts.
 */
@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.sessionDataStore

    private val keyToken = stringPreferencesKey("token")
    private val keyRefreshToken = stringPreferencesKey("refresh_token")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyUsername = stringPreferencesKey("username")

    val session: Flow<Session> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            Session(
                token = prefs[keyToken],
                refreshToken = prefs[keyRefreshToken],
                apiKey = prefs[keyApiKey],
                username = prefs[keyUsername],
            )
        }

    suspend fun update(session: Session) {
        dataStore.edit { prefs ->
            session.token?.let { prefs[keyToken] = it } ?: prefs.remove(keyToken)
            session.refreshToken?.let { prefs[keyRefreshToken] = it } ?: prefs.remove(keyRefreshToken)
            session.apiKey?.let { prefs[keyApiKey] = it } ?: prefs.remove(keyApiKey)
            session.username?.let { prefs[keyUsername] = it } ?: prefs.remove(keyUsername)
        }
    }

    suspend fun updateApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[keyApiKey] = apiKey
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

data class Session(
    val token: String? = null,
    val refreshToken: String? = null,
    val apiKey: String? = null,
    val username: String? = null,
) {
    val isLoggedIn: Boolean get() = !token.isNullOrBlank()
}
