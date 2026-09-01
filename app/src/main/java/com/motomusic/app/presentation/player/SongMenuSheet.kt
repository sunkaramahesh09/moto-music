package com.motomusic.app.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlaylistRemove
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Song
import com.motomusic.app.ui.components.ArtworkImage
import com.motomusic.app.ui.components.songSubtitle

/** The three-dot menu shown for a song anywhere in the app. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongMenuSheet(
    target: SongMenuTarget,
    isFavorite: Boolean,
    onDismiss: () -> Unit,
    onPlayNext: (Song) -> Unit,
    onAddToQueue: (Song) -> Unit,
    onToggleFavorite: (Song) -> Unit,
    onAddToPlaylist: (Song) -> Unit,
    onRemoveFromPlaylist: (Song, Long) -> Unit,
    onGoToAlbum: (Song) -> Unit,
    onGoToArtist: (Song) -> Unit,
    onShowInfo: (Song) -> Unit,
    onHideFromLibrary: (Song) -> Unit,
) {
    val song = target.song
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(bottom = 24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ArtworkImage(albumId = song.albumId, songUri = song.uri, size = 52.dp)
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text = song.title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = songSubtitle(song),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }

            Spacer(Modifier.height(8.dp))

            MenuAction(
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                label = "Play next",
                onClick = {
                    onPlayNext(song)
                    onDismiss()
                },
            )
            MenuAction(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                label = "Add to queue",
                onClick = {
                    onAddToQueue(song)
                    onDismiss()
                },
            )
            MenuAction(
                icon = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = if (isFavorite) "Remove from favourites" else "Add to favourites",
                onClick = {
                    onToggleFavorite(song)
                    onDismiss()
                },
            )
            MenuAction(
                icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                label = "Add to playlist",
                onClick = { onAddToPlaylist(song) },
            )
            if (target.playlistId != null) {
                MenuAction(
                    icon = Icons.Rounded.PlaylistRemove,
                    label = "Remove from this playlist",
                    onClick = {
                        onRemoveFromPlaylist(song, target.playlistId)
                        onDismiss()
                    },
                )
            }
            MenuAction(
                icon = Icons.Rounded.Album,
                label = "Go to album",
                onClick = {
                    onGoToAlbum(song)
                    onDismiss()
                },
            )
            MenuAction(
                icon = Icons.Rounded.Person,
                label = "Go to artist",
                onClick = {
                    onGoToArtist(song)
                    onDismiss()
                },
            )
            MenuAction(
                icon = Icons.Rounded.Info,
                label = "Song details",
                onClick = { onShowInfo(song) },
            )
            MenuAction(
                icon = Icons.Rounded.VisibilityOff,
                label = "Hide from library",
                onClick = {
                    onHideFromLibrary(song)
                    onDismiss()
                },
            )
        }
    }
}

@Composable
private fun MenuAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.width(20.dp))
        Text(label, style = MaterialTheme.typography.bodyLarge)
    }
}
