package com.motomusic.app.playback

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/**
 * Carries user-facing playback messages from the media service to whatever screen is on top.
 * Only friendly text crosses this bus — exceptions are logged, never shown.
 */
@Singleton
class PlaybackEventBus @Inject constructor() {

    private val _messages = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 4,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun post(message: String) {
        _messages.tryEmit(message)
    }
}
