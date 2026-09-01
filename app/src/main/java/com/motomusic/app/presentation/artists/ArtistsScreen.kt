package com.motomusic.app.presentation.artists

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Artist
import com.motomusic.app.ui.components.ArtistRow
import com.motomusic.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ArtistsScreen(
    state: ArtistsUiState,
    contentPadding: PaddingValues,
    onArtistClick: (Artist) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Artists") }) },
    ) { innerPadding ->
        when {
            state.isLoading -> Unit

            state.artists.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Person,
                title = "No artists yet",
                message = "Artists appear once music on this device has been scanned.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                items(items = state.artists, key = { it.id }) { artist ->
                    ArtistRow(artist = artist, onClick = { onArtistClick(artist) })
                }
            }
        }
    }
}
