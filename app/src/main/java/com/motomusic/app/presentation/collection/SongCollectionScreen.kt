package com.motomusic.app.presentation.collection

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.automirrored.rounded.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import com.motomusic.app.presentation.common.CollectionScreen
import com.motomusic.app.presentation.common.songsSummary
import com.motomusic.app.ui.components.CollectionHeader

@Composable
fun SongCollectionScreen(
    state: SongCollectionUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val collection = state.collection
    val icon = collection.icon()

    CollectionScreen(
        title = collection.title,
        songs = state.songs,
        contentPadding = contentPadding,
        onBack = onBack,
        modifier = modifier,
        isLoading = state.isLoading,
        emptyIcon = icon,
        emptyTitle = collection.emptyTitle(),
        emptyMessage = collection.emptyMessage(),
        topBarActions = {
            if (collection.isHistory && state.songs.isNotEmpty()) {
                IconButton(onClick = onClearHistory) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear listening history")
                }
            }
        },
        header = {
            CollectionHeader(
                title = collection.title,
                subtitle = songsSummary(state.songs),
                icon = icon,
            )
        },
    )
}

private val SongCollection.isHistory: Boolean
    get() = this == SongCollection.RECENTLY_PLAYED || this == SongCollection.MOST_PLAYED

private fun SongCollection.icon(): ImageVector = when (this) {
    SongCollection.FAVORITES -> Icons.Rounded.Favorite
    SongCollection.RECENTLY_PLAYED -> Icons.Rounded.History
    SongCollection.MOST_PLAYED -> Icons.AutoMirrored.Rounded.TrendingUp
    SongCollection.RECENTLY_ADDED -> Icons.Rounded.NewReleases
}

private fun SongCollection.emptyTitle(): String = when (this) {
    SongCollection.FAVORITES -> "No favourites yet"
    SongCollection.RECENTLY_PLAYED -> "Nothing played yet"
    SongCollection.MOST_PLAYED -> "Nothing played yet"
    SongCollection.RECENTLY_ADDED -> "Nothing new"
}

private fun SongCollection.emptyMessage(): String = when (this) {
    SongCollection.FAVORITES -> "Tap the heart in a song's menu to keep it here."
    SongCollection.RECENTLY_PLAYED -> "Songs you listen to show up here, newest first."
    SongCollection.MOST_PLAYED -> "Play a few songs and your most-played ones collect here."
    SongCollection.RECENTLY_ADDED -> "Music copied onto this device appears here after a scan."
}
