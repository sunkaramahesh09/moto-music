package com.motomusic.app.presentation.common

import com.motomusic.app.core.formatTotalDuration
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.Song

/** The one-line summary under a collection title, e.g. `12 songs · 48 min`. */
fun songsSummary(songs: List<Song>): String {
    if (songs.isEmpty()) return "No songs"
    val total = songs.sumOf { it.durationMs }
    return "${pluralise(songs.size, "song")} · ${formatTotalDuration(total)}"
}
