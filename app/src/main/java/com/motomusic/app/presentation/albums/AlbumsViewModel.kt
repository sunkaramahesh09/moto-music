package com.motomusic.app.presentation.albums

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Album
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class AlbumsUiState(
    val albums: List<Album> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class AlbumsViewModel @Inject constructor(
    songRepository: SongRepository,
) : ViewModel() {

    val state: StateFlow<AlbumsUiState> = songRepository.observeAlbums()
        .map { AlbumsUiState(albums = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), AlbumsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
