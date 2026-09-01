package com.motomusic.app.presentation.queue

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.formatDuration
import com.motomusic.app.playback.PlaybackUiState
import com.motomusic.app.playback.TrackInfo
import com.motomusic.app.ui.components.ArtworkImage
import com.motomusic.app.ui.components.EmptyState

/**
 * The playing queue.
 *
 * Reordering is done with explicit up/down buttons rather than drag-and-drop: it is reachable
 * with a screen reader, and it survives the list being rebuilt under the finger when the queue
 * advances to the next song.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueScreen(
    playback: PlaybackUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onPlayIndex: (Int) -> Unit,
    onRemove: (Int) -> Unit,
    onMove: (Int, Int) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    // Open on whatever is playing, which is rarely the top of a long queue.
    LaunchedEffect(playback.queueIndex) {
        if (playback.queue.isNotEmpty()) {
            listState.scrollToItem(playback.queueIndex.coerceIn(0, playback.queue.lastIndex))
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Playing queue") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (playback.queue.isNotEmpty()) {
                        IconButton(onClick = onClear) {
                            Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear queue")
                        }
                    }
                },
            )
        },
    ) { innerPadding ->
        if (playback.queue.isEmpty()) {
            EmptyState(
                icon = Icons.AutoMirrored.Rounded.QueueMusic,
                title = "Queue is empty",
                message = "Play something from your library and it will line up here.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )
            return@Scaffold
        }

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                top = innerPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding() + 16.dp,
            ),
        ) {
            itemsIndexed(
                items = playback.queue,
                key = { index, track -> "$index-${track.songId}" },
            ) { index, track ->
                QueueRow(
                    track = track,
                    isCurrent = index == playback.queueIndex,
                    canMoveUp = index > 0,
                    canMoveDown = index < playback.queue.lastIndex,
                    onClick = { onPlayIndex(index) },
                    onRemove = { onRemove(index) },
                    onMoveUp = { onMove(index, index - 1) },
                    onMoveDown = { onMove(index, index + 1) },
                )
            }
        }
    }
}

@Composable
private fun QueueRow(
    track: TrackInfo,
    isCurrent: Boolean,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onClick: () -> Unit,
    onRemove: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
) {
    val background = if (isCurrent) {
        MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(background)
            .clickable(onClick = onClick)
            .padding(start = 16.dp, end = 4.dp, top = 6.dp, bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ArtworkImage(albumId = track.albumId, songUri = track.songUri, size = 44.dp)
        Spacer(Modifier.width(12.dp))

        Column(Modifier.weight(1f)) {
            Text(
                text = track.title,
                style = MaterialTheme.typography.bodyLarge,
                color = if (isCurrent) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${track.artist} • ${formatDuration(track.durationMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }

        IconButton(onClick = onMoveUp, enabled = canMoveUp) {
            Icon(Icons.Rounded.KeyboardArrowUp, contentDescription = "Move ${track.title} up")
        }
        IconButton(onClick = onMoveDown, enabled = canMoveDown) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Move ${track.title} down")
        }
        IconButton(onClick = onRemove) {
            Icon(Icons.Rounded.Close, contentDescription = "Remove ${track.title} from queue")
        }
    }
}
