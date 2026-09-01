package com.motomusic.app.presentation.artists

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class ArtistsUiState(
    val artists: List<Artist> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class ArtistsViewModel @Inject constructor(
    songRepository: SongRepository,
) : ViewModel() {

    val state: StateFlow<ArtistsUiState> = songRepository.observeArtists()
        .map { ArtistsUiState(artists = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), ArtistsUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
