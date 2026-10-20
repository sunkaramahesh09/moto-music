package com.motomusic.app.presentation.player

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.formatDuration
import com.motomusic.app.playback.AudioOutput
import com.motomusic.app.playback.PlaybackPosition
import com.motomusic.app.playback.PlaybackUiState
import com.motomusic.app.playback.RepeatMode
import com.motomusic.app.playback.SleepTimerState
import com.motomusic.app.presentation.audio.AudioOutputChip
import com.motomusic.app.ui.components.HeartButton
import com.motomusic.app.ui.components.LargeArtworkImage
import com.motomusic.app.ui.components.QualityBadge
import com.motomusic.app.ui.components.pressScale
import com.motomusic.app.ui.theme.TabularFigures
import kotlinx.coroutines.delay

/**
 * The full-screen player, laid out as the design's sheet: a grab handle and a quiet header in
 * place of a toolbar, hero artwork with a soft glow, title and heart, a slim scrubber, the
 * transport row around one solid play disc, volume, and a row of capsules for sound, output
 * and the queue.
 *
 * The playing position arrives as a [State] rather than a value, and is read only inside
 * [SeekSection]. That keeps the position tick — and every frame of a drag — out of the
 * artwork, the titles and the transport row, which would otherwise all recompose with it.
 */
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
    onOpenAlbum: () -> Unit,
    output: AudioOutput?,
    outputPinned: Boolean,
    equalizerOn: Boolean,
    onOpenOutputs: () -> Unit,
    onOpenEqualizer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val track = playback.current

    // This screen paints its own background rather than sitting in a Surface, so it has to set
    // the content colour too, or text and icons fall back to black on the dark theme.
    CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.onSurface) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // The grab handle says "this pulls down" before anyone tries it.
            Box(
                Modifier
                    .padding(top = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
            )

            PlayerHeader(
                albumName = track?.album.orEmpty(),
                queueLabel = if (playback.queue.size > 1) "${playback.queueIndex + 1} of ${playback.queue.size}" else null,
                sleepTimerOn = sleepTimer !is SleepTimerState.Off,
                menuEnabled = track != null,
                onCollapse = onBack,
                onOpenAlbum = onOpenAlbum,
                onOpenSleepTimer = onOpenSleepTimer,
                onOpenMenu = onOpenMenu,
            )

            Column(
                modifier = Modifier.padding(horizontal = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(12.dp))
                HeroArtwork(
                    albumId = track?.albumId ?: 0L,
                    songUri = track?.songUri,
                    badge = track?.losslessFormat?.let { "$it · Lossless" },
                )
                Spacer(Modifier.height(28.dp))

                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            text = track?.title ?: "Nothing playing",
                            style = MaterialTheme.typography.headlineSmall,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = track?.artist ?: "Pick a song from your library",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Normal),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Box(
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center,
                    ) {
                        HeartButton(
                            isFavorite = isFavorite,
                            onClick = onToggleFavorite,
                            enabled = track != null,
                            size = 48.dp,
                            iconSize = 24.dp,
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))

                SeekSection(
                    position = position,
                    trackDurationMs = track?.durationMs ?: 0L,
                    enabled = track != null,
                    onSeek = onSeek,
                )

                Spacer(Modifier.height(16.dp))

                TransportRow(
                    playback = playback,
                    enabled = track != null,
                    onPlayPause = onPlayPause,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                )

                Spacer(Modifier.height(24.dp))
                MediaVolumeRow()
                Spacer(Modifier.height(24.dp))

                // Sound, output and queue: the things reached for while listening, in thumb range.
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    UtilityCapsule(
                        icon = Icons.Rounded.GraphicEq,
                        label = "EQ",
                        contentDescription = if (equalizerOn) "Equalizer, on" else "Equalizer, off",
                        active = equalizerOn,
                        onClick = onOpenEqualizer,
                    )
                    AudioOutputChip(
                        output = output,
                        pinned = outputPinned,
                        onClick = onOpenOutputs,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    UtilityCapsule(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        label = "Queue",
                        contentDescription = "Playing queue",
                        active = false,
                        onClick = onOpenQueue,
                    )
                }

                SleepTimerNote(sleepTimer = sleepTimer, onClick = onOpenSleepTimer)
                Spacer(Modifier.height(24.dp))
            }
        }
    }
}

