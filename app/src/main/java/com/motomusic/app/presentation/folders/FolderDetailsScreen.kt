package com.motomusic.app.presentation.folders

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.motomusic.app.presentation.common.CollectionScreen
import com.motomusic.app.presentation.common.songsSummary
import com.motomusic.app.ui.components.CollectionHeader

@Composable
fun FolderDetailsScreen(
    state: FolderDetailsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    CollectionScreen(
        title = state.name,
        songs = state.songs,
        contentPadding = contentPadding,
        onBack = onBack,
        modifier = modifier,
        isLoading = state.isLoading,
        emptyIcon = Icons.Rounded.Folder,
        emptyTitle = "Folder is empty",
        emptyMessage = "There is no music in ${state.path} any more.",
        header = {
            CollectionHeader(
                title = state.name,
                subtitle = "${state.path} · ${songsSummary(state.songs)}",
                icon = Icons.Rounded.Folder,
            )
        },
    )
}
