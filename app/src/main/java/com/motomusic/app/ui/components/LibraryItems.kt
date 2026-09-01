package com.motomusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.domain.model.Playlist

/** Grid tile for an album. Artwork is decoded at the tile size, never full resolution. */
@Composable
fun AlbumGridItem(
    album: Album,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics {
                contentDescription =
                    "${album.name} by ${album.artist}, ${pluralise(album.songCount, "song")}"
            }
            .padding(8.dp),
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(1f)) {
            LargeArtworkImage(
                albumId = album.id,
                songUri = album.artworkSongUri,
                sizeHint = 256.dp,
                cornerRadius = 12.dp,
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            text = album.name,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = album.artist,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = pluralise(album.songCount, "song"),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            maxLines = 1,
        )
    }
}

/** Horizontal carousel tile used on the home screen. */
@Composable
fun ArtworkTile(
    albumId: Long,
    songUri: String?,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .width(148.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .semantics { contentDescription = "$title, $subtitle" }
            .padding(6.dp),
    ) {
        LargeArtworkImage(
            albumId = albumId,
            songUri = songUri,
            sizeHint = 256.dp,
            cornerRadius = 12.dp,
            modifier = Modifier.aspectRatio(1f),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyMedium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
fun ArtistRow(
    artist: Artist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryListRow(
        title = artist.name,
        subtitle = buildString {
            append(pluralise(artist.songCount, "song"))
            if (artist.albumCount > 0) append(" • ${pluralise(artist.albumCount, "album")}")
        },
        onClick = onClick,
        modifier = modifier,
    ) {
        // Artists have no artwork of their own in MediaStore, so one of their album covers is used.
        ArtworkImage(
            albumId = artist.artworkAlbumId,
            songUri = artist.artworkSongUri,
            size = 52.dp,
            cornerRadius = 26.dp,
        )
    }
}

@Composable
fun PlaylistRow(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailing: @Composable (() -> Unit)? = null,
) {
    LibraryListRow(
        title = playlist.name,
        subtitle = pluralise(playlist.songCount, "song"),
        onClick = onClick,
        modifier = modifier,
        trailing = trailing,
    ) {
        if (playlist.artworkAlbumId != null) {
            ArtworkImage(
                albumId = playlist.artworkAlbumId,
                songUri = playlist.artworkSongUri,
                size = 52.dp,
            )
        } else {
            RoundIcon(Icons.AutoMirrored.Rounded.QueueMusic)
        }
    }
}

@Composable
fun FolderRow(
    folder: Folder,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    LibraryListRow(
        title = folder.name,
        subtitle = folder.path,
        onClick = onClick,
        modifier = modifier,
        trailingText = folder.songCount.toString(),
    ) {
        RoundIcon(Icons.Rounded.Folder)
    }
}

@Composable
fun RoundIcon(icon: ImageVector, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun AvatarIcon(modifier: Modifier = Modifier, icon: ImageVector = Icons.Rounded.Person) {
    Box(
        modifier = modifier
            .size(52.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LibraryListRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    trailingText: String? = null,
    trailing: @Composable (() -> Unit)? = null,
    leading: @Composable () -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Box(Modifier.clearAndSetSemantics {}) { leading() }
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        trailing?.invoke()
    }
}

/** Placeholder square used where a list has no artwork at all. */
@Composable
fun EmptyArtworkBox(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surfaceVariant)
    )
}
