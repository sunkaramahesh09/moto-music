package com.motomusic.app.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Song
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.ListActionRow
import com.motomusic.app.ui.components.SongList

/**
 * The shape shared by every screen that is "a title and a list of songs": album, artist, playlist,
 * folder, favourites and the home shortcuts. Only the header above the play/shuffle row differs.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CollectionScreen(
    title: String,
    songs: List<Song>,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    emptyIcon: ImageVector = Icons.Rounded.MusicNote,
    emptyTitle: String = "No songs here",
    emptyMessage: String = "Songs in this collection will show up here.",
    emptyActionLabel: String? = null,
    onEmptyAction: (() -> Unit)? = null,
    topBarActions: @Composable RowScope.() -> Unit = {},
    header: (@Composable () -> Unit)? = null,
    onSongClick: ((Int) -> Unit)? = null,
) {
    val actions = LocalSongActions.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = topBarActions,
            )
        },
    ) { innerPadding ->
        when {
            isLoading -> Unit

            songs.isEmpty() -> Column(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
                header?.invoke()
                EmptyState(
                    icon = emptyIcon,
                    title = emptyTitle,
                    message = emptyMessage,
                    actionLabel = emptyActionLabel,
                    onAction = onEmptyAction,
                )
            }

            else -> SongList(
                songs = songs,
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
                header = {
                    Column {
                        header?.invoke()
                        ListActionRow(
                            songCount = songs.size,
                            onPlay = { actions.play(songs, 0) },
                            onShuffle = { actions.shuffle(songs) },
                        )
                    }
                },
                onSongClick = onSongClick,
            )
        }
    }
}
