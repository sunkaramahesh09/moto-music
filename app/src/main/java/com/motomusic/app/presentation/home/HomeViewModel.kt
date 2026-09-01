package com.motomusic.app.presentation.home

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.PlaylistRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.presentation.app.currentGreeting
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class HomeUiState(
    val greeting: String = "",
    val isLoading: Boolean = true,
    val recentlyPlayed: List<Song> = emptyList(),
    val mostPlayed: List<Song> = emptyList(),
    val recentlyAdded: List<Song> = emptyList(),
    val totalSongs: Int = 0,
    val favoriteCount: Int = 0,
    val playlists: List<Playlist> = emptyList(),
) {
    val isEmpty: Boolean get() = !isLoading && totalSongs == 0
}

/**
 * Home is assembled from small, bounded queries — counts and short lists only — so opening the
 * app never materialises a library of thousands of songs just to draw a few carousels.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    songRepository: SongRepository,
    historyRepository: PlaybackHistoryRepository,
    favoritesRepository: FavoritesRepository,
    playlistRepository: PlaylistRepository,
) : ViewModel() {

    val state: StateFlow<HomeUiState> = combine(
        historyRepository.observeRecentlyPlayed(CAROUSEL_LIMIT),
        historyRepository.observeMostPlayed(CAROUSEL_LIMIT),
        songRepository.observeRecentlyAdded(CAROUSEL_LIMIT),
        songRepository.observeSongCount(),
        combine(
            favoritesRepository.observeFavoriteIds(),
            playlistRepository.observePlaylists(),
        ) { favoriteIds, playlists -> favoriteIds.size to playlists },
    ) { recentlyPlayed, mostPlayed, recentlyAdded, totalSongs, favoritesAndPlaylists ->
        val (favoriteCount, playlists) = favoritesAndPlaylists
        HomeUiState(
            greeting = currentGreeting(),
            isLoading = false,
            recentlyPlayed = recentlyPlayed,
            mostPlayed = mostPlayed,
            recentlyAdded = recentlyAdded,
            totalSongs = totalSongs,
            favoriteCount = favoriteCount,
            playlists = playlists,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
        initialValue = HomeUiState(greeting = currentGreeting()),
    )

    private companion object {
        const val CAROUSEL_LIMIT = 12
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
