package com.motomusic.app.presentation.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaylistRepository
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.playback.PlaybackConnection
import com.motomusic.app.playback.PlaybackEventBus
import com.motomusic.app.playback.PlaybackPosition
import com.motomusic.app.playback.PlaybackUiState
import com.motomusic.app.playback.SleepTimerController
import com.motomusic.app.playback.SleepTimerState
import com.motomusic.app.presentation.common.SongActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns everything about the currently playing music and is shared by the mini player, the full
 * player, the queue and every list that can start playback.
 *
 * It is created once at the root of the app, so all of those screens see exactly the same state.
 */
@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackConnection: PlaybackConnection,
    private val favoritesRepository: FavoritesRepository,
    private val playlistRepository: PlaylistRepository,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    private val sleepTimerController: SleepTimerController,
    private val eventBus: PlaybackEventBus,
) : ViewModel(), SongActions {

    val playback: StateFlow<PlaybackUiState> = playbackConnection.state
    val position: StateFlow<PlaybackPosition> = playbackConnection.position
    val sleepTimer: StateFlow<SleepTimerState> = sleepTimerController.state

    val favoriteIds: StateFlow<Set<Long>> = favoritesRepository.observeFavoriteIds()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptySet())

    /** The song whose three-dot menu is open, or null when the sheet is dismissed. */
    private val _menuTarget = MutableStateFlow<SongMenuTarget?>(null)
    val menuTarget: StateFlow<SongMenuTarget?> = _menuTarget.asStateFlow()

    /** Every playlist, for the "Add to playlist" sheet. */
    val playlists: StateFlow<List<Playlist>> = playlistRepository.observePlaylists()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    /** The song whose "Add to playlist" sheet is open. */
    private val _addToPlaylistSong = MutableStateFlow<Song?>(null)
    val addToPlaylistSong: StateFlow<Song?> = _addToPlaylistSong.asStateFlow()

    /** The song whose details dialog is open. */
    private val _infoSong = MutableStateFlow<Song?>(null)
    val infoSong: StateFlow<Song?> = _infoSong.asStateFlow()

    /** Whether the sleep timer sheet is up. */
    private val _sleepTimerSheetOpen = MutableStateFlow(false)
    val sleepTimerSheetOpen: StateFlow<Boolean> = _sleepTimerSheetOpen.asStateFlow()

    init {
        playbackConnection.connect()
        restoreLastSession()
    }

    /**
     * Puts the previous queue back without starting playback, so "resume last song" gives the
     * user a loaded player rather than surprise audio on launch.
     */
    private fun restoreLastSession() = viewModelScope.launch {
        val session = runCatching { settingsRepository.loadLastSession() }.getOrNull() ?: return@launch
        val songs = songRepository.getSongs(session.songIds)
        if (songs.isEmpty()) return@launch
        playbackConnection.restoreQueue(songs, session.index.coerceIn(0, songs.lastIndex), session.positionMs)
    }

    // --- SongActions ----------------------------------------------------------------------

    override fun play(songs: List<Song>, startIndex: Int) = playbackConnection.playSongs(songs, startIndex)

    override fun shuffle(songs: List<Song>) = playbackConnection.shuffleSongs(songs)

    override fun playNext(song: Song) = playbackConnection.playNext(listOf(song))

    override fun addToQueue(songs: List<Song>) = playbackConnection.addToQueue(songs)

    override fun toggleFavorite(song: Song) {
        viewModelScope.launch { favoritesRepository.toggleFavorite(song.id) }
    }

    /**
     * Hides one file. The library rescans itself off the back of the setting, which is what
     * actually removes the song from every list; the queue is cleaned up here, since a hidden
     * song left playing in the mini player rather defeats the point.
     */
    override fun hideFromLibrary(song: Song) {
        playbackConnection.removeSongFromQueue(song.id)
        viewModelScope.launch {
            settingsRepository.setSongHidden(song.hideKey, hidden = true)
            eventBus.post("\"${song.title}\" hidden. Restore it in Settings → Library.")
        }
    }

    override fun openMenu(song: Song) {
        _menuTarget.value = SongMenuTarget(song)
    }

    /** Same menu, plus the "Remove from playlist" entry that only makes sense inside one. */
    fun openMenuInPlaylist(song: Song, playlistId: Long) {
        _menuTarget.value = SongMenuTarget(song, playlistId)
    }

    // --- Sheets and dialogs ---------------------------------------------------------------

    fun dismissMenu() {
        _menuTarget.value = null
    }

    fun requestAddToPlaylist(song: Song) {
        _menuTarget.value = null
        _addToPlaylistSong.value = song
    }

    fun dismissAddToPlaylist() {
        _addToPlaylistSong.value = null
    }

    fun requestSongInfo(song: Song) {
        _menuTarget.value = null
        _infoSong.value = song
    }

    fun dismissSongInfo() {
        _infoSong.value = null
    }

    // --- Playlists ------------------------------------------------------------------------

    fun addToPlaylist(playlistId: Long, songIds: List<Long>) {
        viewModelScope.launch { playlistRepository.addSongs(playlistId, songIds) }
    }

    /** Creates a playlist and drops the songs straight into it, for "New playlist" in the sheet. */
    fun createPlaylistWith(name: String, songIds: List<Long>) {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return
        viewModelScope.launch {
            val playlistId = playlistRepository.createPlaylist(trimmed)
            if (songIds.isNotEmpty()) playlistRepository.addSongs(playlistId, songIds)
        }
    }

    fun removeFromPlaylist(playlistId: Long, songId: Long) {
        viewModelScope.launch { playlistRepository.removeSong(playlistId, songId) }
    }

    /** Opens the info dialog for whatever is playing right now. */
    fun requestInfoForCurrentSong() = viewModelScope.launch {
        val songId = playbackConnection.state.value.current?.songId ?: return@launch
        _infoSong.value = songRepository.getSong(songId)
    }

    /**
     * Opens the three-dot menu for whatever is playing. The player only knows the track as it
     * sits in the media session, so the full song is read back from the library first.
     */
    fun openMenuForCurrentSong() = viewModelScope.launch {
        val songId = playbackConnection.state.value.current?.songId ?: return@launch
        val song = songRepository.getSong(songId) ?: return@launch
        _menuTarget.value = SongMenuTarget(song)
    }

    fun openSleepTimer() {
        _sleepTimerSheetOpen.value = true
    }

    fun dismissSleepTimer() {
        _sleepTimerSheetOpen.value = false
    }

    // --- Transport ------------------------------------------------------------------------

    fun togglePlayPause() = playbackConnection.togglePlayPause()
    fun next() = playbackConnection.next()
    fun previous() = playbackConnection.previous()
    fun seekTo(positionMs: Long) = playbackConnection.seekTo(positionMs)
    fun toggleShuffle() = playbackConnection.toggleShuffle()
    fun cycleRepeatMode() = playbackConnection.cycleRepeatMode()

    fun toggleFavoriteForCurrent() {
        val songId = playbackConnection.state.value.current?.songId ?: return
        viewModelScope.launch { favoritesRepository.toggleFavorite(songId) }
    }

    // --- Queue ----------------------------------------------------------------------------

    fun skipToQueueIndex(index: Int) = playbackConnection.skipToQueueIndex(index)
    fun removeFromQueue(index: Int) = playbackConnection.removeFromQueue(index)
    fun moveQueueItem(from: Int, to: Int) = playbackConnection.moveQueueItem(from, to)
    fun clearQueue() = playbackConnection.clearQueue()

    // --- Sleep timer ----------------------------------------------------------------------

    fun startSleepTimer(minutes: Int) = sleepTimerController.startCountdown(minutes * 60_000L)
    fun sleepAfterCurrentTrack() = sleepTimerController.stopAfterCurrentTrack()
    fun cancelSleepTimer() = sleepTimerController.cancel()
}
