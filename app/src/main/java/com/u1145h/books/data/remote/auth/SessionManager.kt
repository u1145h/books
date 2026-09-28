package com.u1145h.books.data.remote.auth

import com.u1145h.books.data.local.datastore.Session
import com.u1145h.books.data.local.datastore.SessionDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Holds active authentication sessions in memory for both Kavita and Audiobookshelf.
 */
@Singleton
class SessionManager @Inject constructor(
    private val sessionDataStore: SessionDataStore,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    private val initialSession: Session = runCatching {
        runBlocking { sessionDataStore.session.first() }
    }.getOrDefault(Session())

    private val _session = MutableStateFlow(initialSession)
    val session: StateFlow<Session> = _session

    val isLoggedIn: StateFlow<Boolean> = _session
        .map { it.isLoggedIn }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialSession.isLoggedIn)

    val isKavitaLoggedIn: StateFlow<Boolean> = _session
        .map { it.isKavitaLoggedIn }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialSession.isKavitaLoggedIn)

    val isAbsLoggedIn: StateFlow<Boolean> = _session
        .map { it.isAbsLoggedIn }
        .stateIn(scope, kotlinx.coroutines.flow.SharingStarted.Eagerly, initialSession.isAbsLoggedIn)

    init {
        scope.launch {
            sessionDataStore.session.collect { persisted ->
                _session.value = persisted
            }
        }
    }

    val token: String?
        get() {
            val mem = _session.value.token
            if (!mem.isNullOrBlank()) return mem
            val sync = runCatching { runBlocking { sessionDataStore.session.first() } }.getOrNull()
            if (sync != null && !sync.token.isNullOrBlank()) {
                _session.value = sync
                return sync.token
            }
            return null
        }

    val apiKey: String?
        get() {
            val mem = _session.value.apiKey
            if (!mem.isNullOrBlank()) return mem
            val sync = runCatching { runBlocking { sessionDataStore.session.first() } }.getOrNull()
            if (sync != null && !sync.apiKey.isNullOrBlank()) {
                _session.value = sync
                return sync.apiKey
            }
            return null
        }

    val absToken: String?
        get() {
            val mem = _session.value.absToken
            if (!mem.isNullOrBlank()) return mem
            val sync = runCatching { runBlocking { sessionDataStore.session.first() } }.getOrNull()
            if (sync != null && !sync.absToken.isNullOrBlank()) {
                _session.value = sync
                return sync.absToken
            }
            return null
        }

    val absUsername: String?
        get() = _session.value.absUsername

    val absServerUrl: String?
        get() {
            val mem = _session.value.absServerUrl
            if (!mem.isNullOrBlank()) return mem
            val sync = runCatching { runBlocking { sessionDataStore.session.first() } }.getOrNull()
            if (sync != null && !sync.absServerUrl.isNullOrBlank()) {
                _session.value = sync
                return sync.absServerUrl
            }
            return null
        }

    suspend fun update(session: Session) {
        _session.value = session
        sessionDataStore.update(session)
    }

    suspend fun updateApiKey(apiKey: String) {
        _session.value = _session.value.copy(apiKey = apiKey)
        sessionDataStore.updateApiKey(apiKey)
    }

    suspend fun updateAbsSession(serverUrl: String, token: String, userId: String?, username: String?) {
        val updated = _session.value.copy(
            absServerUrl = serverUrl.trimEnd('/'),
            absToken = token,
            absUserId = userId,
            absUsername = username,
        )
        _session.value = updated
        sessionDataStore.updateAbsSession(serverUrl, token, userId, username)
    }

    suspend fun clearAbs() {
        _session.value = _session.value.copy(absServerUrl = null, absToken = null, absUserId = null, absUsername = null)
        sessionDataStore.clearAbs()
    }

    suspend fun clearKavita() {
        _session.value = _session.value.copy(token = null, refreshToken = null, apiKey = null, username = null)
        sessionDataStore.clearKavita()
    }

    suspend fun clear() {
        _session.value = Session()
        sessionDataStore.clear()
    }
}
