package com.motomusic.app.presentation.browse

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.playback.PlaybackConnection
import com.motomusic.app.playback.PlaybackEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class BrowseUiState(
    val isLoading: Boolean = true,
    val totalSongs: Int = 0,
    /** The newest album on the device, whose cover dresses the "shuffle everything" card. */
    val heroAlbum: Album? = null,
    val newAlbums: List<Album> = emptyList(),
    val topSongs: List<Song> = emptyList(),
    val recentlyPlayed: List<Song> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && totalSongs == 0
}

/** The ready-made ways into the library that Browse offers as tiles. */
enum class QuickMix { FAVORITES, RECENTLY_ADDED, MOST_PLAYED, RANDOM_ALBUM }

/**
 * Browse is the design's discovery page, made from the user's own library: there is no catalogue
 * to stream from, so "new releases" are the albums most recently copied to the phone and the
 * "charts" are the user's own most-played songs.
 */
@HiltViewModel
class BrowseViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val historyRepository: PlaybackHistoryRepository,
    private val favoritesRepository: FavoritesRepository,
    private val playbackConnection: PlaybackConnection,
    private val eventBus: PlaybackEventBus,
) : ViewModel() {

    val state: StateFlow<BrowseUiState> = combine(
        songRepository.observeSongCount(),
        songRepository.observeRecentlyAdded(RECENT_SONGS_FOR_ALBUMS),
        songRepository.observeAlbums(),
        historyRepository.observeMostPlayed(TOP_SONGS),
        historyRepository.observeRecentlyPlayed(CAROUSEL_LIMIT),
    ) { totalSongs, recentlyAdded, albums, topSongs, recentlyPlayed ->
        val albumsById = albums.associateBy { it.id }
        val newAlbums = recentlyAdded
            .map { it.albumId }
            .distinct()
            .mapNotNull { albumsById[it] }
        BrowseUiState(
            isLoading = false,
            totalSongs = totalSongs,
            heroAlbum = newAlbums.firstOrNull(),
            newAlbums = newAlbums.take(NEW_ALBUMS),
            topSongs = topSongs,
            recentlyPlayed = recentlyPlayed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), BrowseUiState())

    /** Every song on the device, in a new order. */
    fun shuffleEverything() = viewModelScope.launch {
        val songs = songRepository.observeSongs(SortOrder.TITLE_ASC).first()
        if (songs.isEmpty()) return@launch
        playbackConnection.shuffleSongs(songs)
    }

    fun playMix(mix: QuickMix) = viewModelScope.launch {
        val songs = when (mix) {
            QuickMix.FAVORITES -> favoritesRepository.observeFavorites().first()
            QuickMix.RECENTLY_ADDED -> songRepository.observeRecentlyAdded(MIX_SIZE).first()
            QuickMix.MOST_PLAYED -> historyRepository.observeMostPlayed(MIX_SIZE).first()
            QuickMix.RANDOM_ALBUM -> songRepository.observeAlbums().first().randomOrNull()
                ?.let { album -> songRepository.observeSongsInAlbum(album.id).first() }
                .orEmpty()
        }
        when {
            songs.isEmpty() -> eventBus.post(
                when (mix) {
                    QuickMix.FAVORITES -> "No favourites yet. Tap the heart on a song you love."
                    QuickMix.MOST_PLAYED -> "Nothing has been played enough yet."
                    else -> "No songs to play yet."
                },
            )
            // An album plays in its own order; everything else is a mix.
            mix == QuickMix.RANDOM_ALBUM -> playbackConnection.playSongs(songs)
            else -> playbackConnection.shuffleSongs(songs)
        }
    }

    private companion object {
        const val RECENT_SONGS_FOR_ALBUMS = 60
        const val NEW_ALBUMS = 4
        const val TOP_SONGS = 5
        const val CAROUSEL_LIMIT = 12
        const val MIX_SIZE = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
