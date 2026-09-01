package com.motomusic.app.presentation.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val version = remember(context) {
        runCatching {
            context.packageManager.getPackageInfo(context.packageName, 0).versionName
        }.getOrNull() ?: "1.0"
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("About") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = innerPadding.calculateTopPadding())
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            Text("Moto Music", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Version $version",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Paragraph(
                title = "An offline player",
                body = "Moto Music plays the audio files already stored on this device. It asks " +
                    "only for access to your audio files and, on newer Android versions, for " +
                    "permission to show the playback notification.",
            )
            Paragraph(
                title = "Nothing leaves your phone",
                body = "There is no account, no analytics and no advertising. The app holds no " +
                    "internet permission at all, so searches, play counts and playlists cannot " +
                    "be sent anywhere even by accident.",
            )
            Paragraph(
                title = "Your library stays yours",
                body = "Playlists, favourites and listening history are kept in a local database. " +
                    "Uninstalling the app removes them and leaves your music files untouched.",
            )
            Paragraph(
                title = "Free and open source",
                body = "Moto Music is released under the GNU General Public License v3.0. You " +
                    "may use, study, change and share it, and any version someone distributes " +
                    "has to stay free software too — which is what stops it coming back to you " +
                    "with advertising in it.",
            )
            Paragraph(
                title = "An independent project",
                body = "This app is not affiliated with, endorsed by or connected to Motorola " +
                    "or Lenovo. It is a hobby project that happens to have been written on a " +
                    "Moto phone.",
            )
            Paragraph(
                title = "Built with",
                body = "Jetpack Compose and Material 3, Media3 ExoPlayer for playback, Room for " +
                    "local storage, Hilt for dependency injection and DataStore for settings — " +
                    "all released by their authors under the Apache License 2.0.",
            )
        }
    }
}

@Composable
private fun Paragraph(title: String, body: String) {
    Spacer(Modifier.height(24.dp))
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(6.dp))
    Text(
        text = body,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
