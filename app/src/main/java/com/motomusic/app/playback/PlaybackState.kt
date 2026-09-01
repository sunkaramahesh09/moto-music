package com.motomusic.app.playback

import androidx.compose.runtime.Immutable
import androidx.media3.common.Player

/** Repeat behaviour, mapped 1:1 onto the Media3 constants. */
enum class RepeatMode(val playerValue: Int, val label: String) {
    OFF(Player.REPEAT_MODE_OFF, "Repeat off"),
    ALL(Player.REPEAT_MODE_ALL, "Repeat all"),
    ONE(Player.REPEAT_MODE_ONE, "Repeat one");

    fun next(): RepeatMode = when (this) {
        OFF -> ALL
        ALL -> ONE
        ONE -> OFF
    }

    companion object {
        fun fromPlayer(value: Int): RepeatMode = entries.firstOrNull { it.playerValue == value } ?: OFF
    }
}

/**
 * Everything the UI needs about playback except the position, which changes every frame and is
 * published separately so a ticking clock never recomposes the whole player.
 */
@Immutable
data class PlaybackUiState(
    val isConnected: Boolean = false,
    val current: TrackInfo? = null,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val shuffleEnabled: Boolean = false,
    val repeatMode: RepeatMode = RepeatMode.OFF,
    val queue: List<TrackInfo> = emptyList(),
    val queueIndex: Int = 0,
    val hasNext: Boolean = false,
    val hasPrevious: Boolean = false,
) {
    val hasSession: Boolean get() = current != null
}

@Immutable
data class PlaybackPosition(
    val positionMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val durationMs: Long = 0L,
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}
