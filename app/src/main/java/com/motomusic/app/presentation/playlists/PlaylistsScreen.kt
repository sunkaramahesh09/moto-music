package com.motomusic.app.presentation.playlists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.PlaylistRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistsScreen(
    state: PlaylistsUiState,
    contentPadding: PaddingValues,
    onPlaylistClick: (Playlist) -> Unit,
    onCreate: (String) -> Unit,
    onRename: (Long, String) -> Unit,
    onDelete: (Long) -> Unit,
    modifier: Modifier = Modifier,
) {
    var createOpen by remember { mutableStateOf(false) }
    var renaming by remember { mutableStateOf<Playlist?>(null) }
    var deleting by remember { mutableStateOf<Playlist?>(null) }

    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Playlists") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = { createOpen = true },
                icon = { Icon(Icons.Rounded.Add, contentDescription = null) },
                text = { Text("New playlist") },
                modifier = Modifier.padding(bottom = contentPadding.calculateBottomPadding()),
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Unit

            state.playlists.isEmpty() -> EmptyState(
                icon = Icons.Rounded.LibraryMusic,
                title = "No playlists yet",
                message = "Group the songs you like into a playlist. They live only on this device.",
                actionLabel = "New playlist",
                onAction = { createOpen = true },
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    // Clears both the mini player and the floating action button.
                    bottom = contentPadding.calculateBottomPadding() + 88.dp,
                ),
            ) {
                items(items = state.playlists, key = { it.id }) { playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        onClick = { onPlaylistClick(playlist) },
                        trailing = {
                            PlaylistOverflowMenu(
                                onRename = { renaming = playlist },
                                onDelete = { deleting = playlist },
                            )
                        },
                    )
                }
            }
        }
    }

    if (createOpen) {
        PlaylistNameDialog(
            title = "New playlist",
            confirmLabel = "Create",
            onConfirm = onCreate,
            onDismiss = { createOpen = false },
        )
    }

    renaming?.let { playlist ->
        PlaylistNameDialog(
            title = "Rename playlist",
            confirmLabel = "Rename",
            initialName = playlist.name,
            onConfirm = { name -> onRename(playlist.id, name) },
            onDismiss = { renaming = null },
        )
    }

    deleting?.let { playlist ->
        DeletePlaylistDialog(
            playlistName = playlist.name,
            onConfirm = { onDelete(playlist.id) },
            onDismiss = { deleting = null },
        )
    }
}

@Composable
private fun PlaylistOverflowMenu(
    onRename: () -> Unit,
    onDelete: () -> Unit,
) {
    var open by remember { mutableStateOf(false) }

    IconButton(onClick = { open = true }) {
        Icon(Icons.Rounded.MoreVert, contentDescription = "Playlist options")
    }
    DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
        DropdownMenuItem(
            text = { Text("Rename") },
            onClick = {
                open = false
                onRename()
            },
        )
        DropdownMenuItem(
            text = { Text("Delete") },
            onClick = {
                open = false
                onDelete()
            },
        )
    }
}
