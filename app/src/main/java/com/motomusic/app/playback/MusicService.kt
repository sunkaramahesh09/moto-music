package com.motomusic.app.playback

import android.app.PendingIntent
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.google.common.util.concurrent.SettableFuture
import com.motomusic.app.MainActivity
import com.motomusic.app.R
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlin.math.min
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * The single playback owner.
 *
 * [MediaSessionService] is the architecture Media3 recommends: it hosts the player and the
 * [MediaSession], and Media3 itself puts the service in the foreground with a media notification
 * whenever playback starts. That is why the app declares no custom foreground service — the
 * notification, the lock-screen controls, Bluetooth/headset buttons and Android Auto style
 * clients all come from the session, not from hand-rolled code.
 *
 * Because the session outlives the UI, music keeps playing when the activity is destroyed, the
 * screen is locked, or the user switches apps.
 */
@OptIn(UnstableApi::class)
@AndroidEntryPoint
class MusicService : MediaSessionService() {

    @Inject lateinit var songRepository: SongRepository
    @Inject lateinit var historyRepository: PlaybackHistoryRepository
    @Inject lateinit var settingsRepository: SettingsRepository
    @Inject lateinit var sleepTimerController: SleepTimerController
    @Inject lateinit var eventBus: PlaybackEventBus
    @Inject lateinit var bitmapLoader: MotoBitmapLoader

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var player: ExoPlayer? = null

    /** The player the session drives: the ExoPlayer above, wrapped in the play/pause fade. */
    private var fadingPlayer: FadingPlayer? = null
    private var mediaSession: MediaSession? = null
    private var progressJob: Job? = null

    /** Guards against an unplayable library spinning through every track in a loop. */
    private var consecutiveErrors = 0

    // Play-count bookkeeping for the item currently being listened to.
    private var trackedMediaId: String? = null
    private var listenedMs = 0L
    private var alreadyCounted = false
    private var msSinceSessionSave = 0L

    override fun onCreate() {
        super.onCreate()

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val exoPlayer = ExoPlayer.Builder(this)
            // handleAudioFocus = true makes ExoPlayer duck and pause for calls and other apps.
            .setAudioAttributes(audioAttributes, /* handleAudioFocus = */ true)
            // Pauses when headphones are unplugged or a Bluetooth device disconnects.
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        exoPlayer.addListener(PlayerListener())
        player = exoPlayer

        val fading = FadingPlayer(exoPlayer, serviceScope)
        fadingPlayer = fading

        mediaSession = MediaSession.Builder(this, fading)
            .setCallback(SessionCallback())
            .setBitmapLoader(bitmapLoader)
            .setSessionActivity(openAppIntent())
            .build()

        setMediaNotificationProvider(
            DefaultMediaNotificationProvider.Builder(this)
                .setChannelId(NOTIFICATION_CHANNEL_ID)
                .setChannelName(R.string.playback_channel_name)
                .setNotificationId(NOTIFICATION_ID)
                .build()
                .apply { setSmallIcon(R.drawable.ic_notification) }
        )

        observeSettings()
        observeSleepTimer()
        startProgressLoop()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = mediaSession

    /**
     * When the user swipes the app away we only stop if nothing is playing; otherwise the
     * session keeps running so music survives the task being removed.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        val current = player
        if (current == null || !current.playWhenReady || current.mediaItemCount == 0) {
            saveSession()
            stopSelf()
        }
        super.onTaskRemoved(rootIntent)
    }

    override fun onDestroy() {
        saveSession()
        progressJob?.cancel()
        mediaSession?.run {
            player.release()
            release()
        }
        mediaSession = null
        fadingPlayer = null
        player = null
        serviceScope.cancel()
        super.onDestroy()
    }

    private fun openAppIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
    }

    private fun observeSettings() {
        settingsRepository.preferences
            .map { PlaybackSettings(it.skipSilence, it.pauseOnHeadphonesDisconnect, it.fadeOnPlayPause) }
            .distinctUntilChanged()
            .onEach { settings ->
                player?.skipSilenceEnabled = settings.skipSilence
                player?.setHandleAudioBecomingNoisy(settings.pauseOnDisconnect)
                fadingPlayer?.fadeEnabled = settings.fadeOnPlayPause
            }
            .launchIn(serviceScope)
    }

    private fun observeSleepTimer() {
        sleepTimerController.expired
            .onEach {
                // Through the fading player, so the timer eases the music away.
                fadingPlayer?.pause()
                eventBus.post("Sleep timer finished. Playback paused.")
            }
            .launchIn(serviceScope)
    }

    /**
     * Drives play-count tracking and periodic session saving. One second is frequent enough
     * for both and cheap enough to run for the lifetime of the service.
     */
    private fun startProgressLoop() {
        progressJob?.cancel()
        progressJob = serviceScope.launch {
            while (isActive) {
                delay(TICK_MS)
                val current = player ?: continue
                if (!current.isPlaying) continue

                listenedMs += TICK_MS
                msSinceSessionSave += TICK_MS

                if (!alreadyCounted && listenedMs >= playThresholdMs(current.duration)) {
                    alreadyCounted = true
                    current.currentMediaItem?.mediaId?.toLongOrNull()?.let { songId ->
                        launch { runCatching { historyRepository.recordPlay(songId) } }
                    }
                }
                if (msSinceSessionSave >= SESSION_SAVE_INTERVAL_MS) {
                    msSinceSessionSave = 0L
                    saveSession()
                }
            }
        }
    }

    private fun resetPlayTracking(mediaId: String?) {
        trackedMediaId = mediaId
        listenedMs = 0L
        alreadyCounted = false
    }

    /** Persists only ids and a position, so the queue can be restored without any file access. */
    private fun saveSession() {
        val current = player ?: return
        val ids = (0 until current.mediaItemCount).mapNotNull {
            current.getMediaItemAt(it).mediaId.toLongOrNull()
        }
        if (ids.isEmpty()) return
        val index = current.currentMediaItemIndex
        val position = current.currentPosition.coerceAtLeast(0L)
        serviceScope.launch {
            runCatching { settingsRepository.saveLastSession(ids, index, position) }
        }
    }

    /** The three player settings watched as one value, so an unrelated change is not reapplied. */
    private data class PlaybackSettings(
        val skipSilence: Boolean,
        val pauseOnDisconnect: Boolean,
        val fadeOnPlayPause: Boolean,
    )

    private inner class PlayerListener : Player.Listener {

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            if (mediaItem?.mediaId != trackedMediaId) resetPlayTracking(mediaItem?.mediaId)
            consecutiveErrors = 0
            saveSession()

            // "Stop after this track" is honoured the moment the next track would start.
            if (reason == Player.MEDIA_ITEM_TRANSITION_REASON_AUTO) {
                sleepTimerController.onTrackFinished()
            }
        }

        override fun onIsPlayingChanged(isPlaying: Boolean) {
            if (!isPlaying) saveSession()
        }

        override fun onPlayerError(error: PlaybackException) {
            val failedItem = player?.currentMediaItem
            val title = failedItem?.mediaMetadata?.title?.toString() ?: "This track"
            Log.w(TAG, "Playback error for $title (${error.errorCodeName})", error)

            // A file that MediaStore still lists but that no longer opens is dropped from the
            // library, otherwise it would keep reappearing in every list.
            if (error.errorCode == PlaybackException.ERROR_CODE_IO_FILE_NOT_FOUND) {
                failedItem?.mediaId?.toLongOrNull()?.let { songId ->
                    serviceScope.launch { runCatching { songRepository.removeMissingSong(songId) } }
                }
            }

            consecutiveErrors++
            val current = player
            if (current == null || consecutiveErrors >= MAX_CONSECUTIVE_ERRORS) {
                eventBus.post("Playback stopped: these files could not be played.")
                current?.pause()
                consecutiveErrors = 0
                return
            }

            if (current.hasNextMediaItem()) {
                eventBus.post("Couldn't play \"$title\". Skipping to the next song.")
                current.seekToNextMediaItem()
                current.prepare()
            } else {
                eventBus.post("Couldn't play \"$title\".")
                current.pause()
            }
        }
    }

