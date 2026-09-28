package com.u1145h.books.core.util

import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException

/**
 * Parses Kavita's `lastModifiedUtc` (ISO-8601, possibly without offset) into
 * epoch millis. Returns 0 for unparseable input.
 */
fun parseIsoToMillis(value: String): Long {
    val v = value.trim()
    if (v.isEmpty()) return 0L
    return try {
        OffsetDateTime.parse(v).toInstant().toEpochMilli()
    } catch (_: DateTimeParseException) {
        try {
            LocalDateTime.parse(v, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .toInstant(ZoneOffset.UTC)
                .toEpochMilli()
        } catch (_: DateTimeParseException) {
            0L
        }
    }
}

/** Formats epoch millis as ISO-8601 UTC for the server. */
fun millisToIsoUtc(millis: Long): String =
    Instant.ofEpochMilli(millis).toString()
