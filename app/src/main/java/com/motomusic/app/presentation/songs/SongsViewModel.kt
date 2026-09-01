package com.motomusic.app.presentation.songs

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.Song
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SongsUiState(
    val songs: List<Song> = emptyList(),
    val sortOrder: SortOrder = SortOrder.TITLE_ASC,
    val query: String = "",
    val isLoading: Boolean = true,
    val scanState: ScanState = ScanState.Idle,
) {
    val isSearching: Boolean get() = query.isNotBlank()
}

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SongsViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val sortOrder: StateFlow<SortOrder> = settingsRepository.preferences
        .map { it.songSortOrder }
        .distinctUntilChanged()
        .stateIn(viewModelScope, SharingStarted.Eagerly, SortOrder.TITLE_ASC)

    /**
     * Typing swaps the source of truth from the sorted library query to the search query.
     * [flatMapLatest] cancels the previous query so only the newest keystroke hits the database.
     */
    private val songs: StateFlow<List<Song>?> = combine(
        _query.debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }.distinctUntilChanged(),
        sortOrder,
    ) { query, order -> query to order }
        .flatMapLatest { (query, order) ->
            if (query.isBlank()) songRepository.observeSongs(order) else songRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), null)

    val state: StateFlow<SongsUiState> = combine(
        songs,
        sortOrder,
        _query,
        songRepository.scanState,
    ) { songs, order, query, scanState ->
        SongsUiState(
            songs = songs.orEmpty(),
            sortOrder = order,
            query = query,
            isLoading = songs == null,
            scanState = scanState,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SongsUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    fun setSortOrder(order: SortOrder) {
        viewModelScope.launch { settingsRepository.setSongSortOrder(order) }
    }

    fun rescan() {
        viewModelScope.launch { songRepository.rescan() }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
