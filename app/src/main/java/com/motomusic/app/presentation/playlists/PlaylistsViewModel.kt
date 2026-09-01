package com.motomusic.app.presentation.playlists

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Playlist
import com.motomusic.app.domain.repository.PlaylistRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class PlaylistsUiState(
    val playlists: List<Playlist> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class PlaylistsViewModel @Inject constructor(
    private val playlistRepository: PlaylistRepository,
) : ViewModel() {

    val state: StateFlow<PlaylistsUiState> = playlistRepository.observePlaylists()
        .map { PlaylistsUiState(playlists = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), PlaylistsUiState())

    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    fun createPlaylist(name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        if (playlistRepository.playlistNameExists(trimmed)) {
            _messages.tryEmit("A playlist called \"$trimmed\" already exists")
            return@launch
        }
        playlistRepository.createPlaylist(trimmed)
    }

    fun renamePlaylist(id: Long, name: String) = viewModelScope.launch {
        val trimmed = name.trim()
        if (trimmed.isEmpty()) return@launch
        playlistRepository.renamePlaylist(id, trimmed)
    }

    fun deletePlaylist(id: Long) = viewModelScope.launch {
        playlistRepository.deletePlaylist(id)
    }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
