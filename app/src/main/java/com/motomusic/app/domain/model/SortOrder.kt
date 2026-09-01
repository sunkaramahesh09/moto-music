package com.motomusic.app.domain.model

/** Sort orders offered on the Songs screen and persisted in settings. */
enum class SortOrder(val storageKey: String, val label: String) {
    TITLE_ASC("title_asc", "Title A–Z"),
    TITLE_DESC("title_desc", "Title Z–A"),
    ARTIST_ASC("artist_asc", "Artist"),
    ALBUM_ASC("album_asc", "Album"),
    DATE_ADDED_DESC("date_added_desc", "Recently added"),
    DURATION_ASC("duration_asc", "Shortest first"),
    DURATION_DESC("duration_desc", "Longest first");

    companion object {
        fun fromStorageKey(key: String?): SortOrder =
            entries.firstOrNull { it.storageKey == key } ?: TITLE_ASC
    }
}
