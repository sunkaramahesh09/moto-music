package com.motomusic.app.presentation.artists

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.Album
import com.motomusic.app.presentation.common.CollectionScreen
import com.motomusic.app.presentation.common.songsSummary
import com.motomusic.app.ui.components.AlbumGridItem
import com.motomusic.app.ui.components.CollectionHeader
import com.motomusic.app.ui.components.SectionHeader

@Composable
fun ArtistDetailsScreen(
    state: ArtistDetailsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    val artist = state.artist

    CollectionScreen(
        title = artist?.name ?: "Artist",
        songs = state.songs,
        contentPadding = contentPadding,
        onBack = onBack,
        modifier = modifier,
        isLoading = state.isLoading,
        emptyIcon = Icons.Rounded.Person,
        emptyTitle = "Nothing by this artist",
        emptyMessage = "Their songs are no longer on this device.",
        header = {
            Column {
                if (artist != null) {
                    CollectionHeader(
                        title = artist.name,
                        subtitle = "${pluralise(artist.albumCount, "album")} · ${songsSummary(state.songs)}",
                        albumId = artist.artworkAlbumId,
                        songUri = artist.artworkSongUri,
                        icon = Icons.Rounded.Person,
                    )
                }

                if (state.albums.isNotEmpty()) {
                    SectionHeader(title = "Albums")
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        items(items = state.albums, key = { it.id }) { album ->
                            AlbumGridItem(
                                album = album,
                                onClick = { onAlbumClick(album) },
                                modifier = Modifier.width(150.dp),
                            )
                        }
                    }
                    SectionHeader(title = "Songs")
                }
            }
        },
    )
}
