package com.u1145h.books.domain.model

/**
 * Book formats supported by the offline library and its native readers.
 */
enum class BookFormat(val label: String, val extension: String) {
    EPUB("EPUB", "epub"),
    PDF("PDF", "pdf"),
    CBZ("CBZ", "cbz"),
    CBR("CBR", "cbr");

    companion object {
        fun fromFileName(fileName: String): BookFormat? {
            val ext = fileName.substringAfterLast('.', "").lowercase()
            return entries.firstOrNull { it.extension == ext }
        }
    }
}
