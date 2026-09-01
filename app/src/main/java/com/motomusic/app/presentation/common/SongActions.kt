package com.motomusic.app.presentation.common

import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import com.motomusic.app.domain.model.Song

/**
 * Actions every list of songs needs.
 *
 * Screens call these instead of holding playback logic themselves; the implementation lives in
 * `PlayerViewModel` and is published once at the root of the app.
 */
@Stable
interface SongActions {
    fun play(songs: List<Song>, startIndex: Int = 0)
    fun shuffle(songs: List<Song>)
    fun playNext(song: Song)
    fun addToQueue(songs: List<Song>)
    fun toggleFavorite(song: Song)
    /** Leaves the file on the device but takes it out of the library until it is restored. */
    fun hideFromLibrary(song: Song)
    /** Opens the three-dot menu sheet hosted by the app scaffold. */
    fun openMenu(song: Song)
}

val LocalSongActions = staticCompositionLocalOf<SongActions> {
    error("LocalSongActions must be provided by MotoApp")
}

/** Ids of favourited songs, so any row can show the heart without its own database query. */
val LocalFavoriteIds = compositionLocalOf { emptySet<Long>() }

/** Id of the song currently loaded in the player, used to highlight it in lists. */
val LocalNowPlayingId = compositionLocalOf<Long?> { null }
