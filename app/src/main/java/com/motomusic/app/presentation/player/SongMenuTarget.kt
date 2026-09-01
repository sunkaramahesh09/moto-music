package com.motomusic.app.presentation.player

import androidx.compose.runtime.Immutable
import com.motomusic.app.domain.model.Song

/**
 * What the three-dot sheet is showing.
 *
 * [playlistId] is set only when the menu was opened from inside a playlist, which is the one
 * place "Remove from playlist" means anything.
 */
@Immutable
data class SongMenuTarget(
    val song: Song,
    val playlistId: Long? = null,
)
