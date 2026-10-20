package com.motomusic.app.presentation.browse

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Casino
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.Album
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.ui.components.AlbumGridItem
import com.motomusic.app.ui.components.ArtworkTile
import com.motomusic.app.ui.components.CapsuleChip
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.GroupedCard
import com.motomusic.app.ui.components.HeaderIconButton
import com.motomusic.app.ui.components.LargeArtworkImage
import com.motomusic.app.ui.components.LargeTitleHeader
import com.motomusic.app.ui.components.QualityBadge
import com.motomusic.app.ui.components.QuickAccessTile
import com.motomusic.app.ui.components.SectionHeader
import com.motomusic.app.ui.components.SongRow
import com.motomusic.app.ui.components.TileTints
import com.motomusic.app.ui.theme.TabularFigures

/** The Browse tab: ready-made ways to start listening, all drawn from the user's own music. */
@Composable
fun BrowseScreen(
    state: BrowseUiState,
    contentPadding: PaddingValues,
    onShuffleEverything: () -> Unit,
    onPlayMix: (QuickMix) -> Unit,
    onAlbumClick: (Album) -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenOutputs: () -> Unit,
    onOpenSettings: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalSongActions.current

    Column(modifier.fillMaxSize()) {
        LargeTitleHeader(title = "Browse") {
            HeaderIconButton(Icons.Rounded.SpeakerGroup, "Choose where music plays", onOpenOutputs)
            HeaderIconButton(Icons.Rounded.Settings, "Settings", onOpenSettings, filled = true)
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "Nothing to browse yet",
                message = "Once there is music on this phone, mixes and your most played songs appear here.",
                actionLabel = "Scan for music",
                onAction = onRescan,
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = 4.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item(key = "hero") {
                ShuffleHeroCard(
                    album = state.heroAlbum,
                    totalSongs = state.totalSongs,
                    onListen = onShuffleEverything,
                )
            }

            item(key = "mixes-header") { SectionHeader(title = "Quick mixes") }
            item(key = "mixes") { QuickMixGrid(onPlayMix) }

            if (state.newAlbums.isNotEmpty()) {
                item(key = "albums-header") {
                    SectionHeader(title = "New on this device", actionLabel = "All albums", onAction = onOpenAlbums)
                }
                item(key = "albums") {
                    Column(Modifier.padding(horizontal = 10.dp)) {
                        state.newAlbums.chunked(2).forEach { row ->
                            Row {
                                row.forEach { album ->
                                    AlbumGridItem(
                                        album = album,
                                        onClick = { onAlbumClick(album) },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                                if (row.size == 1) Spacer(Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            if (state.topSongs.isNotEmpty()) {
                item(key = "top-header") {
                    SectionHeader(title = "Your top songs", actionLabel = "See all", onAction = onOpenMostPlayed)
                }
                item(key = "top") {
                    GroupedCard {
                        state.topSongs.forEachIndexed { index, song ->
                            SongRow(
                                song = song,
                                onClick = { actions.play(state.topSongs, index) },
                                leading = {
                                    Text(
                                        text = "${index + 1}",
                                        style = MaterialTheme.typography.titleMedium.merge(TabularFigures),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.widthIn(min = 16.dp),
                                    )
                                },
                            )
                        }
                    }
                }
            }

            if (state.recentlyPlayed.isNotEmpty()) {
                item(key = "recent-header") { SectionHeader(title = "Recently played") }
                item(key = "recent") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(state.recentlyPlayed, key = { _, song -> song.id }) { index, song ->
                            ArtworkTile(
                                albumId = song.albumId,
                                songUri = song.uri,
                                title = song.title,
                                subtitle = song.artist,
                                onClick = { actions.play(state.recentlyPlayed, index) },
                            )
                        }
                    }
                }
            }

            item(key = "sound") {
                Spacer(Modifier.height(24.dp))
                SoundCard(onConfigure = onOpenEqualizer)
            }
        }
    }
}

/**
 * The editorial hero of the design, doing the one thing an offline library can always offer:
 * everything, shuffled. Dressed in the newest album's cover under a dark ramp for legibility.
 */
@Composable
private fun ShuffleHeroCard(album: Album?, totalSongs: Int, onListen: () -> Unit) {
    val shape = RoundedCornerShape(28.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .height(236.dp)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        if (album != null) {
            LargeArtworkImage(
                albumId = album.id,
                songUri = album.artworkSongUri,
                sizeHint = 360.dp,
                cornerRadius = 0.dp,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.05f),
                        0.45f to Color.Black.copy(alpha = 0.35f),
                        1f to Color.Black.copy(alpha = 0.85f),
                    ),
                ),
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
        ) {
            QualityBadge(text = "Your library", onArtwork = true)
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Shuffle everything",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White,
            )
            Text(
                text = "${pluralise(totalSongs, "song")}, in a new order every time",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(14.dp))
            Button(
                onClick = onListen,
                shape = CircleShape,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                contentPadding = PaddingValues(horizontal = 22.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(6.dp))
                Text("Listen now")
            }
        }
    }
}

@Composable
private fun QuickMixGrid(onPlayMix: (QuickMix) -> Unit) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile(
                icon = Icons.Rounded.Favorite,
                tint = TileTints.Rose,
                title = "Loved",
                subtitle = "Your favourites",
                onClick = { onPlayMix(QuickMix.FAVORITES) },
                modifier = Modifier.weight(1f),
            )
            QuickAccessTile(
                icon = Icons.Rounded.Schedule,
                tint = TileTints.Teal,
                title = "Fresh",
                subtitle = "Newest additions",
                onClick = { onPlayMix(QuickMix.RECENTLY_ADDED) },
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                tint = TileTints.Purple,
                title = "On repeat",
                subtitle = "Your most played",
                onClick = { onPlayMix(QuickMix.MOST_PLAYED) },
                modifier = Modifier.weight(1f),
            )
            QuickAccessTile(
                icon = Icons.Rounded.Casino,
                tint = TileTints.Orange,
                title = "Lucky dip",
                subtitle = "A random album",
                onClick = { onPlayMix(QuickMix.RANDOM_ALBUM) },
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** The design's "Studio Master Audio" card, pointing at the sound settings the app really has. */
@Composable
private fun SoundCard(onConfigure: () -> Unit) {
    GroupedCard {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text("Studio sound", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Equalizer, and lossless files played at full resolution",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            CapsuleChip(label = "Configure", onClick = onConfigure)
        }
    }
}
