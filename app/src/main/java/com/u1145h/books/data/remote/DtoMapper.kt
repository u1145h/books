package com.u1145h.books.data.remote

import com.u1145h.books.data.remote.dto.CollectionDto
import com.u1145h.books.data.remote.dto.LibraryDto
import com.u1145h.books.data.remote.dto.SeriesDetailDto
import com.u1145h.books.data.remote.dto.SeriesDto
import com.u1145h.books.data.remote.dto.VolumeDto
import com.u1145h.books.domain.model.Chapter
import com.u1145h.books.domain.model.Collection
import com.u1145h.books.domain.model.Library
import com.u1145h.books.domain.model.LibraryType
import com.u1145h.books.domain.model.Series
import com.u1145h.books.domain.model.SeriesDetail
import com.u1145h.books.domain.model.Volume

object DtoMapper {

    fun LibraryDto.toDomain() = Library(
        id = id,
        name = name,
        type = LibraryType.fromValue(type),
        seriesCount = count,
    )

    fun SeriesDto.toDomain() = Series(
        id = id,
        name = name,
        sortName = sortName ?: name,
        localizedName = localizedName,
        libraryId = libraryId,
        libraryName = libraryName,
        format = format,
        pagesRead = pagesRead,
        pages = pages,
        userRating = userRating,
        avgRating = avgRating,
        latestReadDate = latestReadDate,
        lastReadingProgressUtc = lastReadingProgressUtc,
        created = created,
    )

    fun com.u1145h.books.data.remote.dto.SearchResultDto.toDomain() = Series(
        id = seriesId,
        name = name,
        sortName = sortName ?: name,
        localizedName = localizedName,
        libraryId = libraryId,
        libraryName = libraryName,
        format = format,
    )

    fun SeriesDetailDto.toDomain() = SeriesDetail(
        id = id,
        name = name,
        summary = summary,
        libraryId = libraryId,
        libraryName = libraryName,
        format = format,
        pages = pages,
        pagesRead = pagesRead,
        volumes = volumes.map { it.toDomain() },
        genres = genres.map { it.title },
        tags = tags.map { it.title },
        writers = writers.map { it.name },
        publishers = publishers.map { it.name },
        ageRating = ageRating,
        avgRating = avgRating,
    )

    fun VolumeDto.toDomain() = Volume(
        id = id,
        number = number,
        name = name,
        pages = pages,
        pagesRead = pagesRead,
        seriesId = seriesId,
        chapters = chapters.map { c ->
            Chapter(
                id = c.id,
                number = c.number,
                range = c.range,
                title = c.title,
                pages = c.pages,
                pagesRead = c.pagesRead,
                isSpecial = c.isSpecial,
                volumeId = c.volumeId,
                seriesId = c.seriesId,
            )
        },
    )

    fun CollectionDto.toDomain() = Collection(
        id = id,
        title = title,
        summary = summary,
        itemCount = itemCount,
    )
}
