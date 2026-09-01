package com.motomusic.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.formatDuration
import com.motomusic.app.domain.model.Song
import com.motomusic.app.presentation.common.LocalFavoriteIds
import com.motomusic.app.presentation.common.LocalNowPlayingId
import com.motomusic.app.presentation.common.LocalSongActions

/**
 * One song in a list.
 *
 * Kept deliberately cheap: the only state it reads is the now-playing id and the favourites
 * set, both of which change rarely, so scrolling a library of thousands stays smooth.
 */
@Composable
fun SongRow(
    song: Song,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    trailingText: String? = null,
) {
    val actions = LocalSongActions.current
    val isPlaying = LocalNowPlayingId.current == song.id
    val isFavorite = song.id in LocalFavoriteIds.current

    val titleColor = if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showArtwork) {
            ArtworkImage(
                albumId = song.albumId,
                songUri = song.uri,
                size = 52.dp,
                cornerRadius = 10.dp,
            )
            Spacer(Modifier.width(14.dp))
        }

        Column(Modifier.weight(1f)) {
            Text(
                text = song.title,
                style = MaterialTheme.typography.bodyLarge,
                color = titleColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = songSubtitle(song),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        Spacer(Modifier.width(8.dp))

        if (isFavorite) {
            Icon(
                imageVector = Icons.Rounded.Favorite,
                contentDescription = "Favourite",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(8.dp))
        }

        Text(
            text = trailingText ?: formatDuration(song.durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        IconButton(onClick = { actions.openMenu(song) }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "More options for ${song.title}",
            )
        }
    }
}

/** "Artist • Album", collapsing gracefully when one of the tags is missing. */
fun songSubtitle(song: Song): String = listOf(song.artist, song.album)
    .filter { it.isNotBlank() }
    .joinToString(" • ")

/** A thin divider-free separator used between grouped rows. */
@Composable
fun RowSpacer(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
    )
}

/** Compact header used above a list of songs, with play/shuffle affordances. */
@Composable
fun ListActionRow(
    songCount: Int,
    onPlay: () -> Unit,
    onShuffle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlayButton(onClick = onPlay, enabled = songCount > 0, modifier = Modifier.weight(1f))
        ShuffleButton(onClick = onShuffle, enabled = songCount > 0, modifier = Modifier.weight(1f))
    }
}
