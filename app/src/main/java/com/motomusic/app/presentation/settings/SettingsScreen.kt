package com.motomusic.app.presentation.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.pluralise
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.ui.theme.supportsDynamicColor

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onDynamicColorChange: (Boolean) -> Unit,
    onResumeLastSessionChange: (Boolean) -> Unit,
    onSkipSilenceChange: (Boolean) -> Unit,
    onPauseOnHeadphonesChange: (Boolean) -> Unit,
    onFadeOnPlayPauseChange: (Boolean) -> Unit,
    onHideVoiceRecordingsChange: (Boolean) -> Unit,
    onUnhideSong: (String) -> Unit,
    onUnhideAllSongs: () -> Unit,
    onRescan: () -> Unit,
    onClearHistory: () -> Unit,
    onOpenAbout: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val preferences = state.preferences
    var themeDialogOpen by remember { mutableStateOf(false) }
    var clearHistoryOpen by remember { mutableStateOf(false) }
    var hiddenSongsOpen by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
                .padding(bottom = contentPadding.calculateBottomPadding() + 24.dp),
        ) {
            SettingsSection("Appearance")
            SettingsRow(
                title = "Theme",
                subtitle = preferences.themeMode.label,
                onClick = { themeDialogOpen = true },
            )
            if (supportsDynamicColor) {
                SettingsToggle(
                    title = "Colours from wallpaper",
                    subtitle = "Match Material You colours on this device",
                    checked = preferences.useDynamicColor,
                    onCheckedChange = onDynamicColorChange,
                )
            }

            HorizontalDivider()
            SettingsSection("Playback")
            SettingsToggle(
                title = "Resume last session",
                subtitle = "Reload the previous queue when the app opens",
                checked = preferences.resumeLastSession,
                onCheckedChange = onResumeLastSessionChange,
            )
            SettingsToggle(
                title = "Skip silence",
                subtitle = "Shorten silent passages between and inside tracks",
                checked = preferences.skipSilence,
                onCheckedChange = onSkipSilenceChange,
            )
            SettingsToggle(
                title = "Pause when headphones disconnect",
                subtitle = "Stop the music instead of playing out loud",
                checked = preferences.pauseOnHeadphonesDisconnect,
                onCheckedChange = onPauseOnHeadphonesChange,
            )
            SettingsToggle(
                title = "Fade in and out",
                subtitle = "Ease the volume up on play and down on pause",
                checked = preferences.fadeOnPlayPause,
                onCheckedChange = onFadeOnPlayPauseChange,
            )

            HorizontalDivider()
            SettingsSection("Library")
            SettingsToggle(
                title = "Hide voice notes and recordings",
                subtitle = "Leave out WhatsApp voice notes, call and voice-recorder files",
                checked = preferences.hideVoiceRecordings,
                onCheckedChange = onHideVoiceRecordingsChange,
            )
            SettingsRow(
                title = "Hidden songs",
                subtitle = if (preferences.hiddenSongKeys.isEmpty()) {
                    "Nothing hidden. Use a song's ⋮ menu to hide one."
                } else {
                    "${pluralise(preferences.hiddenSongKeys.size, "file")} kept out of the library"
                },
                enabled = preferences.hiddenSongKeys.isNotEmpty(),
                onClick = { hiddenSongsOpen = true },
            )
            SettingsRow(
                title = "Scan for music",
                subtitle = when (val scan = state.scanState) {
                    is ScanState.Scanning -> "Scanning… ${scan.processed} of ${scan.total}"
                    is ScanState.Finished ->
                        "${pluralise(state.songCount, "song")} · " +
                            "${scan.added} added, ${scan.removed} removed last time"
                    is ScanState.Failed -> "Last scan failed: ${scan.message}"
                    ScanState.Idle -> pluralise(state.songCount, "song")
                },
                enabled = !state.isScanning,
                onClick = onRescan,
            )
            SettingsRow(
                title = "Clear listening history",
                subtitle = "Forget recently and frequently played",
                onClick = { clearHistoryOpen = true },
            )

            HorizontalDivider()
            SettingsSection("About")
            SettingsRow(
                title = "About Moto Music",
                subtitle = "Version, licences and what this app does not do",
                onClick = onOpenAbout,
            )
        }
    }

    if (themeDialogOpen) {
        ThemeModeDialog(
            selected = preferences.themeMode,
            onSelect = onThemeModeChange,
            onDismiss = { themeDialogOpen = false },
        )
    }

    if (hiddenSongsOpen) {
        HiddenSongsDialog(
            hiddenKeys = preferences.hiddenSongKeys,
            onUnhide = onUnhideSong,
            onUnhideAll = {
                onUnhideAllSongs()
                hiddenSongsOpen = false
            },
            onDismiss = { hiddenSongsOpen = false },
        )
    }

    if (clearHistoryOpen) {
        AlertDialog(
            onDismissRequest = { clearHistoryOpen = false },
            title = { Text("Clear listening history?") },
            text = { Text("Play counts and recently played songs are forgotten. Your music stays put.") },
            confirmButton = {
                TextButton(onClick = {
                    onClearHistory()
                    clearHistoryOpen = false
                }) { Text("Clear") }
            },
            dismissButton = {
                TextButton(onClick = { clearHistoryOpen = false }) { Text("Cancel") }
            },
        )
    }
}

/**
 * The way back from "Hide from library". Files are listed by name — the rest of the path is
 * how they are stored, not something worth showing — and restoring one rescans the library.
 */
@Composable
private fun HiddenSongsDialog(
    hiddenKeys: Set<String>,
    onUnhide: (String) -> Unit,
    onUnhideAll: () -> Unit,
    onDismiss: () -> Unit,
) {
    val keys = remember(hiddenKeys) { hiddenKeys.sortedBy { it.substringAfterLast('/').lowercase() } }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Hidden songs") },
        text = {
            LazyColumn(Modifier.heightIn(max = 320.dp)) {
                items(keys, key = { it }) { key ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = key.substringAfterLast('/').ifEmpty { key },
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f),
                        )
                        TextButton(onClick = { onUnhide(key) }) { Text("Restore") }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onUnhideAll) { Text("Restore all") }
        },
    )
}

@Composable
private fun ThemeModeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSelect(mode)
                                onDismiss()
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun SettingsSection(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = 20.dp, bottom = 4.dp),
    )
}

@Composable
private fun SettingsRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(enabled = enabled, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 14.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = if (enabled) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
        )
        Spacer(Modifier.height(2.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SettingsToggle(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 24.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(2.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
