package com.motomusic.app.presentation.albums

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Album
import com.motomusic.app.ui.components.AlbumGridItem
import com.motomusic.app.ui.components.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlbumsScreen(
    state: AlbumsUiState,
    contentPadding: PaddingValues,
    onAlbumClick: (Album) -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = { TopAppBar(title = { Text("Albums") }) },
    ) { innerPadding ->
        when {
            state.isLoading -> Unit

            state.albums.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Album,
                title = "No albums yet",
                message = "Albums appear once music on this device has been scanned.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 150.dp),
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding() + 4.dp,
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                    start = 6.dp,
                    end = 6.dp,
                ),
            ) {
                items(items = state.albums, key = { it.id }) { album ->
                    AlbumGridItem(album = album, onClick = { onAlbumClick(album) })
                }
            }
        }
    }
}
