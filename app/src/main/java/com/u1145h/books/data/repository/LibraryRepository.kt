package com.u1145h.books.data.repository

import com.u1145h.books.data.remote.DtoMapper.toDomain
import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.dto.PageRequestDto
import com.u1145h.books.data.remote.dto.SeriesFilterStatementDto
import com.u1145h.books.data.remote.dto.SeriesFilterV2Dto
import com.u1145h.books.domain.model.Collection
import com.u1145h.books.domain.model.Library
import com.u1145h.books.domain.model.Series
import com.u1145h.books.domain.model.SeriesDetail
import com.u1145h.books.domain.model.Volume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton

interface LibraryRepository {
    suspend fun getLibraries(): Result<List<Library>>
    suspend fun getAllSeries(libraryId: Int, page: Int = 0, pageSize: Int = 30): Result<List<Series>>
    suspend fun getOnDeck(page: Int = 0): Result<List<Series>>
    suspend fun getRecentlyAdded(page: Int = 0): Result<List<Series>>
    suspend fun getInProgress(page: Int = 0): Result<List<Series>>
    suspend fun getSeries(seriesId: Int): Result<SeriesDetail>
    suspend fun getVolumes(seriesId: Int): Result<List<Volume>>
    suspend fun getCollections(): Result<List<Collection>>
    suspend fun getCollectionSeries(collectionId: Int, page: Int = 0): Result<List<Series>>
}

@Singleton
class LibraryRepositoryImpl @Inject constructor(
    private val api: KavitaApiService,
) : LibraryRepository {

    override suspend fun getLibraries(): Result<List<Library>> = safeApiCallList(
        call = { api.getLibraries() },
        transform = { it.toDomain() },
    )

    override suspend fun getAllSeries(
        libraryId: Int,
        page: Int,
        pageSize: Int,
    ): Result<List<Series>> = safeApiCallList(
        call = {
            val filter = if (libraryId > 0) {
                SeriesFilterV2Dto(
                    statements = listOf(
                        SeriesFilterStatementDto(
                            comparison = 0, // Equal
                            field = 19,     // Libraries
                            value = libraryId.toString(),
                        )
                    )
                )
            } else {
                SeriesFilterV2Dto()
            }
            api.getAllSeries(
                pageNumber = page,
                pageSize = pageSize,
                filter = filter,
            )
        },
        transform = { it.toDomain() },
    )

    override suspend fun getOnDeck(page: Int): Result<List<Series>> = safeApiCallList(
        call = { api.getOnDeck(pageNumber = page) },
        transform = { it.toDomain() },
    )

    override suspend fun getRecentlyAdded(page: Int): Result<List<Series>> = safeApiCallList(
        call = { api.getRecentlyAdded(pageNumber = page) },
        transform = { it.toDomain() },
    )

    override suspend fun getInProgress(page: Int): Result<List<Series>> = safeApiCallList(
        call = { api.getInProgress(pageNumber = page) },
        transform = { it.toDomain() },
    )

    override suspend fun getSeries(seriesId: Int): Result<SeriesDetail> = withContext(Dispatchers.IO) {
        runCatching {
            val seriesResp = api.getSeries(seriesId)
            if (!seriesResp.isSuccessful) {
                throw RuntimeException("HTTP ${seriesResp.code()}: ${seriesResp.message()}")
            }
            val series = seriesResp.body() ?: throw IllegalStateException("Empty series response")

            val volumes = runCatching {
                api.getVolumes(seriesId).body()?.map { it.toDomain() }
            }.getOrNull() ?: emptyList()

            val metadata = runCatching {
                api.getSeriesMetadata(seriesId).body()
            }.getOrNull()

            SeriesDetail(
                id = series.id,
                name = series.name,
                summary = metadata?.summary,
                libraryId = series.libraryId,
                libraryName = series.libraryName,
                format = series.format,
                pages = series.pages,
                pagesRead = series.pagesRead,
                volumes = volumes,
                genres = metadata?.genres?.map { it.title } ?: emptyList(),
                tags = metadata?.tags?.map { it.title } ?: emptyList(),
                writers = metadata?.writers?.map { it.name } ?: emptyList(),
                publishers = metadata?.publishers?.map { it.name } ?: emptyList(),
                ageRating = metadata?.ageRating ?: series.ageRating,
                avgRating = series.avgRating,
            )
        }
    }

    override suspend fun getVolumes(seriesId: Int): Result<List<Volume>> = safeApiCallList(
        call = { api.getVolumes(seriesId) },
        transform = { it.toDomain() },
    )

    override suspend fun getCollections(): Result<List<Collection>> = safeApiCallList(
        call = { api.getCollections() },
        transform = { it.toDomain() },
    )

    override suspend fun getCollectionSeries(
        collectionId: Int,
        page: Int,
    ): Result<List<Series>> = safeApiCallList(
        call = {
            api.getCollectionSeries(
                collectionId,
                PageRequestDto(pageNumber = page),
            )
        },
        transform = { it.toDomain() },
    )

    private suspend fun <T, R> safeApiCallList(
        call: suspend () -> Response<List<T>>,
        transform: (T) -> R,
    ): Result<List<R>> = withContext(Dispatchers.IO) {
        runCatching {
            val response = call()
            if (!response.isSuccessful) {
                val errBody = response.errorBody()?.string()?.take(200)
                throw RuntimeException(
                    "HTTP ${response.code()} ${response.message()}${if (!errBody.isNullOrBlank()) ": $errBody" else ""}"
                )
            }
            val list = response.body() ?: emptyList()
            list.map(transform)
        }
    }
}
