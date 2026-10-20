package com.motomusic.app.presentation.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.ui.components.CapsuleChip
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.LargeTitleHeader
import com.motomusic.app.ui.components.PressableCard
import com.motomusic.app.ui.components.SectionHeader
import com.motomusic.app.ui.components.SongList
import com.motomusic.app.ui.components.TileTints

/**
 * The Search tab. Before anything is typed it offers recent searches and ways to browse; as soon
 * as there is a query it shows matching songs. Everything comes from the on-device library.
 *
 * [focusRequest] changes when another screen sends the user here to type — the Library's search
 * bar — and the field then takes focus; arriving through the tab bar leaves the keyboard down.
 */
@Composable
fun SearchScreen(
    state: SearchUiState,
    contentPadding: PaddingValues,
    focusRequest: Int,
    onFocusHandled: () -> Unit,
    onQueryChange: (String) -> Unit,
    onClearQuery: () -> Unit,
    onRememberQuery: () -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val actions = LocalSongActions.current
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(focusRequest) {
        if (focusRequest > 0) {
            focusRequester.requestFocus()
            onFocusHandled()
        }
    }

    Column(modifier.fillMaxSize()) {
        LargeTitleHeader(title = "Search")

        SearchField(
            query = state.query,
            onQueryChange = onQueryChange,
            onClear = onClearQuery,
            onSearch = {
                onRememberQuery()
                keyboard?.hide()
            },
            focusRequester = focusRequester,
        )

        val bottom = contentPadding.calculateBottomPadding() + 24.dp
        when {
            !state.hasQuery -> SearchHome(
                recentSearches = state.recentSearches,
                bottomPadding = bottom,
                onRecentClick = onQueryChange,
                onRemoveRecent = onRemoveRecent,
                onClearRecents = onClearRecents,
                onOpenAlbums = onOpenAlbums,
                onOpenArtists = onOpenArtists,
                onOpenPlaylists = onOpenPlaylists,
                onOpenFolders = onOpenFolders,
            )

            state.isEmptyResult -> EmptyState(
                icon = Icons.Rounded.SearchOff,
                title = "No matches",
                message = "Nothing in your library matches \"${state.query}\".",
            )

            else -> SongList(
                songs = state.results,
                contentPadding = PaddingValues(bottom = bottom),
                header = { SectionHeader(title = pluralise(state.results.size, "result")) },
                onSongClick = { index ->
                    // Playing a result is the clearest sign the search worked, so it is kept.
                    onRememberQuery()
                    actions.play(state.results, index)
                },
            )
        }
    }
}

/** The design's capsule search bar, as a real text field. */
@Composable
private fun SearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    onClear: () -> Unit,
    onSearch: () -> Unit,
    focusRequester: FocusRequester,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .heightIn(min = 48.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(start = 16.dp, end = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Search,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(10.dp))
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
            if (query.isEmpty()) {
                Text(
                    text = "Songs, artists, albums",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                )
            }
            BasicTextField(
                value = query,
                onValueChange = onQueryChange,
                singleLine = true,
                textStyle = MaterialTheme.typography.bodyLarge.copy(color = MaterialTheme.colorScheme.onSurface),
                cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { onSearch() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focusRequester),
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = onClear) {
                Icon(Icons.Rounded.Close, contentDescription = "Clear search")
            }
        } else {
            Spacer(Modifier.height(48.dp))
        }
    }
}

@Composable
private fun SearchHome(
    recentSearches: List<String>,
    bottomPadding: androidx.compose.ui.unit.Dp,
    onRecentClick: (String) -> Unit,
    onRemoveRecent: (String) -> Unit,
    onClearRecents: () -> Unit,
    onOpenAlbums: () -> Unit,
    onOpenArtists: () -> Unit,
    onOpenPlaylists: () -> Unit,
    onOpenFolders: () -> Unit,
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = bottomPadding),
    ) {
        if (recentSearches.isNotEmpty()) {
            item(key = "recent-header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 4.dp, top = 20.dp, bottom = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Recent searches",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onClearRecents) { Text("Clear all") }
                }
            }
            item(key = "recent") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(recentSearches, key = { it }) { query ->
                        CapsuleChip(
                            label = query,
                            onClick = { onRecentClick(query) },
                            icon = Icons.Rounded.History,
                            trailing = {
                                IconButton(onClick = { onRemoveRecent(query) }, modifier = Modifier.size(32.dp)) {
                                    Icon(
                                        Icons.Rounded.Close,
                                        contentDescription = "Remove \"$query\" from recent searches",
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        item(key = "browse-header") { SectionHeader(title = "Browse your library") }
        item(key = "browse") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryCard("Albums", "Every record", Icons.Rounded.Album, TileTints.Blue, onOpenAlbums, Modifier.weight(1f))
                    CategoryCard("Artists", "Who you listen to", Icons.Rounded.Person, TileTints.Purple, onOpenArtists, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    CategoryCard(
                        "Playlists",
                        "Made by you",
                        Icons.AutoMirrored.Rounded.QueueMusic,
                        TileTints.Rose,
                        onOpenPlaylists,
                        Modifier.weight(1f),
                    )
                    CategoryCard("Folders", "Where files live", Icons.Rounded.Folder, TileTints.Orange, onOpenFolders, Modifier.weight(1f))
                }
            }
        }
    }
}

/**
 * A genre-card look-alike: a tinted gradient with the title on top and a large glyph tilted into
 * the bottom-right corner, clipped by the card's curve.
 */
@Composable
private fun CategoryCard(
    title: String,
    tagline: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    PressableCard(onClick = onClick, cornerRadius = 22.dp, modifier = modifier.height(108.dp)) {
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(tint.copy(alpha = 0.55f), tint.copy(alpha = 0.12f)))),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.35f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 14.dp, y = 14.dp)
                    .size(84.dp)
                    .rotate(15f),
            )
            Column(Modifier.padding(16.dp)) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                Text(
                    text = tagline.uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f),
                )
            }
        }
    }
}
