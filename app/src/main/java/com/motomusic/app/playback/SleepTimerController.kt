package com.motomusic.app.playback

import android.os.SystemClock
import com.motomusic.app.di.ApplicationScope
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface SleepTimerState {
    data object Off : SleepTimerState
    data class Countdown(val remainingMs: Long, val totalMs: Long) : SleepTimerState
    data object EndOfTrack : SleepTimerState
}

/**
 * Owns the sleep timer independently of the media service.
 *
 * The service and the UI both live in the same process, so a shared singleton keeps the
 * remaining time in sync without pushing timer state through the media session.
 */
@Singleton
class SleepTimerController @Inject constructor(
    @param:ApplicationScope private val scope: CoroutineScope,
) {
    private val _state = MutableStateFlow<SleepTimerState>(SleepTimerState.Off)
    val state: StateFlow<SleepTimerState> = _state.asStateFlow()

    private val _expired = MutableSharedFlow<Unit>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /** Emits once when playback should be paused. Collected by the media service. */
    val expired: SharedFlow<Unit> = _expired.asSharedFlow()

    private var countdownJob: Job? = null

    fun startCountdown(durationMs: Long) {
        countdownJob?.cancel()
        if (durationMs <= 0L) {
            cancel()
            return
        }
        // Elapsed realtime keeps the countdown honest across clock changes and doze.
        val endsAt = SystemClock.elapsedRealtime() + durationMs
        _state.value = SleepTimerState.Countdown(durationMs, durationMs)
        countdownJob = scope.launch {
            while (isActive) {
                val remaining = endsAt - SystemClock.elapsedRealtime()
                if (remaining <= 0L) {
                    _state.value = SleepTimerState.Off
                    _expired.tryEmit(Unit)
                    return@launch
                }
                _state.value = SleepTimerState.Countdown(remaining, durationMs)
                delay(TICK_MS)
            }
        }
    }

    fun stopAfterCurrentTrack() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = SleepTimerState.EndOfTrack
    }

    /** Called by the service when a track finishes while [SleepTimerState.EndOfTrack] is armed. */
    fun onTrackFinished() {
        if (_state.value is SleepTimerState.EndOfTrack) {
            _state.value = SleepTimerState.Off
            _expired.tryEmit(Unit)
        }
    }

    fun cancel() {
        countdownJob?.cancel()
        countdownJob = null
        _state.value = SleepTimerState.Off
    }

    val isArmed: Boolean get() = _state.value != SleepTimerState.Off

    private companion object {
        const val TICK_MS = 1_000L
    }
}
