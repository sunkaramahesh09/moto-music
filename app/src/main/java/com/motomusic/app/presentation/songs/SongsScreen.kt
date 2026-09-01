package com.motomusic.app.presentation.songs

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.ListActionRow
import com.motomusic.app.ui.components.ScanProgressBar
import com.motomusic.app.ui.components.SongList

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SongsScreen(
    state: SongsUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onSortOrderChange: (SortOrder) -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalSongActions.current
    var searchVisible by remember { mutableStateOf(false) }
    var sortMenuOpen by remember { mutableStateOf(false) }
    // Reset the scroll position when the list content changes meaning entirely.
    val listState = rememberLazyListState()

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Songs") },
                actions = {
                    IconButton(onClick = {
                        searchVisible = !searchVisible
                        if (!searchVisible) onClearQuery()
                    }) {
                        Icon(
                            imageVector = if (searchVisible) Icons.Rounded.Close else Icons.Rounded.Search,
                            contentDescription = if (searchVisible) "Close search" else "Search songs",
                        )
                    }
                    IconButton(onClick = { sortMenuOpen = true }) {
                        Icon(Icons.AutoMirrored.Rounded.Sort, contentDescription = "Change sort order")
                    }
                    DropdownMenu(expanded = sortMenuOpen, onDismissRequest = { sortMenuOpen = false }) {
                        SortOrder.entries.forEach { order ->
                            DropdownMenuItem(
                                text = { Text(order.label) },
                                onClick = {
                                    onSortOrderChange(order)
                                    sortMenuOpen = false
                                },
                                trailingIcon = {
                                    if (order == state.sortOrder) {
                                        Icon(Icons.Rounded.Check, contentDescription = "Selected")
                                    }
                                },
                            )
                        }
                    }
                    IconButton(onClick = onRescan) {
                        Icon(Icons.Rounded.Refresh, contentDescription = "Scan for new music")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            if (searchVisible) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = onQueryChange,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
                    placeholder = { Text("Search title, artist or album") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    trailingIcon = {
                        if (state.query.isNotEmpty()) {
                            IconButton(onClick = onClearQuery) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = ImeAction.Search,
                    ),
                )
            }

            val scanState = state.scanState
            if (scanState is ScanState.Scanning) {
                ScanProgressBar(processed = scanState.processed, total = scanState.total)
            }

            when {
                state.isLoading -> Unit

                state.songs.isEmpty() && state.isSearching -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No matches",
                    message = "Nothing in your library matches \"${state.query}\".",
                )

                state.songs.isEmpty() -> EmptyState(
                    icon = Icons.Rounded.MusicNote,
                    title = "No music found",
                    message = "Music stored on your device will appear here.",
                    actionLabel = "Scan for music",
                    onAction = onRescan,
                )

                else -> SongList(
                    songs = state.songs,
                    listState = listState,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
                    header = if (state.isSearching) {
                        null
                    } else {
                        {
                            ListActionRow(
                                songCount = state.songs.size,
                                onPlay = { actions.play(state.songs, 0) },
                                onShuffle = { actions.shuffle(state.songs) },
                            )
                        }
                    },
                )
            }
        }
    }
}
