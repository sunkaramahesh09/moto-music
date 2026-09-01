package com.motomusic.app.playback

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlin.math.sqrt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Wraps the real player so music eases in and out instead of being cut on and off.
 *
 * Every play and pause command in the app arrives here — the transport controls, the
 * notification, the lock screen and Bluetooth headsets all drive the session player — so the
 * fade applies everywhere rather than only where the UI remembered to ask for it.
 *
 * Pausing is deliberately deferred until the fade-out has finished, because pausing first and
 * fading afterwards would fade nothing: there would be no audio left to fade.
 *
 * All of this runs on the player's own (main) thread, which is where Media3 requires player
 * calls to be made.
 */
@OptIn(UnstableApi::class)
class FadingPlayer(
    private val delegate: Player,
    private val scope: CoroutineScope,
) : ForwardingPlayer(delegate) {

    /** Turned off by the "Fade in and out" setting, which restores instant play/pause. */
    var fadeEnabled: Boolean = true
        set(value) {
            field = value
            if (!value) {
                fadeJob?.cancel()
                fadeJob = null
                delegate.volume = targetVolume
            }
        }

    /** The volume that was asked for; a fade only ever moves between silence and this. */
    private var targetVolume: Float = delegate.volume

    private var fadeJob: Job? = null

    override fun play() {
        if (!fadeEnabled) {
            fadeJob?.cancel()
            fadeJob = null
            delegate.volume = targetVolume
            delegate.play()
            return
        }

        fadeJob?.cancel()
        // Resuming mid-fade-out starts from wherever the volume got to, so a quick
        // pause-then-play does not click.
        val from = if (delegate.isPlaying) delegate.volume else 0f
        delegate.volume = from
        delegate.play()
        fadeJob = scope.launch {
            ramp(from = from, to = targetVolume, durationMs = FADE_IN_MS)
            fadeJob = null
        }
    }

    override fun pause() {
        fadeJob?.cancel()
        if (!fadeEnabled || !delegate.isPlaying) {
            fadeJob = null
            delegate.pause()
            delegate.volume = targetVolume
            return
        }

        fadeJob = scope.launch {
            ramp(from = delegate.volume, to = 0f, durationMs = FADE_OUT_MS)
            delegate.pause()
            // Restored straight away so the next play — or a resume from another app —
            // never starts silent.
            delegate.volume = targetVolume
            fadeJob = null
        }
    }

    override fun setPlayWhenReady(playWhenReady: Boolean) {
        if (playWhenReady) play() else pause()
    }

    /**
     * A volume set from outside becomes the new fade target rather than fighting a fade in
     * progress.
     */
    override fun setVolume(volume: Float) {
        targetVolume = volume.coerceIn(0f, 1f)
        if (fadeJob == null) delegate.volume = targetVolume
    }

    /** Reports the intended volume, so a fade is never mistaken for the user turning it down. */
    override fun getVolume(): Float = targetVolume

    /**
     * Moves the volume along a squared curve in both directions, which is what an even fade
     * sounds like: loudness follows roughly the square root of amplitude, so a straight
     * amplitude ramp rushes the quiet end and crawls through the loud one.
     *
     * The interpolation happens in that perceived space — hence the square roots — so a fade
     * that starts from a half-faded volume picks up exactly where the last one left off.
     */
    private suspend fun ramp(from: Float, to: Float, durationMs: Long) {
        val peak = targetVolume
        if (durationMs <= 0L || peak <= 0f || from == to) {
            delegate.volume = to
            return
        }
        val fromLevel = sqrt((from / peak).coerceIn(0f, 1f))
        val toLevel = sqrt((to / peak).coerceIn(0f, 1f))

        val start = SystemClock.elapsedRealtime()
        while (currentCoroutineContext().isActive) {
            val elapsed = SystemClock.elapsedRealtime() - start
            if (elapsed >= durationMs) break
            val level = fromLevel + (toLevel - fromLevel) * (elapsed.toFloat() / durationMs)
            delegate.volume = (peak * level * level).coerceIn(0f, 1f)
            delay(STEP_MS)
        }
        delegate.volume = to
    }

    private companion object {
        const val FADE_IN_MS = 400L
        const val FADE_OUT_MS = 300L

        /** Roughly one step per frame: fine enough to be inaudible as steps, cheap to run. */
        const val STEP_MS = 16L
    }
}
