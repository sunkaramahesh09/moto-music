package com.motomusic.app.presentation.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.SectionHeader
import com.motomusic.app.ui.components.SongList

/**
 * Full-screen search. The field takes focus on entry so the keyboard is already up, which is
 * the whole point of arriving here rather than using the search toggle on the songs list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    contentPadding: PaddingValues,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(Unit) { focusRequester.requestFocus() }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Search") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(top = innerPadding.calculateTopPadding())) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp)
                    .focusRequester(focusRequester),
                placeholder = { Text("Songs, artists, albums") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                trailingIcon = {
                    if (state.hasQuery) {
                        IconButton(onClick = onClearQuery) {
                            Icon(Icons.Rounded.Close, contentDescription = "Clear search")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                    onSearch = { keyboard?.hide() },
                ),
            )

            when {
                !state.hasQuery -> EmptyState(
                    icon = Icons.Rounded.Search,
                    title = "Search your library",
                    message = "Find any song, artist or album stored on this device. Nothing you type leaves your phone.",
                )

                state.isEmptyResult -> EmptyState(
                    icon = Icons.Rounded.SearchOff,
                    title = "No matches",
                    message = "Nothing in your library matches \"${state.query}\".",
                )

                else -> SongList(
                    songs = state.results,
                    contentPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding() + 16.dp),
                    header = {
                        SectionHeader(title = pluralise(state.results.size, "result"))
                    },
                )
            }
        }
    }
}
