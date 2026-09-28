package com.u1145h.books.data.repository

import com.u1145h.books.data.remote.abs.api.AudiobookshelfApiService
import com.u1145h.books.data.remote.abs.dto.AbsLibraryDto
import com.u1145h.books.data.remote.abs.dto.AbsLibraryItemDto
import com.u1145h.books.data.remote.abs.dto.AbsLoginRequest
import com.u1145h.books.data.remote.abs.dto.AbsPlaySessionResponse
import com.u1145h.books.data.remote.abs.dto.AbsProgressRequest
import com.u1145h.books.data.remote.abs.dto.AbsUserDto
import com.u1145h.books.data.remote.auth.SessionManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface AudiobookshelfRepository {
    suspend fun login(username: String, password: String): Result<AbsUserDto>
    suspend fun testConnection(): Result<Boolean>
    suspend fun getLibraries(): Result<List<AbsLibraryDto>>
    suspend fun getLibraryItems(libraryId: String, limit: Int = 50, page: Int = 0): Result<List<AbsLibraryItemDto>>
    suspend fun getContinueListening(libraryId: String): Result<List<AbsLibraryItemDto>>
    suspend fun getItem(itemId: String): Result<AbsLibraryItemDto>
    suspend fun startPlaySession(itemId: String): Result<AbsPlaySessionResponse>
    suspend fun syncProgress(itemId: String, currentTime: Double, duration: Double, progress: Double): Result<Unit>
    fun getCoverUrl(itemId: String): String
    fun getStreamUrl(itemId: String, fileId: String): String
}

@Singleton
class AudiobookshelfRepositoryImpl @Inject constructor(
    private val api: AudiobookshelfApiService,
    private val sessionManager: SessionManager,
    private val settingsRepository: SettingsRepository,
) : AudiobookshelfRepository {

    override suspend fun login(username: String, password: String): Result<AbsUserDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.login(AbsLoginRequest(username = username, password = password))
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string()?.take(200)
                    throw IllegalArgumentException("Audiobookshelf HTTP ${resp.code()}: $err")
                }
                val body = resp.body() ?: throw IllegalStateException("Empty response from Audiobookshelf")
                body.user ?: throw IllegalStateException("User object missing in Audiobookshelf response")
            }
        }

    override suspend fun testConnection(): Result<Boolean> =
        withContext(Dispatchers.IO) {
            runCatching {
                val ping = api.ping()
                if (ping.isSuccessful) return@runCatching true
                val auth = api.authorize()
                auth.isSuccessful
            }
        }

    override suspend fun getLibraries(): Result<List<AbsLibraryDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getLibraries()
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}")
                }
                resp.body()?.libraries ?: emptyList()
            }
        }

    override suspend fun getLibraryItems(libraryId: String, limit: Int, page: Int): Result<List<AbsLibraryItemDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getLibraryItems(libraryId, limit, page)
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}")
                }
                resp.body()?.results ?: emptyList()
            }
        }

    override suspend fun getContinueListening(libraryId: String): Result<List<AbsLibraryItemDto>> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getPersonalized(libraryId)
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}")
                }
                val sections = resp.body() ?: emptyList()
                sections.firstOrNull { it.id == "continue-listening" || it.type == "continue-listening" }?.entities
                    ?: emptyList()
            }
        }

    override suspend fun getItem(itemId: String): Result<AbsLibraryItemDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getItem(itemId, expanded = 1)
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}")
                }
                resp.body() ?: throw IllegalStateException("Item not found")
            }
        }

    override suspend fun startPlaySession(itemId: String): Result<AbsPlaySessionResponse> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.startPlaySession(itemId)
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}")
                }
                resp.body() ?: throw IllegalStateException("Failed to initialize audio session")
            }
        }

    override suspend fun syncProgress(itemId: String, currentTime: Double, duration: Double, progress: Double): Result<Unit> =
        withContext(Dispatchers.IO) {
            runCatching {
                val req = AbsProgressRequest(
                    currentTime = currentTime,
                    timeListened = 15.0,
                    duration = duration,
                    progress = progress,
                    isFinished = progress >= 0.99,
                )
                api.syncProgress(itemId, req)
            }.map { }
        }

    override fun getCoverUrl(itemId: String): String {
        val base = settingsRepository.currentAbsServerUrl.trimEnd('/')
        if (base.isBlank()) return ""
        val token = sessionManager.absToken
        val tokenParam = if (!token.isNullOrBlank()) "?token=$token" else ""
        return "$base/api/items/$itemId/cover$tokenParam"
    }

    override fun getStreamUrl(itemId: String, fileId: String): String {
        val base = settingsRepository.currentAbsServerUrl.trimEnd('/')
        if (base.isBlank()) return ""
        val token = sessionManager.absToken
        val tokenParam = if (!token.isNullOrBlank()) "?token=$token" else ""
        return "$base/api/items/$itemId/file/$fileId$tokenParam"
    }
}
