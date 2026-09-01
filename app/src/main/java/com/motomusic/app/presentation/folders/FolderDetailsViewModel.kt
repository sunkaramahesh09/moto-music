package com.motomusic.app.presentation.folders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class FolderDetailsUiState(
    val path: String = "",
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
) {
    /** Last path segment, which is what the user thinks of as the folder's name. */
    val name: String get() = path.substringAfterLast('/').ifEmpty { path }
}

@HiltViewModel
class FolderDetailsViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    songRepository: SongRepository,
) : ViewModel() {

    private val path: String = checkNotNull(savedStateHandle[Routes.ARG_FOLDER_PATH]) {
        "FolderDetails needs a ${Routes.ARG_FOLDER_PATH} argument"
    }

    val state: StateFlow<FolderDetailsUiState> = songRepository.observeSongsInFolder(path)
        .map { FolderDetailsUiState(path = path, songs = it, isLoading = false) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            FolderDetailsUiState(path = path),
        )

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
