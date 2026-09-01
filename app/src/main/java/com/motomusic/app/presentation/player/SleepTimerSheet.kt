package com.motomusic.app.presentation.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material3.AssistChip
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.motomusic.app.core.formatDuration
import com.motomusic.app.playback.SleepTimerState

/** Stops playback after a set time, or at the end of whatever is playing. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SleepTimerSheet(
    state: SleepTimerState,
    onDismiss: () -> Unit,
    onStart: (Int) -> Unit,
    onEndOfTrack: () -> Unit,
    onCancel: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(Modifier.padding(horizontal = 24.dp).padding(bottom = 32.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Bedtime, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Sleep timer", style = MaterialTheme.typography.titleMedium)
            }

            Spacer(Modifier.height(8.dp))

            Text(
                text = when (state) {
                    is SleepTimerState.Off -> "Music keeps playing until you stop it."
                    is SleepTimerState.Countdown -> "Pausing in ${formatDuration(state.remainingMs)}."
                    is SleepTimerState.EndOfTrack -> "Pausing at the end of this track."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRESET_MINUTES.forEach { minutes ->
                    AssistChip(
                        onClick = {
                            onStart(minutes)
                            onDismiss()
                        },
                        label = { Text("$minutes min") },
                    )
                }
                AssistChip(
                    onClick = {
                        onEndOfTrack()
                        onDismiss()
                    },
                    label = { Text("End of track") },
                    leadingIcon = { Icon(Icons.Rounded.MusicNote, contentDescription = null) },
                )
            }

            if (state !is SleepTimerState.Off) {
                Spacer(Modifier.height(8.dp))
                TextButton(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                ) { Text("Turn off timer") }
            }
        }
    }
}

private val PRESET_MINUTES = listOf(15, 30, 45, 60, 90)
