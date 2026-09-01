package com.motomusic.app.presentation.playlists

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.PlaylistRepository
import com.motomusic.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PlaylistDetailsUiState(
    val playlist: Playlist? = null,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlaylistDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val playlistId: Long = checkNotNull(savedStateHandle[Routes.ARG_PLAYLIST_ID]) {
        "PlaylistDetails needs a ${Routes.ARG_PLAYLIST_ID} argument"
    }

    val state: StateFlow<PlaylistDetailsUiState> = combine(
        playlistRepository.observePlaylist(playlistId),
        playlistRepository.observeSongsInPlaylist(playlistId),
    ) { playlist, songs ->
        PlaylistDetailsUiState(playlist = playlist, songs = songs, isLoading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaylistDetailsUiState())

    fun rename(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isNotEmpty()) playlistRepository.renamePlaylist(playlistId, trimmed)
    }

    fun delete() = viewModelScope.launch {
        playlistRepository.deletePlaylist(playlistId)
    }

    fun moveSong(from: Int, to: Int) = viewModelScope.launch {
        playlistRepository.moveSong(playlistId, from, to)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
