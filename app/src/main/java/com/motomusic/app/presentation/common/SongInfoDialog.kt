package com.motomusic.app.presentation.common

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.motomusic.app.core.formatBitrate
import com.motomusic.app.core.formatDuration
import com.motomusic.app.core.formatFileSize
import com.motomusic.app.core.formatSampleRate
import com.motomusic.app.domain.model.Song

/**
 * Everything the app knows about one file. Sample rate, channels and an exact bitrate are read
 * from the file itself when the dialog opens, so they appear a moment after the rest.
 */
@Composable
fun SongInfoDialog(
    song: Song,
    onDismiss: () -> Unit,
    viewModel: SongInfoViewModel = hiltViewModel(),
) {
    val details by viewModel.details.collectAsStateWithLifecycle()

    LaunchedEffect(song.id) { viewModel.load(song.uri) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Song details") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState())) {
                InfoRow("Title", song.title)
                InfoRow("Artist", song.artist)
                InfoRow("Album", song.album)
                if (song.trackNumber > 0) InfoRow("Track", song.trackNumber.toString())
                if (song.year > 0) InfoRow("Year", song.year.toString())
                InfoRow("Duration", formatDuration(song.durationMs))
                InfoRow("Format", song.mimeType.substringAfter('/').uppercase())
                InfoRow("Size", formatFileSize(song.sizeBytes))
                InfoRow("Bitrate", formatBitrate(details?.bitrateBps ?: song.bitrateBps) ?: "Unknown")
                InfoRow("Sample rate", formatSampleRate(details?.sampleRateHz) ?: "Unknown")
                InfoRow(
                    label = "Channels",
                    value = when (details?.channelCount) {
                        null -> "Unknown"
                        1 -> "Mono"
                        2 -> "Stereo"
                        else -> "${details?.channelCount} channels"
                    },
                )
                InfoRow("File", song.fileName)
                if (song.folderPath.isNotBlank()) InfoRow("Folder", song.folderPath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(96.dp),
        )
        Spacer(Modifier.width(12.dp))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
    }
    Spacer(Modifier.height(8.dp))
}
