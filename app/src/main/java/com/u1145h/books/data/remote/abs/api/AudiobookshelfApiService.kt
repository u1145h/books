package com.u1145h.books.data.remote.abs.api

import com.u1145h.books.data.remote.abs.dto.AbsLibrariesResponse
import com.u1145h.books.data.remote.abs.dto.AbsLibraryItemDto
import com.u1145h.books.data.remote.abs.dto.AbsLibraryItemsResponse
import com.u1145h.books.data.remote.abs.dto.AbsLoginRequest
import com.u1145h.books.data.remote.abs.dto.AbsLoginResponse
import com.u1145h.books.data.remote.abs.dto.AbsPersonalizedSectionDto
import com.u1145h.books.data.remote.abs.dto.AbsPlaySessionResponse
import com.u1145h.books.data.remote.abs.dto.AbsProgressRequest
import com.u1145h.books.data.remote.abs.dto.AbsProgressResponse
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

interface AudiobookshelfApiService {

    @POST("login")
    suspend fun login(@Body body: AbsLoginRequest): Response<AbsLoginResponse>

    @GET("api/authorize")
    suspend fun authorize(): Response<AbsLoginResponse>

    @GET("ping")
    suspend fun ping(): Response<ResponseBody>

    @GET("api/libraries")
    suspend fun getLibraries(): Response<AbsLibrariesResponse>

    @GET("api/libraries/{libraryId}/items")
    suspend fun getLibraryItems(
        @Path("libraryId") libraryId: String,
        @Query("limit") limit: Int = 50,
        @Query("page") page: Int = 0,
        @Query("sort") sort: String = "media.metadata.title",
    ): Response<AbsLibraryItemsResponse>

    @GET("api/libraries/{libraryId}/personalized")
    suspend fun getPersonalized(
        @Path("libraryId") libraryId: String,
    ): Response<List<AbsPersonalizedSectionDto>>

    @GET("api/items/{itemId}")
    suspend fun getItem(
        @Path("itemId") itemId: String,
        @Query("expanded") expanded: Int = 1,
    ): Response<AbsLibraryItemDto>

    @POST("api/items/{itemId}/play")
    suspend fun startPlaySession(
        @Path("itemId") itemId: String,
    ): Response<AbsPlaySessionResponse>

    @PATCH("api/me/progress/{libraryItemId}")
    suspend fun syncProgress(
        @Path("libraryItemId") libraryItemId: String,
        @Body body: AbsProgressRequest,
    ): Response<AbsProgressResponse>
}
