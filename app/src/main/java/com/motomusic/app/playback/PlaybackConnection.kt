package com.motomusic.app.playback

import android.content.ComponentName
import android.content.Context
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.MoreExecutors
import com.motomusic.app.di.ApplicationScope
import com.motomusic.app.domain.model.Song
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * The app's only handle on playback.
 *
 * A [MediaController] talks to [MusicService] through the media session, which means the UI can
 * be created and destroyed freely while playback continues in the service. All commands are
 * issued on the main thread, as Media3 requires.
 */
@OptIn(UnstableApi::class)
@Singleton
class PlaybackConnection @Inject constructor(
    @param:ApplicationContext private val context: Context,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(PlaybackUiState())
    val state: StateFlow<PlaybackUiState> = _state.asStateFlow()

    private val _position = MutableStateFlow(PlaybackPosition())
    val position: StateFlow<PlaybackPosition> = _position.asStateFlow()

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller: MediaController? = null

    /** Rebuilt only when the timeline changes; mapping a long queue on every event is not free. */
    private var cachedQueue: List<TrackInfo> = emptyList()
    private var positionJob: Job? = null
    private var connecting = false

    /** Safe to call repeatedly; connects on first use and reconnects if the service was killed. */
    fun connect() {
        if (controller != null || connecting) return
        connecting = true
        scope.launch(Dispatchers.Main.immediate) {
            val token = SessionToken(context, ComponentName(context, MusicService::class.java))
            val future = MediaController.Builder(context, token).buildAsync()
            controllerFuture = future
            future.addListener({
                connecting = false
                val newController = runCatching { future.get() }.getOrElse {
                    Log.e(TAG, "Could not connect to the media session", it)
                    return@addListener
                }
                controller = newController
                newController.addListener(ControllerListener())
                pushState()
                startPositionUpdates()
            }, MoreExecutors.directExecutor())
        }
    }

    fun release() {
        positionJob?.cancel()
        controller?.release()
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        cachedQueue = emptyList()
        _state.value = PlaybackUiState()
    }

    // --- Commands -------------------------------------------------------------------------

    /** Replaces the queue with [songs] and starts at [startIndex]. */
    fun playSongs(songs: List<Song>, startIndex: Int = 0, shuffle: Boolean = false) {
        if (songs.isEmpty()) return
        withController { player ->
            player.shuffleModeEnabled = shuffle
            player.setMediaItems(
                MediaItemMapper.toMediaItems(songs),
                startIndex.coerceIn(0, songs.lastIndex),
                C.TIME_UNSET,
            )
            player.prepare()
            player.play()
        }
    }

    fun shuffleSongs(songs: List<Song>) {
        if (songs.isEmpty()) return
        playSongs(songs, startIndex = songs.indices.random(), shuffle = true)
    }

    /** Restores a previous queue without starting playback, used on cold start. */
    fun restoreQueue(songs: List<Song>, index: Int, positionMs: Long) {
        if (songs.isEmpty()) return
        withController { player ->
            if (player.mediaItemCount > 0) return@withController
            player.setMediaItems(
                MediaItemMapper.toMediaItems(songs),
                index.coerceIn(0, songs.lastIndex),
                positionMs.coerceAtLeast(0L),
            )
            player.prepare()
        }
    }

    fun togglePlayPause() = withController { player ->
        if (player.isPlaying) {
            player.pause()
        } else {
            if (player.playbackState == Player.STATE_IDLE) player.prepare()
            player.play()
        }
    }

    fun play() = withController { it.play() }

    fun pause() = withController { it.pause() }

    fun next() = withController { player ->
        if (player.hasNextMediaItem()) player.seekToNextMediaItem()
    }

    /** Matches the platform convention: restart the track unless it just started. */
    fun previous() = withController { player ->
        if (player.currentPosition > RESTART_THRESHOLD_MS || !player.hasPreviousMediaItem()) {
            player.seekTo(0)
        } else {
            player.seekToPreviousMediaItem()
        }
    }

    fun seekTo(positionMs: Long) = withController { it.seekTo(positionMs.coerceAtLeast(0L)) }

    fun toggleShuffle() = withController { it.shuffleModeEnabled = !it.shuffleModeEnabled }

    fun cycleRepeatMode() = withController { player ->
        player.repeatMode = RepeatMode.fromPlayer(player.repeatMode).next().playerValue
    }

    fun addToQueue(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { it.addMediaItems(MediaItemMapper.toMediaItems(songs)) }
    }

    /** Inserts right after the current track. */
    fun playNext(songs: List<Song>) {
        if (songs.isEmpty()) return
        withController { player ->
            val insertAt = (player.currentMediaItemIndex + 1).coerceAtMost(player.mediaItemCount)
            player.addMediaItems(insertAt, MediaItemMapper.toMediaItems(songs))
        }
    }

    fun removeFromQueue(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) player.removeMediaItem(index)
    }

    /**
     * Drops every copy of one song from the queue, for when it is hidden from the library.
     * Removing the item that is playing hands playback to the next one, which is what the
     * user asking to never see this file again would expect.
     */
    fun removeSongFromQueue(songId: Long) = withController { player ->
        val mediaId = songId.toString()
        for (index in player.mediaItemCount - 1 downTo 0) {
            if (player.getMediaItemAt(index).mediaId == mediaId) player.removeMediaItem(index)
        }
    }

    fun moveQueueItem(from: Int, to: Int) = withController { player ->
        val count = player.mediaItemCount
        if (from in 0 until count && to in 0 until count && from != to) {
            player.moveMediaItem(from, to)
        }
    }

    fun skipToQueueIndex(index: Int) = withController { player ->
        if (index in 0 until player.mediaItemCount) {
            player.seekTo(index, 0L)
            player.play()
        }
    }

    fun clearQueue() = withController { player ->
        player.clearMediaItems()
        player.stop()
    }

    // --- Internals ------------------------------------------------------------------------

    private fun withController(block: (MediaController) -> Unit) {
        val active = controller
        if (active == null) {
            connect()
            // Retry once the controller arrives so the very first tap is not swallowed.
            scope.launch(Dispatchers.Main.immediate) {
                repeat(CONNECT_RETRIES) {
                    delay(CONNECT_RETRY_DELAY_MS)
                    controller?.let { ready ->
                        runCatching { block(ready) }
                            .onFailure { Log.w(TAG, "Playback command failed", it) }
                        return@launch
                    }
                }
                Log.w(TAG, "Dropped a playback command: no media session")
            }
            return
        }
        scope.launch(Dispatchers.Main.immediate) {
            runCatching { block(active) }.onFailure { Log.w(TAG, "Playback command failed", it) }
        }
    }

    private fun startPositionUpdates() {
        positionJob?.cancel()
        positionJob = scope.launch(Dispatchers.Main.immediate) {
            while (isActive) {
                val active = controller
                if (active != null) {
                    _position.value = PlaybackPosition(
                        positionMs = active.currentPosition.coerceAtLeast(0L),
                        bufferedMs = active.bufferedPosition.coerceAtLeast(0L),
                        durationMs = active.duration.takeIf { it != C.TIME_UNSET && it > 0 }
                            ?: _state.value.current?.durationMs ?: 0L,
                    )
                }
                // Only tick quickly while something is actually moving.
                delay(if (active?.isPlaying == true) PLAYING_TICK_MS else IDLE_TICK_MS)
            }
        }
    }

    private fun pushState(rebuildQueue: Boolean = true) {
        val active = controller
        if (active == null) {
            cachedQueue = emptyList()
            _state.value = PlaybackUiState()
            return
        }
        val queue = if (rebuildQueue || cachedQueue.size != active.mediaItemCount) {
            (0 until active.mediaItemCount)
                .map { MediaItemMapper.toTrackInfo(active.getMediaItemAt(it)) }
                .also { cachedQueue = it }
        } else {
            cachedQueue
        }
        _state.value = PlaybackUiState(
            isConnected = true,
            current = active.currentMediaItem?.let(MediaItemMapper::toTrackInfo),
            isPlaying = active.isPlaying,
            isBuffering = active.playbackState == Player.STATE_BUFFERING,
            shuffleEnabled = active.shuffleModeEnabled,
            repeatMode = RepeatMode.fromPlayer(active.repeatMode),
            queue = queue,
            queueIndex = active.currentMediaItemIndex.coerceAtLeast(0),
            hasNext = active.hasNextMediaItem(),
            hasPrevious = active.hasPreviousMediaItem(),
        )
    }

    private inner class ControllerListener : Player.Listener {
        /**
         * A single handler keeps the published state consistent no matter what changed, but
         * only for the events the UI actually shows: a fade, for instance, reports a volume
         * change every frame, and republishing the whole session for each of those would put
         * the UI to work sixty times a second for nothing.
         */
        override fun onEvents(player: Player, events: Player.Events) {
            if (!events.containsAny(*STATE_EVENTS)) return
            pushState(rebuildQueue = events.contains(Player.EVENT_TIMELINE_CHANGED))
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            _position.value = PlaybackPosition(durationMs = _state.value.current?.durationMs ?: 0L)
        }
    }

    private companion object {
        const val TAG = "PlaybackConnection"

        /** The events that change something the UI draws. */
        val STATE_EVENTS = intArrayOf(
            Player.EVENT_TIMELINE_CHANGED,
            Player.EVENT_MEDIA_ITEM_TRANSITION,
            Player.EVENT_MEDIA_METADATA_CHANGED,
            Player.EVENT_IS_PLAYING_CHANGED,
            Player.EVENT_PLAY_WHEN_READY_CHANGED,
            Player.EVENT_PLAYBACK_STATE_CHANGED,
            Player.EVENT_POSITION_DISCONTINUITY,
            Player.EVENT_SHUFFLE_MODE_ENABLED_CHANGED,
            Player.EVENT_REPEAT_MODE_CHANGED,
        )

        /** Four times a second: the seek bar and the elapsed label both follow this. */
        const val PLAYING_TICK_MS = 250L
        const val IDLE_TICK_MS = 1_000L
        const val RESTART_THRESHOLD_MS = 3_000L
        const val CONNECT_RETRIES = 20
        const val CONNECT_RETRY_DELAY_MS = 100L
    }
}
