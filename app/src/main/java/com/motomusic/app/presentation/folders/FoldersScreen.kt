package com.motomusic.app.presentation.folders

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.domain.model.Folder
import com.motomusic.app.ui.components.EmptyState
import com.motomusic.app.ui.components.FolderRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoldersScreen(
    state: FoldersUiState,
    contentPadding: PaddingValues,
    onFolderClick: (Folder) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Folders") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            state.isLoading -> Unit

            state.folders.isEmpty() -> EmptyState(
                icon = Icons.Rounded.Folder,
                title = "No folders",
                message = "Folders appear once music on this device has been scanned.",
                modifier = Modifier.fillMaxSize().padding(innerPadding),
            )

            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(
                    top = innerPadding.calculateTopPadding(),
                    bottom = contentPadding.calculateBottomPadding() + 16.dp,
                ),
            ) {
                items(items = state.folders, key = { it.path }) { folder ->
                    FolderRow(folder = folder, onClick = { onFolderClick(folder) })
                }
            }
        }
    }
}