    private inner class SessionCallback : MediaSession.Callback {

        /**
         * Media3 drops a MediaItem's local URI when it crosses the session boundary, so the
         * playable URI is restored here from the request metadata the controller sent.
         */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            val resolved = mediaItems.mapNotNull { item ->
                val uri = item.requestMetadata.mediaUri ?: item.localConfiguration?.uri
                uri?.let { item.buildUpon().setUri(it).build() }
            }.toMutableList()
            return Futures.immediateFuture(resolved)
        }

        /**
         * Lets Android resume the last queue from the notification or a Bluetooth "play"
         * command after the app process was killed.
         *
         * The same queue is handed back whether the system is about to play it or is only
         * rebuilding the notification, so [isForPlayback] needs no special case here.
         */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            isForPlayback: Boolean,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> {
            val future = SettableFuture.create<MediaSession.MediaItemsWithStartPosition>()
            serviceScope.launch {
                val restored = runCatching {
                    val session = settingsRepository.loadLastSession()
                        ?: return@runCatching null
                    val songs = songRepository.getSongs(session.songIds)
                    if (songs.isEmpty()) return@runCatching null
                    MediaSession.MediaItemsWithStartPosition(
                        MediaItemMapper.toMediaItems(songs),
                        session.index.coerceIn(0, songs.lastIndex),
                        session.positionMs,
                    )
                }.getOrNull()

                if (restored != null) {
                    future.set(restored)
                } else {
                    future.setException(UnsupportedOperationException("Nothing to resume"))
                }
            }
            return future
        }
    }

    companion object {
        private const val TAG = "MusicService"
        private const val TICK_MS = 1_000L
        private const val SESSION_SAVE_INTERVAL_MS = 15_000L
        private const val MAX_CONSECUTIVE_ERRORS = 5

        const val NOTIFICATION_ID = 1001
        const val NOTIFICATION_CHANNEL_ID = "moto_music_playback"

        /**
         * A listen counts once the user has heard 30 seconds, or half of a shorter track, so
         * skipping through a library never inflates "Most played".
         */
        fun playThresholdMs(durationMs: Long): Long =
            if (durationMs > 0) min(30_000L, durationMs / 2) else 30_000L
    }
}
