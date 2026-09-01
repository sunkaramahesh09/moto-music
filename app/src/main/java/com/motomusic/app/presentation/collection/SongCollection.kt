package com.motomusic.app.presentation.collection

/** The four "a query over the library" screens reachable from home. */
enum class SongCollection(val key: String, val title: String) {
    FAVORITES("favorites", "Favourites"),
    RECENTLY_PLAYED("recently_played", "Recently played"),
    MOST_PLAYED("most_played", "Frequently played"),
    RECENTLY_ADDED("recently_added", "Recently added");

    companion object {
        fun fromKey(key: String?): SongCollection = entries.firstOrNull { it.key == key } ?: FAVORITES
    }
}
