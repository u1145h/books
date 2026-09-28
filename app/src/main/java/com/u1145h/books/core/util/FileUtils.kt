package com.u1145h.books.core.util

import java.security.MessageDigest
import java.util.Locale

/** Formats a byte count into a human readable string (e.g. "12.4 MB"). */
fun Long.toFormattedSize(): String {
    if (this <= 0) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var value = this.toDouble()
    var index = 0
    while (value >= 1024.0 && index < units.lastIndex) {
        value /= 1024.0
        index++
    }
    return if (index == 0) {
        "${value.toLong()} ${units[index]}"
    } else {
        String.format(Locale.US, "%.1f %s", value, units[index])
    }
}

/** Short stable hash of a string, useful for derived file names. */
fun String.shortHash(length: Int = 10): String {
    val digest = MessageDigest.getInstance("SHA-256").digest(toByteArray(Charsets.UTF_8))
    return digest.take(length).joinToString("") { "%02x".format(it) }
}
