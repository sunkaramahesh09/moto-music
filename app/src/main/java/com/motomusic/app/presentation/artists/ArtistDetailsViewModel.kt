package com.motomusic.app.presentation.artists

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@Immutable
data class ArtistDetailsUiState(
    val artist: Artist? = null,
    val albums: List<Album> = emptyList(),
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArtistDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    songRepository: SongRepository,
) : ViewModel() {

    private val artistId: Long = checkNotNull(savedStateHandle[Routes.ARG_ARTIST_ID]) {
        "ArtistDetails needs an ${Routes.ARG_ARTIST_ID} argument"
    }

    val state: StateFlow<ArtistDetailsUiState> = combine(
        songRepository.observeArtist(artistId),
        songRepository.observeAlbumsByArtist(artistId),
        songRepository.observeSongsByArtist(artistId),
    ) { artist, albums, songs ->
        ArtistDetailsUiState(artist = artist, albums = albums, songs = songs, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ArtistDetailsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
