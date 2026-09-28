package com.u1145h.books.data.repository

import com.u1145h.books.data.remote.api.KavitaApiService
import com.u1145h.books.data.remote.dto.SearchResultGroupDto
import com.u1145h.books.data.remote.DtoMapper.toDomain
import com.u1145h.books.domain.model.SearchResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

interface SearchRepository {
    suspend fun search(query: String): Result<SearchResult>
}

@Singleton
class SearchRepositoryImpl @Inject constructor(
    private val api: KavitaApiService,
) : SearchRepository {

    override suspend fun search(query: String): Result<SearchResult> =
        withContext(Dispatchers.IO) {
            runCatching {
                val resp = api.search(query)
                if (!resp.isSuccessful) {
                    throw RuntimeException("HTTP ${resp.code()}: ${resp.message()}")
                }
                val dto: SearchResultGroupDto = resp.body()
                    ?: return@runCatching SearchResult()
                SearchResult(
                    series = dto.series.map { it.toDomain() },
                    collections = dto.collections.map { it.toDomain() },
                )
            }
        }
}
