package com.motomusic.app.presentation.folders

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

@Immutable
data class FoldersUiState(
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class FoldersViewModel @Inject constructor(
    songRepository: SongRepository,
) : ViewModel() {

    val state: StateFlow<FoldersUiState> = songRepository.observeFolders()
        .map { FoldersUiState(folders = it, isLoading = false) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), FoldersUiState())

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
