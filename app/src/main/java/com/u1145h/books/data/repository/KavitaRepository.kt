package com.u1145h.books.data.repository

import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.dto.AuthKeyDto
import com.u1145h.books.data.remote.dto.CreateAuthKeyRequest
import com.u1145h.books.data.remote.dto.BookInfoDto
import com.u1145h.books.data.remote.dto.ChapterInfoDto
import com.u1145h.books.data.remote.dto.LoginRequest
import com.u1145h.books.data.remote.dto.ProgressDto
import com.u1145h.books.data.remote.dto.UserDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface KavitaRepository {
    suspend fun login(username: String, password: String): Result<UserDto>
    suspend fun getProgress(chapterId: Int): Result<ProgressDto>
    suspend fun saveProgress(dto: ProgressDto): Result<Unit>
    suspend fun markChapterRead(seriesId: Int, chapterId: Int): Result<Unit>
    suspend fun getBookInfo(chapterId: Int): Result<BookInfoDto>
    suspend fun getBookPage(chapterId: Int, page: Int): Result<String>
    suspend fun getChapterInfo(chapterId: Int): Result<ChapterInfoDto>
    suspend fun fetchSeriesCover(seriesId: Int, destination: File): Result<File>
    suspend fun getAuthKeys(): Result<List<AuthKeyDto>>
    suspend fun createAuthKey(name: String): Result<AuthKeyDto>
}

@Singleton
class KavitaRepositoryImpl @Inject constructor(
    private val api: KavitaApiService,
) : KavitaRepository {

    override suspend fun login(username: String, password: String): Result<UserDto> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.login(LoginRequest(username = username, password = password))
                if (!response.isSuccessful) {
                    val code = response.code()
                    val errorBody = response.errorBody()?.string()
                    val message = when (code) {
                        401 -> "Invalid username or password"
                        400 -> "Invalid request: ${errorBody?.take(100) ?: "Bad Request"}"
                        404 -> "Kavita API not found (HTTP 404)"
                        else -> "Server returned HTTP $code"
                    }
                    throw IllegalArgumentException(message)
                }
                response.body() ?: throw IllegalStateException("Empty response from server")
            }
        }

    override suspend fun getProgress(chapterId: Int): Result<ProgressDto> =
        safeApiCall { api.getProgress(chapterId) }

    override suspend fun saveProgress(dto: ProgressDto): Result<Unit> =
        safeApiCall { api.saveProgress(dto) }.map { }

    override suspend fun markChapterRead(seriesId: Int, chapterId: Int): Result<Unit> =
        safeApiCall {
            api.markChapterRead(
                com.u1145h.books.data.remote.dto.MarkChapterReadDto(
                    seriesId = seriesId,
                    chapterId = chapterId,
                    generateReadingSession = true,
                )
            )
        }.map { }

    override suspend fun getBookInfo(chapterId: Int): Result<BookInfoDto> =
        safeApiCall { api.getBookInfo(chapterId) }

    override suspend fun getBookPage(chapterId: Int, page: Int): Result<String> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.getBookPage(chapterId, page)
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string()?.take(200)
                    throw RuntimeException("HTTP ${resp.code()}: $err")
                }
                resp.body()?.string() ?: ""
            }
        }

    override suspend fun getChapterInfo(chapterId: Int): Result<ChapterInfoDto> =
        safeApiCall { api.getChapterInfo(chapterId) }

    override suspend fun fetchSeriesCover(seriesId: Int, destination: File): Result<File> =
        withContext(Dispatchers.IO) {
            runCatching {
                val response = api.getSeriesCover(seriesId)
                if (!response.isSuccessful) {
                    throw RuntimeException("HTTP ${response.code()}: ${response.message()}")
                }
                val body = response.body() ?: throw IllegalStateException("Empty cover response")
                body.byteStream().use { input ->
                    destination.outputStream().use { output -> input.copyTo(output) }
                }
                destination
            }
        }

    override suspend fun getAuthKeys(): Result<List<AuthKeyDto>> =
        safeApiCall { api.getAuthKeys() }

    override suspend fun createAuthKey(name: String): Result<AuthKeyDto> =
        safeApiCall { api.createAuthKey(CreateAuthKeyRequest(name = name)) }

    private suspend fun <T> safeApiCall(block: suspend () -> retrofit2.Response<T>): Result<T> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = block()
                if (!resp.isSuccessful) {
                    val err = resp.errorBody()?.string()?.take(200)
                    throw RuntimeException("HTTP ${resp.code()} ${resp.message()}${if (!err.isNullOrBlank()) ": $err" else ""}")
                }
                resp.body() ?: throw IllegalStateException("Empty response from server")
            }
        }
}
