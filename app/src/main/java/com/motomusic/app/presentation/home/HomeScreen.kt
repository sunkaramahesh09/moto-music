package com.motomusic.app.presentation.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.Song
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.ui.components.ArtworkTile
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.SectionHeader
import com.motomusic.app.ui.components.SongRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    contentPadding: PaddingValues,
    onOpenSearch: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSongs: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenFolders: () -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalSongActions.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(state.greeting, style = MaterialTheme.typography.titleLarge)
                        Text(
                            text = "What would you like to hear?",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSearch) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search your music")
                    }
                    IconButton(onClick = onOpenSettings) {
                        Icon(Icons.Rounded.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
    ) { innerPadding ->
        if (state.isEmpty) {
            EmptyState(
                icon = Icons.Rounded.MusicNote,
                title = "No music found",
                message = "Music stored on your device will appear here. Add some songs, then scan again.",
                actionLabel = "Scan for music",
                onAction = onRescan,
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 24.dp,
            ),
        ) {
            item(key = "quick-access") {
                QuickAccessGrid(
                    totalSongs = state.totalSongs,
                    favoriteCount = state.favoriteCount,
                    recentlyAddedCount = state.recentlyAdded.size,
                    mostPlayedCount = state.mostPlayed.size,
                    onOpenSongs = onOpenSongs,
                    onOpenFavorites = onOpenFavorites,
                    onOpenRecentlyAdded = onOpenRecentlyAdded,
                    onOpenMostPlayed = onOpenMostPlayed,
                    onOpenFolders = onOpenFolders,
                )
            }

            if (state.recentlyPlayed.isNotEmpty()) {
                songCarousel(
                    key = "recently-played",
                    title = "Recently played",
                    songs = state.recentlyPlayed,
                    onPlay = { index -> actions.play(state.recentlyPlayed, index) },
                )
            }

            if (state.mostPlayed.isNotEmpty()) {
                songCarousel(
                    key = "most-played",
                    title = "Frequently played",
                    songs = state.mostPlayed,
                    actionLabel = "See all",
                    onAction = onOpenMostPlayed,
                    onPlay = { index -> actions.play(state.mostPlayed, index) },
                )
            }

            if (state.recentlyAdded.isNotEmpty()) {
                item(key = "recently-added-header") {
                    SectionHeader(
                        title = "Recently added",
                        actionLabel = "See all",
                        onAction = onOpenRecentlyAdded,
                    )
                }
                itemsIndexed(
                    items = state.recentlyAdded.take(5),
                    key = { _, song -> song.id },
                ) { index, song ->
                    SongRow(song = song, onClick = { actions.play(state.recentlyAdded, index) })
                }
            }
        }
    }
}

/** A horizontal shelf of artwork tiles. */
private fun LazyListScope.songCarousel(
    key: String,
    title: String,
    songs: List<Song>,
    onPlay: (Int) -> Unit,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    item(key = "$key-header") {
        SectionHeader(title = title, actionLabel = actionLabel, onAction = onAction)
    }
    item(key = "$key-row") {
        LazyRow(
            contentPadding = PaddingValues(horizontal = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            itemsIndexed(items = songs, key = { _, song -> song.id }) { index, song ->
                ArtworkTile(
                    albumId = song.albumId,
                    songUri = song.uri,
                    title = song.title,
                    subtitle = song.artist,
                    onClick = { onPlay(index) },
                )
            }
        }
    }
}

@Composable
private fun QuickAccessGrid(
    totalSongs: Int,
    favoriteCount: Int,
    recentlyAddedCount: Int,
    mostPlayedCount: Int,
    onOpenSongs: () -> Unit,
    onOpenFavorites: () -> Unit,
    onOpenRecentlyAdded: () -> Unit,
    onOpenMostPlayed: () -> Unit,
    onOpenFolders: () -> Unit,
) {
    // Folders is deliberately always present: this grid is the only way into folder browsing,
    // and it used to be dropped as soon as "Most played" had something to show, which left the
    // Folders screen unreachable for anyone who had ever played a track.
    val shortcuts = buildList {
        add(Shortcut(Icons.Rounded.LibraryMusic, "All songs", pluralise(totalSongs, "song"), onOpenSongs))
        add(Shortcut(Icons.Rounded.Favorite, "Favourites", pluralise(favoriteCount, "song"), onOpenFavorites))
        add(
            Shortcut(
                icon = Icons.Rounded.NewReleases,
                title = "Recently added",
                subtitle = if (recentlyAddedCount > 0) "Fresh on this device" else "Nothing new yet",
                onClick = onOpenRecentlyAdded,
            ),
        )
        if (mostPlayedCount > 0) {
            add(
                Shortcut(
                    icon = Icons.AutoMirrored.Rounded.TrendingUp,
                    title = "Most played",
                    subtitle = "Your top tracks",
                    onClick = onOpenMostPlayed,
                ),
            )
        }
        add(Shortcut(Icons.Rounded.Folder, "Folders", "Browse by location", onOpenFolders))
    }

    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        shortcuts.chunked(2).forEachIndexed { index, row ->
            if (index > 0) Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                row.forEach { shortcut ->
                    QuickAccessCard(
                        icon = shortcut.icon,
                        title = shortcut.title,
                        subtitle = shortcut.subtitle,
                        onClick = shortcut.onClick,
                        modifier = Modifier.weight(1f),
                    )
                }
                // An odd number of shortcuts leaves the last card half width, not stretched.
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Immutable
private data class Shortcut(
    val icon: ImageVector,
    val title: String,
    val subtitle: String,
    val onClick: () -> Unit,
)

@Composable
private fun QuickAccessCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(22.dp),
            )
            Spacer(Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}
