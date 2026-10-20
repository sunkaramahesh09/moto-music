package com.motomusic.app.presentation.home

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.playback.losslessFormat
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.ui.components.ArtworkTile
import com.motomusic.app.ui.components.CapsuleChip
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.GroupedCard
import com.motomusic.app.ui.components.HeaderIconButton
import com.motomusic.app.ui.components.LargeTitleHeader
import com.motomusic.app.ui.components.QualityBadge
import com.motomusic.app.ui.components.QuickAccessTile
import com.motomusic.app.ui.components.SearchPill
import com.motomusic.app.ui.components.SongRow
import com.motomusic.app.ui.components.TileTints

/**
 * The Library tab: a way into every part of the collection, then what is new and what gets
 * played most. It is assembled from small, bounded queries so opening the app never loads a
 * library of thousands of songs just to draw a few shelves.
 */
@Composable
fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenOutputs: () -> Unit,
    onOpenSongs: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalSongActions.current

    Column(modifier.fillMaxSize()) {
        LargeTitleHeader(title = "Library") {
            HeaderIconButton(
                icon = Icons.Rounded.SpeakerGroup,
                contentDescription = "Choose where music plays",
                onClick = onOpenOutputs,
            )
            HeaderIconButton(
                icon = Icons.Rounded.Settings,
                contentDescription = "Settings",
                onClick = onOpenSettings,
                filled = true,
            )
        }

        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "No music found",
                message = "Music stored on your device will appear here. Add some songs, then scan again.",
                actionLabel = "Scan for music",
                onAction = onRescan,
                modifier = Modifier.fillMaxSize(),
            )
            return@Column
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            item(key = "search") {
                SearchPill(placeholder = "Search in library…", onClick = onOpenSearch)
            }

            item(key = "filters") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    item { CapsuleChip("Playlists", onOpenPlaylists, icon = Icons.AutoMirrored.Rounded.QueueMusic) }
                    item { CapsuleChip("Artists", onOpenArtists, icon = Icons.Rounded.Person) }
                    item { CapsuleChip("Albums", onOpenAlbums, icon = Icons.Rounded.Album) }
                    item { CapsuleChip("Songs", onOpenSongs, icon = Icons.Rounded.MusicNote) }
                    item { CapsuleChip("Folders", onOpenFolders, icon = Icons.Rounded.Folder) }
                }
            }

            item(key = "quick-access") {
                QuickAccessGrid(
                    totalSongs = state.totalSongs,
                    favoriteCount = state.favoriteCount,
                    hasRecentlyAdded = state.recentlyAdded.isNotEmpty(),
                    hasMostPlayed = state.mostPlayed.isNotEmpty(),
                    onOpenSongs = onOpenSongs,
                    onOpenFavorites = onOpenFavorites,
                    onOpenRecentlyAdded = onOpenRecentlyAdded,
                    onOpenMostPlayed = onOpenMostPlayed,
                )
            }

            if (state.recentlyAdded.isNotEmpty()) {
                item(key = "recently-added-header") {
                    ShelfHeader(
                        title = "Recently added",
                        badge = if (state.recentlyAdded.any { it.losslessFormat != null }) "Hi-res" else null,
                        actionLabel = "See all",
                        onAction = onOpenRecentlyAdded,
                    )
                }
                item(key = "recently-added-row") {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        itemsIndexed(items = state.recentlyAdded, key = { _, song -> song.id }) { index, song ->
                            ArtworkTile(
                                albumId = song.albumId,
                                songUri = song.uri,
                                title = song.title,
                                subtitle = song.artist,
                                badge = song.losslessFormat?.let { "Lossless" },
                                onClick = { actions.play(state.recentlyAdded, index) },
                            )
                        }
                    }
                }
            }

            // Frequently played, or until anything has been played enough, what was played last.
            val (listTitle, listSongs, seeAll) = when {
                state.mostPlayed.isNotEmpty() -> Triple("Most played", state.mostPlayed, onOpenMostPlayed)
                else -> Triple("Played lately", state.recentlyPlayed, null)
            }
            if (listSongs.isNotEmpty()) {
                item(key = "list-header") {
                    ShelfHeader(
                        title = listTitle,
                        onShuffle = { actions.shuffle(listSongs) },
                        actionLabel = if (seeAll != null) "See all" else null,
                        onAction = seeAll,
                    )
                }
                itemsIndexed(
                    items = listSongs.take(LIST_LIMIT),
                    key = { _, song -> "list-${song.id}" },
                ) { index, song ->
                    SongRow(song = song, onClick = { actions.play(listSongs, index) })
                }
            }

            item(key = "summary") {
                Spacer(Modifier.height(20.dp))
                LibrarySummaryCard(totalSongs = state.totalSongs, onRescan = onRescan)
            }
        }
    }
}

private const val LIST_LIMIT = 6

/** A section title with an optional quality badge, shuffle button and "See all". */
@Composable
private fun ShelfHeader(
    title: String,
    badge: String? = null,
    onShuffle: (() -> Unit)? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 16.dp, end = 4.dp, top = 24.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (badge != null) {
            Spacer(Modifier.width(8.dp))
            QualityBadge(text = badge, accent = true)
        }
        Spacer(Modifier.weight(1f))
        if (onShuffle != null) {
            IconButton(onClick = onShuffle) {
                Icon(
                    Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle $title",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (actionLabel != null && onAction != null) {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

@Composable
private fun QuickAccessGrid(
    totalSongs: Int,
    favoriteCount: Int,
    hasRecentlyAdded: Boolean,
    hasMostPlayed: Boolean,
    onOpenSongs: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    onOpenMostPlayed: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile(
                icon = Icons.Rounded.MusicNote,
                tint = TileTints.Blue,
                title = "All songs",
                subtitle = pluralise(totalSongs, "song"),
                onClick = onOpenSongs,
                modifier = Modifier.weight(1f),
            )
            QuickAccessTile(
                icon = Icons.Rounded.Favorite,
                tint = TileTints.Rose,
                title = "Favourites",
                subtitle = pluralise(favoriteCount, "song"),
                onClick = onOpenFavorites,
                modifier = Modifier.weight(1f),
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            QuickAccessTile(
                icon = Icons.Rounded.Schedule,
                tint = TileTints.Teal,
                title = "Recently added",
                subtitle = if (hasRecentlyAdded) "Newest first" else "Nothing new yet",
                onClick = onOpenRecentlyAdded,
                modifier = Modifier.weight(1f),
            )
            QuickAccessTile(
                icon = Icons.AutoMirrored.Rounded.TrendingUp,
                tint = TileTints.Purple,
                title = "Most played",
                subtitle = if (hasMostPlayed) "Heavy rotation" else "Play something first",
                onClick = onOpenMostPlayed,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/** Where the music lives: everything is on this phone, which is also why there is a rescan. */
@Composable
private fun LibrarySummaryCard(totalSongs: Int, onRescan: () -> Unit) {
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
                Text("On this device", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "${pluralise(totalSongs, "song")} · nothing leaves your phone",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Spacer(Modifier.width(8.dp))
            CapsuleChip(label = "Rescan", onClick = onRescan)
        }
    }
}
