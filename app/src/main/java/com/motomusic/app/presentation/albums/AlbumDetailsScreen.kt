package com.motomusic.app.presentation.albums

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motomusic.app.presentation.common.CollectionScreen
import com.motomusic.app.presentation.common.songsSummary
import com.motomusic.app.ui.components.CollectionHeader

@Composable
fun AlbumDetailsScreen(
    state: AlbumDetailsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onArtistClick: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    val album = state.album

    CollectionScreen(
        title = album?.name ?: "Album",
        songs = state.songs,
        contentPadding = contentPadding,
        onBack = onBack,
        modifier = modifier,
        isLoading = state.isLoading,
        emptyIcon = Icons.Rounded.Album,
        emptyTitle = "Album is empty",
        emptyMessage = "The songs of this album are no longer on this device.",
        topBarActions = {
            if (album != null) {
                IconButton(onClick = { onArtistClick(album.artistId) }) {
                    Icon(Icons.Rounded.Person, contentDescription = "Go to artist")
                }
            }
        },
        header = {
            if (album != null) {
                CollectionHeader(
                    title = album.name,
                    subtitle = buildString {
                        append(album.artist)
                        if (album.year > 0) append(" · ${album.year}")
                        append(" · ${songsSummary(state.songs)}")
                    },
                    albumId = album.id,
                    songUri = album.artworkSongUri,
                )
            }
        },
    )
}
