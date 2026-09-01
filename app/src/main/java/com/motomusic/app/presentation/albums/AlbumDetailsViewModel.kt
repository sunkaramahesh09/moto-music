package com.motomusic.app.presentation.albums

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Album
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
data class AlbumDetailsUiState(
    val album: Album? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AlbumDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    songRepository: SongRepository,
) : ViewModel() {

    private val albumId: Long = checkNotNull(savedStateHandle[Routes.ARG_ALBUM_ID]) {
        "AlbumDetails needs an ${Routes.ARG_ALBUM_ID} argument"
    }

    val state: StateFlow<AlbumDetailsUiState> = combine(
        songRepository.observeAlbum(albumId),
        songRepository.observeSongsInAlbum(albumId),
    ) { album, songs ->
        AlbumDetailsUiState(album = album, songs = songs, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlbumDetailsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