/** Collapse on the left; where the music is coming from in the middle; timer and menu on the right. */
@Composable
private fun PlayerHeader(
    albumName: String,
    queueLabel: String?,
    sleepTimerOn: Boolean,
    menuEnabled: Boolean,
    onCollapse: () -> Unit,
    onOpenAlbum: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onOpenMenu: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onCollapse) {
            Icon(Icons.Rounded.KeyboardArrowDown, contentDescription = "Collapse player", modifier = Modifier.size(30.dp))
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(12.dp))
                .clickable(enabled = albumName.isNotEmpty(), onClick = onOpenAlbum)
                .padding(vertical = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = if (queueLabel != null) "Playing $queueLabel" else "Now playing",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
            )
            if (albumName.isNotEmpty()) {
                Text(
                    text = albumName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        IconButton(onClick = onOpenSleepTimer) {
            Icon(
                imageVector = Icons.Rounded.Bedtime,
                contentDescription = if (sleepTimerOn) "Sleep timer, on" else "Sleep timer",
                tint = if (sleepTimerOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onOpenMenu, enabled = menuEnabled) {
            Icon(Icons.Rounded.MoreHoriz, contentDescription = "More options")
        }
    }
}

/**
 * The cover, large and rounded, lifted by a soft shadow with a blurred copy of itself glowing
 * underneath. Blur needs Android 12; older phones get the shadow alone rather than a sharp copy.
 */
@Composable
private fun HeroArtwork(albumId: Long, songUri: String?, badge: String?) {
    Box(
        modifier = Modifier.fillMaxWidth().widthIn(max = 360.dp).aspectRatio(1f),
        contentAlignment = Alignment.Center,
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            LargeArtworkImage(
                albumId = albumId,
                songUri = songUri,
                sizeHint = 120.dp,
                cornerRadius = ARTWORK_RADIUS,
                modifier = Modifier
                    .fillMaxSize()
                    // One layer for blur, fade and offset: a separate alpha layer would render
                    // the glow off-screen at the cover's size and clip it into a hard square.
                    .graphicsLayer {
                        scaleX = 1.04f
                        scaleY = 1.04f
                        translationY = 18.dp.toPx()
                        alpha = 0.55f
                        val radius = 36.dp.toPx()
                        renderEffect = BlurEffect(radius, radius, TileMode.Decal)
                        clip = false
                    },
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .shadow(24.dp, RoundedCornerShape(ARTWORK_RADIUS), clip = false),
        ) {
            LargeArtworkImage(
                albumId = albumId,
                songUri = songUri,
                sizeHint = 360.dp,
                cornerRadius = ARTWORK_RADIUS,
                modifier = Modifier.fillMaxSize(),
            )
            if (badge != null) {
                QualityBadge(
                    text = badge,
                    onArtwork = true,
                    modifier = Modifier.align(Alignment.BottomEnd).padding(14.dp),
                )
            }
        }
    }
}

private val ARTWORK_RADIUS = 28.dp

@Composable
private fun TransportRow(
    playback: PlaybackUiState,
    enabled: Boolean,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ToggleIconButton(
            icon = Icons.Rounded.Shuffle,
            contentDescription = if (playback.shuffleEnabled) "Shuffle on" else "Shuffle off",
            active = playback.shuffleEnabled,
            onClick = onToggleShuffle,
        )
        IconButton(onClick = onPrevious, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipPrevious, contentDescription = "Previous song", modifier = Modifier.size(40.dp))
        }

        // The one solid disc on the screen: white with a dark glyph in the dark theme, ink with
        // a white glyph in the light one. It presses in like a physical button.
        val interaction = remember { MutableInteractionSource() }
        val disc = MaterialTheme.colorScheme.onSurface
        Box(
            modifier = Modifier
                .pressScale(interaction)
                .size(72.dp)
                .shadow(16.dp, CircleShape, ambientColor = disc, spotColor = disc)
                .clip(CircleShape)
                .background(if (enabled) disc else disc.copy(alpha = 0.3f))
                .clickable(
                    interactionSource = interaction,
                    indication = ripple(),
                    enabled = enabled,
                    role = Role.Button,
                    onClick = onPlayPause,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (playback.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                contentDescription = if (playback.isPlaying) "Pause" else "Play",
                tint = MaterialTheme.colorScheme.surface,
                modifier = Modifier.size(38.dp),
            )
        }

        IconButton(onClick = onNext, enabled = enabled, modifier = Modifier.size(56.dp)) {
            Icon(Icons.Rounded.SkipNext, contentDescription = "Next song", modifier = Modifier.size(40.dp))
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
}

/** A frosted capsule for one of the player's utilities. */
@Composable
private fun UtilityCapsule(
    icon: ImageVector,
    label: String,
    contentDescription: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .heightIn(min = 40.dp)
            .semantics { this.contentDescription = contentDescription },
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(6.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = tint)
        }
    }
}

@Composable
private fun SleepTimerNote(sleepTimer: SleepTimerState, onClick: () -> Unit) {
    val text = when (sleepTimer) {
        is SleepTimerState.Countdown -> "Sleeping in ${formatDuration(sleepTimer.remainingMs)}"
        is SleepTimerState.EndOfTrack -> "Stopping after this track"
        else -> return
    }
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium.merge(TabularFigures),
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier
            .padding(top = 16.dp)
            .clip(CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
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

    SlimSlider(
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

    // Elapsed on the left, time remaining on the right, in figures that never change width.
    val elapsedMs = (fraction * durationMs).toLong()
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(
            text = formatDuration(elapsedMs),
            style = MaterialTheme.typography.labelMedium.merge(TabularFigures),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "−" + formatDuration((durationMs - elapsedMs).coerceAtLeast(0L)),
            style = MaterialTheme.typography.labelMedium.merge(TabularFigures),
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

