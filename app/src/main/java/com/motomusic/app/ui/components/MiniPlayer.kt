package com.motomusic.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.playback.TrackInfo

/**
 * The persistent player strip above the bottom navigation.
 *
 * Swiping it up opens the full player, the counterpart of swiping the player down to close it.
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
    onPrevious: () -> Unit,
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

        val density = LocalDensity.current
        val expandDistance = with(density) { SWIPE_UP_DISTANCE.toPx() }
        val expandVelocity = with(density) { SWIPE_UP_VELOCITY.toPx() }
        val dragTotal = remember { mutableFloatStateOf(0f) }

        SwipeToDismissBox(
            state = dismissState,
            backgroundContent = {},
            modifier = Modifier.fillMaxWidth(),
        ) {
            // A floating glass dock: rounded, translucent, hairline-edged, with the progress
            // line running along its bottom edge.
            val shape = RoundedCornerShape(DOCK_RADIUS)
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainer.copy(alpha = GLASS_ALPHA),
                shape = shape,
                shadowElevation = 8.dp,
                border = BorderStroke(0.5.dp, hairlineColor()),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Box {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .clickable(onClick = onExpand)
                            .draggable(
                                state = rememberDraggableState { dragTotal.floatValue += it },
                                orientation = Orientation.Vertical,
                                onDragStarted = { dragTotal.floatValue = 0f },
                                onDragStopped = { velocity ->
                                    if (dragTotal.floatValue < -expandDistance || velocity < -expandVelocity) onExpand()
                                },
                            )
                            .semantics { contentDescription = "Now playing: ${track.title}. Open player." }
                            .padding(start = 10.dp, end = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // 10dp inset inside a 22dp dock: 12dp corners keep the curves concentric.
                        ArtworkImage(
                            albumId = track.albumId,
                            songUri = track.songUri,
                            size = 44.dp,
                            cornerRadius = 12.dp,
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                style = MaterialTheme.typography.titleSmall,
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
                        IconButton(onClick = onPrevious) {
                            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song")
                        }
                        // The play button is the one solid disc on the dock: ink on light, white on dark.
                        IconButton(onClick = onPlayPause) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.onSurface),
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = MaterialTheme.colorScheme.surface,
                                    modifier = Modifier.size(22.dp),
                                )
                            }
                        }
                        IconButton(onClick = onNext) {
                            Icon(Icons.Rounded.SkipNext, contentDescription = "Next song")
                        }
                    }

                    LinearProgressIndicator(
                        progress = progress,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .fillMaxWidth()
                            .height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = Color.Transparent,
                        gapSize = 0.dp,
                        drawStopIndicator = {},
                    )
                }
            }
        }
    }
}

private val DOCK_RADIUS = 22.dp
private val SWIPE_UP_DISTANCE = 32.dp
private val SWIPE_UP_VELOCITY = 600.dp

/** Height reserved for the mini player so lists can pad their last item. */
val MiniPlayerHeight = 72.dp

/** Small fixed-size icon slot, keeping every control on a 48dp touch target. */
@Composable
fun ControlSlot(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) { content() }
}
