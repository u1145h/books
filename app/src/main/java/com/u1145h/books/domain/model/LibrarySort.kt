package com.u1145h.books.domain.model

/**
 * Sort options for the offline library grid.
 */
enum class LibrarySort(val label: String) {
    RECENTLY_ADDED("Recently added"),
    RECENTLY_READ("Recently read"),
    TITLE("Title"),
    AUTHOR("Author"),
    SERIES("Series"),
}
