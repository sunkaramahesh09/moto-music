package com.motomusic.app.presentation.search

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.Song
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
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SearchUiState(
    val query: String = "",
    val results: List<Song> = emptyList(),
    val isSearching: Boolean = false,
    val recentSearches: List<String> = emptyList(),
) {
    val hasQuery: Boolean get() = query.isNotBlank()
    val isEmptyResult: Boolean get() = hasQuery && !isSearching && results.isEmpty()
}

/**
 * Local-only search. Every match comes from the on-device database — nothing is ever sent
 * anywhere, and there is no network code to send it with.
 */
@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
) : ViewModel() {

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val results: StateFlow<List<Song>> = _query
        .debounce { if (it.isBlank()) 0L else SEARCH_DEBOUNCE_MS }
        .distinctUntilChanged()
        .flatMapLatest { query ->
            if (query.isBlank()) flowOf(emptyList()) else songRepository.search(query)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), emptyList())

    val state: StateFlow<SearchUiState> = combine(
        _query,
        results,
        settingsRepository.preferences.map { it.recentSearches }.distinctUntilChanged(),
    ) { query, results, recent ->
        SearchUiState(query = query, results = results, isSearching = false, recentSearches = recent)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SearchUiState())

    fun onQueryChange(value: String) {
        _query.value = value
    }

    fun clearQuery() {
        _query.value = ""
    }

    /** Remembers the current query, once the user has shown it found what they wanted. */
    fun rememberQuery() {
        val query = _query.value.trim()
        if (query.length < MIN_REMEMBERED_LENGTH) return
        viewModelScope.launch { settingsRepository.addRecentSearch(query) }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch { settingsRepository.removeRecentSearch(query) }
    }

    fun clearRecentSearches() {
        viewModelScope.launch { settingsRepository.clearRecentSearches() }
    }

    private companion object {
        const val SEARCH_DEBOUNCE_MS = 180L
        const val MIN_REMEMBERED_LENGTH = 2
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
