package com.motomusic.app.presentation.audio

import android.content.ActivityNotFoundException
import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoMode
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cable
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Hearing
import androidx.compose.material.icons.rounded.Speaker
import androidx.compose.material.icons.rounded.SpeakerGroup
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material.icons.rounded.Usb
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.motomusic.app.playback.AudioOutput
import com.motomusic.app.playback.AudioOutputKind
import com.motomusic.app.playback.AudioOutputState

/**
 * Where the music plays. Lists the outputs that are connected right now; anything that still
 * needs connecting is handed to Android's own switcher, or to Bluetooth settings before 14.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioOutputSheet(
    state: AudioOutputState,
    canShowSystemSwitcher: Boolean,
    onDismiss: () -> Unit,
    onSelect: (Int?) -> Unit,
    onShowSystemSwitcher: () -> Boolean,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(bottom = 24.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 24.dp),
            ) {
                Icon(Icons.Rounded.SpeakerGroup, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Play on", style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))

            OutputRow(
                icon = Icons.Rounded.AutoMode,
                title = "Automatic",
                subtitle = state.outputs.firstOrNull { it.id == state.activeId }
                    ?.let { "Android's choice · ${it.name}" }
                    ?: "Let Android pick the newest device",
                selected = state.selectedId == null,
                onClick = {
                    onSelect(null)
                    onDismiss()
                },
            )

            state.outputs.forEach { output ->
                OutputRow(
                    icon = output.kind.icon,
                    title = output.name,
                    subtitle = when {
                        output.id == state.activeId -> "Playing here now"
                        else -> output.kind.label
                    },
                    selected = output.id == state.selectedId,
                    onClick = {
                        onSelect(output.id)
                        onDismiss()
                    },
                )
            }

            HorizontalDivider(Modifier.padding(vertical = 8.dp))

            OutputRow(
                icon = Icons.Rounded.Bluetooth,
                title = "Connect a device",
                subtitle = if (canShowSystemSwitcher) "Open Android's output switcher" else "Open Bluetooth settings",
                selected = false,
                onClick = {
                    if (!onShowSystemSwitcher()) {
                        try {
                            context.startActivity(
                                Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            )
                        } catch (_: ActivityNotFoundException) {
                            // No Bluetooth settings on this device; nothing more to offer.
                        }
                        onDismiss()
                    }
                },
            )
        }
    }
}

@Composable
private fun OutputRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 24.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            shape = CircleShape,
            color = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surfaceContainerHighest
            },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.padding(8.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        Spacer(Modifier.width(16.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = MaterialTheme.colorScheme.primary)
        }
    }
}

/**
 * The output chip in the full player: shows where the music is going and opens the picker.
 * It sits at the bottom left, where a thumb rests, rather than up in the crowded top bar.
 */
@Composable
fun AudioOutputChip(
    output: AudioOutput?,
    pinned: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = output?.name ?: "This phone"
    val tint = if (pinned) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
            .heightIn(min = 40.dp)
            .semantics { contentDescription = "Playing on $name. Change output." },
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 12.dp, end = 16.dp, top = 8.dp, bottom = 8.dp),
        ) {
            Icon(
                imageVector = output?.kind?.icon ?: Icons.Rounded.Speaker,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = name,
                style = MaterialTheme.typography.labelLarge,
                color = tint,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.widthIn(max = 160.dp),
            )
        }
    }
}

val AudioOutputKind.icon: ImageVector
    get() = when (this) {
        AudioOutputKind.SPEAKER -> Icons.Rounded.Speaker
        AudioOutputKind.WIRED -> Icons.Rounded.Headphones
        AudioOutputKind.BLUETOOTH -> Icons.Rounded.Bluetooth
        AudioOutputKind.USB -> Icons.Rounded.Usb
        AudioOutputKind.HDMI -> Icons.Rounded.Tv
        AudioOutputKind.HEARING_AID -> Icons.Rounded.Hearing
        AudioOutputKind.OTHER -> Icons.Rounded.Cable
    }

private val AudioOutputKind.label: String
    get() = when (this) {
        AudioOutputKind.SPEAKER -> "Phone speaker"
        AudioOutputKind.WIRED -> "Wired"
        AudioOutputKind.BLUETOOTH -> "Bluetooth"
        AudioOutputKind.USB -> "USB audio"
        AudioOutputKind.HDMI -> "HDMI"
        AudioOutputKind.HEARING_AID -> "Hearing aid"
        AudioOutputKind.OTHER -> "Audio output"
    }
