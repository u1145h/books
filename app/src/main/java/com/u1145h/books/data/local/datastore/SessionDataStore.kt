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
 * Persisted authentication session supporting both Kavita and Audiobookshelf servers.
 */
@Singleton
class SessionDataStore @Inject constructor(
    @ApplicationContext context: Context,
) {
    private val dataStore = context.sessionDataStore

    // Kavita Keys
    private val keyToken = stringPreferencesKey("token")
    private val keyRefreshToken = stringPreferencesKey("refresh_token")
    private val keyApiKey = stringPreferencesKey("api_key")
    private val keyUsername = stringPreferencesKey("username")

    // Audiobookshelf Keys
    private val keyAbsServerUrl = stringPreferencesKey("abs_server_url")
    private val keyAbsToken = stringPreferencesKey("abs_token")
    private val keyAbsUserId = stringPreferencesKey("abs_user_id")
    private val keyAbsUsername = stringPreferencesKey("abs_username")

    val session: Flow<Session> = dataStore.data
        .catch { emit(emptyPreferences()) }
        .map { prefs ->
            Session(
                token = prefs[keyToken],
                refreshToken = prefs[keyRefreshToken],
                apiKey = prefs[keyApiKey],
                username = prefs[keyUsername],
                absServerUrl = prefs[keyAbsServerUrl],
                absToken = prefs[keyAbsToken],
                absUserId = prefs[keyAbsUserId],
                absUsername = prefs[keyAbsUsername],
            )
        }

    suspend fun update(session: Session) {
        dataStore.edit { prefs ->
            session.token?.let { prefs[keyToken] = it } ?: prefs.remove(keyToken)
            session.refreshToken?.let { prefs[keyRefreshToken] = it } ?: prefs.remove(keyRefreshToken)
            session.apiKey?.let { prefs[keyApiKey] = it } ?: prefs.remove(keyApiKey)
            session.username?.let { prefs[keyUsername] = it } ?: prefs.remove(keyUsername)

            session.absServerUrl?.let { prefs[keyAbsServerUrl] = it } ?: prefs.remove(keyAbsServerUrl)
            session.absToken?.let { prefs[keyAbsToken] = it } ?: prefs.remove(keyAbsToken)
            session.absUserId?.let { prefs[keyAbsUserId] = it } ?: prefs.remove(keyAbsUserId)
            session.absUsername?.let { prefs[keyAbsUsername] = it } ?: prefs.remove(keyAbsUsername)
        }
    }

    suspend fun updateApiKey(apiKey: String) {
        dataStore.edit { prefs ->
            prefs[keyApiKey] = apiKey
        }
    }

    suspend fun updateAbsSession(serverUrl: String, token: String, userId: String?, username: String?) {
        dataStore.edit { prefs ->
            prefs[keyAbsServerUrl] = serverUrl.trimEnd('/')
            prefs[keyAbsToken] = token
            userId?.let { prefs[keyAbsUserId] = it }
            username?.let { prefs[keyAbsUsername] = it }
        }
    }

    suspend fun clearAbs() {
        dataStore.edit { prefs ->
            prefs.remove(keyAbsServerUrl)
            prefs.remove(keyAbsToken)
            prefs.remove(keyAbsUserId)
            prefs.remove(keyAbsUsername)
        }
    }

    suspend fun clearKavita() {
        dataStore.edit { prefs ->
            prefs.remove(keyToken)
            prefs.remove(keyRefreshToken)
            prefs.remove(keyApiKey)
            prefs.remove(keyUsername)
        }
    }

    suspend fun clear() {
        dataStore.edit { it.clear() }
    }
}

data class Session(
    // Kavita Session
    val token: String? = null,
    val refreshToken: String? = null,
    val apiKey: String? = null,
    val username: String? = null,

    // Audiobookshelf Session
    val absServerUrl: String? = null,
    val absToken: String? = null,
    val absUserId: String? = null,
    val absUsername: String? = null,
) {
    val isKavitaLoggedIn: Boolean get() = !token.isNullOrBlank()
    val isAbsLoggedIn: Boolean get() = !absToken.isNullOrBlank()
    val isLoggedIn: Boolean get() = isKavitaLoggedIn || isAbsLoggedIn
}
