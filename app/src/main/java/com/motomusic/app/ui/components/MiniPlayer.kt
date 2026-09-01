package com.motomusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.playback.TrackInfo

/**
 * The persistent player strip above the bottom navigation.
 *
 * Swiping it sideways stops playback and clears the queue, which is the only way the strip can
 * be dismissed — otherwise there would be no way back to it.
 *
 * [progress] is a lambda, not a value: the progress bar reads it while drawing, so the ticking
 * position redraws two pixels of line instead of recomposing the strip and everything above it.
 */
@Composable
fun MiniPlayer(
    track: TrackInfo?,
    isPlaying: Boolean,
    progress: () -> Float,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onExpand: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(
        visible = track != null,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier,
    ) {
        if (track == null) return@AnimatedVisibility

        val dismissState = rememberSwipeToDismissBoxState()

        // Clearing the queue drops the strip out of the composition, so there is nothing to
        // reset afterwards: the swipe simply ends the session.
        LaunchedEffect(dismissState.currentValue) {
            if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) onDismiss()
        }

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            Surface(
                tonalElevation = 3.dp,
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
                    .clip(RoundedCornerShape(16.dp)),
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onExpand)
                            .semantics { contentDescription = "Now playing: ${track.title}. Open player." }
                            .padding(horizontal = 8.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ArtworkImage(
                            albumId = track.albumId,
                            songUri = track.songUri,
                            size = 44.dp,
                            cornerRadius = 8.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.bodyMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = track.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        IconButton(onClick = onPlayPause) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                            )
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next song")
                        }
                    }

                    Box(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = Modifier.fillMaxWidth().height(2.dp),
                            trackColor = MaterialTheme.colorScheme.surfaceVariant,
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

/** Height reserved for the mini player so lists can pad their last item. */
val MiniPlayerHeight = 72.dp

/** Small fixed-size icon slot, keeping every control on a 48dp touch target. */
@Composable
fun ControlSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) { content() }
}
