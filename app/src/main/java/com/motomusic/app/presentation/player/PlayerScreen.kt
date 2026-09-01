package com.motomusic.app.presentation.player

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.formatDuration
import com.motomusic.app.playback.PlaybackPosition
import com.motomusic.app.playback.PlaybackUiState
import com.motomusic.app.playback.RepeatMode
import com.motomusic.app.playback.SleepTimerState
import com.motomusic.app.ui.components.LargeArtworkImage
import kotlinx.coroutines.delay

/**
 * The full-screen player.
 *
 * The playing position arrives as a [State] rather than a value, and is read only inside
 * [SeekSection]. That keeps the twice-a-second position tick — and every frame of a drag —
 * out of the artwork, the titles and the transport row, which would otherwise all recompose
 * along with the seek bar.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlayerScreen(
    playback: PlaybackUiState,
    position: State<PlaybackPosition>,
    isFavorite: Boolean,
    sleepTimer: SleepTimerState,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onToggleFavorite: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMenu: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.current

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = if (playback.queue.isEmpty()) {
                            "Now playing"
                        } else {
                            "Playing ${playback.queueIndex + 1} of ${playback.queue.size}"
                        },
                        style = MaterialTheme.typography.labelLarge,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse player")
                    }
                },
                actions = {
                    IconButton(onClick = onOpenSleepTimer) {
                        Icon(
                            imageVector = Icons.Rounded.Bedtime,
                            contentDescription = "Sleep timer",
                            tint = if (sleepTimer is SleepTimerState.Off) {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        )
                    }
                    IconButton(onClick = onOpenQueue) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Playing queue")
                    }
                    IconButton(onClick = onOpenMenu, enabled = track != null) {
                        Icon(Icons.Rounded.MoreVert, contentDescription = "More options")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            LargeArtworkImage(
                albumId = track?.albumId ?: 0L,
                songUri = track?.songUri,
                sizeHint = 320.dp,
                cornerRadius = 24.dp,
                modifier = Modifier.fillMaxWidth().widthIn(max = 380.dp).aspectRatio(1f),
            )

            Spacer(Modifier.height(28.dp))

            Text(
                text = track?.title ?: "Nothing playing",
                style = MaterialTheme.typography.headlineSmall,
                textAlign = TextAlign.Center,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(6.dp))
            Text(
                text = track?.artist ?: "Pick a song from your library",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(20.dp))

            SeekSection(
                position = position,
                trackDurationMs = track?.durationMs ?: 0L,
                enabled = track != null,
                onSeek = onSeek,
            )

            Spacer(Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ToggleIconButton(
                    icon = Icons.Rounded.Shuffle,
                    contentDescription = if (playback.shuffleEnabled) "Shuffle on" else "Shuffle off",
                    active = playback.shuffleEnabled,
                    onClick = onToggleShuffle,
                )

                IconButton(onClick = onPrevious, enabled = track != null, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipPrevious,
                        contentDescription = "Previous song",
                        modifier = Modifier.size(40.dp),
                    )
                }

                FilledIconButton(
                    onClick = onPlayPause,
                    enabled = track != null,
                    modifier = Modifier.size(76.dp),
                    colors = IconButtonDefaults.filledIconButtonColors(),
                ) {
                    Icon(
                        imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = if (playback.isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }

                IconButton(onClick = onNext, enabled = track != null, modifier = Modifier.size(56.dp)) {
                    Icon(
                        imageVector = Icons.Rounded.SkipNext,
                        contentDescription = "Next song",
                        modifier = Modifier.size(40.dp),
                    )
                }

                ToggleIconButton(
                    icon = when (playback.repeatMode) {
                        RepeatMode.ONE -> Icons.Rounded.RepeatOne
                        else -> Icons.Rounded.Repeat
                    },
                    contentDescription = playback.repeatMode.label,
                    active = playback.repeatMode != RepeatMode.OFF,
                    onClick = onCycleRepeat,
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onToggleFavorite, enabled = track != null) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove from favourites" else "Add to favourites",
                        tint = if (isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }

                val timer = sleepTimer
                if (timer is SleepTimerState.Countdown) {
                    Text(
                        text = "Sleeping in ${formatDuration(timer.remainingMs)}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(onClick = onOpenSleepTimer),
                    )
                } else if (timer is SleepTimerState.EndOfTrack) {
                    Text(
                        text = "Stopping after this track",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .clickable(onClick = onOpenSleepTimer),
                    )
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

/**
 * The seek bar and the two time labels — the only part of the player that follows the clock.
 *
 * While a drag is in progress the thumb follows the finger rather than the player, and for a
 * moment after the finger lifts it keeps showing the requested position: the player reports
 * where it used to be until the seek lands, and following that would snap the thumb backwards.
 */
@Composable
private fun SeekSection(
    position: State<PlaybackPosition>,
    trackDurationMs: Long,
    enabled: Boolean,
    onSeek: (Long) -> Unit,
) {
    var scrubbing by remember { mutableStateOf(false) }
    var scrubFraction by remember { mutableFloatStateOf(0f) }
    var requestedMs by remember { mutableLongStateOf(NO_REQUESTED_SEEK) }

    LaunchedEffect(requestedMs) {
        if (requestedMs == NO_REQUESTED_SEEK) return@LaunchedEffect
        delay(SEEK_SETTLE_MS)
        requestedMs = NO_REQUESTED_SEEK
    }

    val current = position.value
    // Media3 reports 0 for an unprepared item; the track's own duration is the better guess.
    val durationMs = if (current.durationMs > 0L) current.durationMs else trackDurationMs
    val fraction = when {
        scrubbing -> scrubFraction
        durationMs <= 0L -> 0f
        requestedMs != NO_REQUESTED_SEEK -> (requestedMs.toFloat() / durationMs).coerceIn(0f, 1f)
        else -> (current.positionMs.toFloat() / durationMs).coerceIn(0f, 1f)
    }

    Slider(
        value = fraction,
        onValueChange = { value ->
            scrubbing = true
            scrubFraction = value
        },
        onValueChangeFinished = {
            val target = (scrubFraction * durationMs).toLong()
            scrubbing = false
            requestedMs = target
            onSeek(target)
        },
        enabled = enabled && durationMs > 0L,
        modifier = Modifier.fillMaxWidth(),
    )

    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = formatDuration((fraction * durationMs).toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = formatDuration(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** No seek is waiting to land. Positions are never negative, so this cannot collide. */
private const val NO_REQUESTED_SEEK = -1L

/** How long the thumb holds the requested position before it follows the player again. */
private const val SEEK_SETTLE_MS = 400L

@Composable
private fun ToggleIconButton(
    icon: ImageVector,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
