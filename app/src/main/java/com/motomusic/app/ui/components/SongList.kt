package com.motomusic.app.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motomusic.app.domain.model.Song
import com.motomusic.app.presentation.common.LocalSongActions

/**
 * The standard song list.
 *
 * Rows are keyed by MediaStore id so Compose reuses them across sorts, searches and rescans;
 * that plus a fixed row height is what keeps a 10,000-song list scrolling smoothly.
 */
@Composable
fun SongList(
    songs: List<Song>,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    listState: LazyListState = rememberLazyListState(),
    header: (@Composable () -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
) {
    val actions = LocalSongActions.current

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        state = listState,
        contentPadding = contentPadding,
    ) {
        if (header != null) {
            item(key = "song-list-header", contentType = "header") { header() }
        }
        items(
            items = songs,
            key = { it.id },
            contentType = { "song" },
        ) { song ->
            SongRow(
                song = song,
                onClick = {
                    val index = songs.indexOf(song)
                    if (onSongClick != null) onSongClick(index) else actions.play(songs, index)
                },
            )
        }
    }
}
