package com.motomusic.app.presentation.collection

import androidx.compose.runtime.Immutable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.repository.FavoritesRepository
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.navigation.Routes
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SongCollectionUiState(
    val collection: SongCollection = SongCollection.FAVORITES,
    val songs: List<Song> = emptyList(),
    val isLoading: Boolean = true,
)

@HiltViewModel
class SongCollectionViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    songRepository: SongRepository,
    favoritesRepository: FavoritesRepository,
    private val historyRepository: PlaybackHistoryRepository,
) : ViewModel() {

    val collection: SongCollection =
        SongCollection.fromKey(savedStateHandle[Routes.ARG_COLLECTION_TYPE])

    val state: StateFlow<SongCollectionUiState> = when (collection) {
        SongCollection.FAVORITES -> favoritesRepository.observeFavorites()
        SongCollection.RECENTLY_PLAYED -> historyRepository.observeRecentlyPlayed(HISTORY_LIMIT)
        SongCollection.MOST_PLAYED -> historyRepository.observeMostPlayed(HISTORY_LIMIT)
        SongCollection.RECENTLY_ADDED -> songRepository.observeRecentlyAdded(RECENTLY_ADDED_LIMIT)
    }
        .map { SongCollectionUiState(collection = collection, songs = it, isLoading = false) }
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS),
            SongCollectionUiState(collection = collection),
        )

    /** Offered on the history screens only, where a stale list is worth being able to wipe. */
    fun clearHistory() = viewModelScope.launch { historyRepository.clearHistory() }

    private companion object {
        const val HISTORY_LIMIT = 100
        const val RECENTLY_ADDED_LIMIT = 100
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
