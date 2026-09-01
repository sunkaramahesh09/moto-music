package com.motomusic.app.presentation.playlists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.motomusic.app.domain.model.Song
import com.motomusic.app.presentation.common.CollectionScreen
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.presentation.common.SongActions
import com.motomusic.app.presentation.common.songsSummary
import com.motomusic.app.ui.components.CollectionHeader

@Composable
fun PlaylistDetailsScreen(
    state: PlaylistDetailsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onRename: (String) -> Unit,
    onDelete: () -> Unit,
    onOpenSongMenu: (Song) -> Unit,
    modifier: Modifier = Modifier,
) {
    val playlist = state.playlist
    var renaming by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }

    // The rows here open a menu that also offers "Remove from playlist"; everything else about
    // a song row behaves exactly as it does anywhere else.
    val parentActions = LocalSongActions.current
    val scopedActions = remember(parentActions, onOpenSongMenu) {
        object : SongActions by parentActions {
            override fun openMenu(song: Song) = onOpenSongMenu(song)
        }
    }

    CompositionLocalProvider(LocalSongActions provides scopedActions) {
        CollectionScreen(
            title = playlist?.name ?: "Playlist",
            songs = state.songs,
            contentPadding = contentPadding,
            onBack = onBack,
            modifier = modifier,
            isLoading = state.isLoading,
            emptyIcon = Icons.Rounded.LibraryMusic,
            emptyTitle = "Playlist is empty",
            emptyMessage = "Add songs from any list using the three-dot menu.",
            topBarActions = {
                var menuOpen by remember { mutableStateOf(false) }
                IconButton(onClick = { menuOpen = true }) {
                    Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
                }
                DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                    DropdownMenuItem(
                        text = { Text("Rename") },
                        onClick = {
                            menuOpen = false
                            renaming = true
                        },
                    )
                    DropdownMenuItem(
                        text = { Text("Delete") },
                        onClick = {
                            menuOpen = false
                            deleting = true
                        },
                    )
                }
            },
            header = {
                if (playlist != null) {
                    CollectionHeader(
                        title = playlist.name,
                        subtitle = songsSummary(state.songs),
                        albumId = playlist.artworkAlbumId,
                        songUri = playlist.artworkSongUri,
                        icon = Icons.Rounded.LibraryMusic,
                    )
                }
            },
        )
    }

    if (renaming && playlist != null) {
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = playlist.name,
            onConfirm = onRename,
            onDismiss = { renaming = false },
        )
    }

    if (deleting && playlist != null) {
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onConfirm = {
                onDelete()
                onBack()
            },
            onDismiss = { deleting = false },
        )
    }
}
