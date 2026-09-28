package com.u1145h.books.data.remote.api

import com.u1145h.books.data.remote.dto.AuthKeyDto
import com.u1145h.books.data.remote.dto.BookInfoDto
import com.u1145h.books.data.remote.dto.CreateAuthKeyRequest
import com.u1145h.books.data.remote.dto.ChapterInfoDto
import com.u1145h.books.data.remote.dto.CollectionDto
import com.u1145h.books.data.remote.dto.LibraryDto
import com.u1145h.books.data.remote.dto.LoginRequest
import com.u1145h.books.data.remote.dto.MarkChapterReadDto
import com.u1145h.books.data.remote.dto.PageRequestDto
import com.u1145h.books.data.remote.dto.ProgressDto
import com.u1145h.books.data.remote.dto.SearchResultDto
import com.u1145h.books.data.remote.dto.SearchResultGroupDto
import com.u1145h.books.data.remote.dto.SeriesDetailDto
import com.u1145h.books.data.remote.dto.SeriesDto
import com.u1145h.books.data.remote.dto.SeriesFilterV2Dto
import com.u1145h.books.data.remote.dto.SeriesMetadataDto
import com.u1145h.books.data.remote.dto.SeriesWithProgressDto
import com.u1145h.books.data.remote.dto.ServerInfoDto
import com.u1145h.books.data.remote.dto.TokenRefreshRequest
import com.u1145h.books.data.remote.dto.TokenRefreshResponse
import com.u1145h.books.data.remote.dto.UserDto
import com.u1145h.books.data.remote.dto.VolumeDto
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

/**
 * Full Kavita REST API surface. Base URL resolves to `{server}/api/`.
 */
interface KavitaApiService {

    // ── Account ────────────────────────────────────────────────────────────────

    @POST("Account/login")
    suspend fun login(@Body body: LoginRequest): Response<UserDto>

    @POST("Account/refresh-token")
    suspend fun refreshToken(@Body body: TokenRefreshRequest): Response<TokenRefreshResponse>

    @GET("Account")
    suspend fun getAccount(): Response<UserDto>

    @GET("Account/auth-keys")
    suspend fun getAuthKeys(): Response<List<AuthKeyDto>>

    @POST("Account/create-auth-key")
    suspend fun createAuthKey(@Body body: CreateAuthKeyRequest): Response<AuthKeyDto>

    // ── Server ─────────────────────────────────────────────────────────────────

    @GET("Server/server-info")
    suspend fun getServerInfo(): Response<ServerInfoDto>

    @GET("Health")
    suspend fun healthCheck(): Response<Unit>

    // ── Library ────────────────────────────────────────────────────────────────

    @GET("Library/libraries")
    suspend fun getLibraries(): Response<List<LibraryDto>>

    // ── Series ─────────────────────────────────────────────────────────────────

    @POST("Series/all-v2")
    suspend fun getAllSeries(
        @Query("pageNumber") pageNumber: Int = 0,
        @Query("pageSize") pageSize: Int = 30,
        @Body filter: SeriesFilterV2Dto = SeriesFilterV2Dto(),
    ): Response<List<SeriesDto>>

    @GET("Series/{seriesId}")
    suspend fun getSeries(@Path("seriesId") seriesId: Int): Response<SeriesDto>

    @GET("Series/series-detail")
    suspend fun getSeriesDetail(@Query("seriesId") seriesId: Int): Response<SeriesDetailDto>

    @GET("Series/metadata")
    suspend fun getSeriesMetadata(@Query("seriesId") seriesId: Int): Response<SeriesMetadataDto>

    @GET("Series/volumes")
    suspend fun getVolumes(@Query("seriesId") seriesId: Int): Response<List<VolumeDto>>

    @POST("Series/on-deck")
    suspend fun getOnDeck(
        @Query("pageNumber") pageNumber: Int = 0,
        @Query("pageSize") pageSize: Int = 30,
        @Query("libraryId") libraryId: Int? = null,
    ): Response<List<SeriesDto>>

    @POST("Series/recently-added-v2")
    suspend fun getRecentlyAdded(
        @Query("pageNumber") pageNumber: Int = 0,
        @Query("pageSize") pageSize: Int = 30,
        @Body filter: SeriesFilterV2Dto = SeriesFilterV2Dto(),
    ): Response<List<SeriesDto>>

    @GET("Series/currently-reading")
    suspend fun getInProgress(
        @Query("pageNumber") pageNumber: Int = 0,
        @Query("pageSize") pageSize: Int = 30,
    ): Response<List<SeriesDto>>

    // ── Reader ─────────────────────────────────────────────────────────────────

    @GET("Reader/get-progress")
    suspend fun getProgress(@Query("chapterId") chapterId: Int): Response<ProgressDto>

    @POST("Reader/progress")
    suspend fun saveProgress(@Body body: ProgressDto): Response<Unit>

    @GET("Reader/chapter-info")
    suspend fun getChapterInfo(@Query("chapterId") chapterId: Int): Response<ChapterInfoDto>

    @GET("Reader/continue-point")
    suspend fun getContinuePoint(@Query("seriesId") seriesId: Int): Response<ChapterInfoDto>

    @POST("Reader/mark-chapter-read")
    suspend fun markChapterRead(@Body body: MarkChapterReadDto): Response<Unit>

    // ── Image ──────────────────────────────────────────────────────────────────

    @Streaming
    @GET("Image/series-cover")
    suspend fun getSeriesCover(@Query("seriesId") seriesId: Int): Response<ResponseBody>

    @Streaming
    @GET("Image/library-cover")
    suspend fun getLibraryCover(@Query("libraryId") libraryId: Int): Response<ResponseBody>

    // ── Book (EPUB) ────────────────────────────────────────────────────────────

    @GET("Book/{chapterId}/book-info")
    suspend fun getBookInfo(@Path("chapterId") chapterId: Int): Response<BookInfoDto>

    @Streaming
    @GET("Book/{chapterId}/book-page")
    suspend fun getBookPage(
        @Path("chapterId") chapterId: Int,
        @Query("page") page: Int,
    ): Response<ResponseBody>

    // ── Search ─────────────────────────────────────────────────────────────────

    @GET("Search/search")
    suspend fun search(@Query("queryString") query: String): Response<SearchResultGroupDto>

    // ── Collections ────────────────────────────────────────────────────────────

    @GET("Collection")
    suspend fun getCollections(): Response<List<CollectionDto>>

    @POST("Collection/{id}/series")
    suspend fun getCollectionSeries(
        @Path("id") collectionId: Int,
        @Body request: PageRequestDto,
    ): Response<List<SeriesDto>>
}
