package com.motomusic.app.presentation.audio

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Equalizer
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.motomusic.app.playback.EqualizerPreset
import com.motomusic.app.playback.EqualizerUiState
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToInt

/**
 * Presets as chips, then one slider per band the device actually has. Moving a slider switches
 * to the custom curve; the change is heard immediately and saved when the finger lifts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    state: EqualizerUiState,
    onDismiss: () -> Unit,
    onEnabledChange: (Boolean) -> Unit,
    onSelectPreset: (EqualizerPreset) -> Unit,
    onBandLevelChange: (band: Int, levelMb: Int) -> Unit,
    onBandLevelChangeFinished: () -> Unit,
    onReset: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            Modifier
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Equalizer, contentDescription = null)
                Spacer(Modifier.width(12.dp))
                Text("Equalizer", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                Switch(checked = state.enabled, onCheckedChange = onEnabledChange)
            }

            Spacer(Modifier.height(8.dp))
            Text(
                text = when {
                    !state.available -> "Play a song to tune the sound. Some devices have no equalizer."
                    state.enabled -> "Pick a preset, or drag the sliders to make your own."
                    else -> "Off. The music plays exactly as it was recorded."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Spacer(Modifier.height(16.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                EqualizerPreset.builtIn.forEach { preset ->
                    FilterChip(
                        selected = state.enabled && state.preset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset.label) },
                    )
                }
                if (state.preset == EqualizerPreset.CUSTOM) {
                    FilterChip(
                        selected = state.enabled,
                        onClick = { onSelectPreset(EqualizerPreset.CUSTOM) },
                        label = { Text(EqualizerPreset.CUSTOM.label) },
                    )
                }
            }

            if (state.available && state.bandCentersHz.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                state.bandCentersHz.forEachIndexed { band, hz ->
                    val level = state.levelsMb.getOrElse(band) { 0 }
                    BandSlider(
                        label = formatFrequency(hz),
                        levelMb = level,
                        minMb = state.minLevelMb,
                        maxMb = state.maxLevelMb,
                        active = state.enabled,
                        onChange = { onBandLevelChange(band, it) },
                        onChangeFinished = onBandLevelChangeFinished,
                    )
                }

                Spacer(Modifier.height(4.dp))
                TextButton(onClick = onReset, modifier = Modifier.align(Alignment.End)) {
                    Text("Reset to flat")
                }
            }
        }
    }
}

@Composable
private fun BandSlider(
    label: String,
    levelMb: Int,
    minMb: Int,
    maxMb: Int,
    active: Boolean,
    onChange: (Int) -> Unit,
    onChangeFinished: () -> Unit,
) {
    // Dimmed while the equalizer is off, but still live: moving a band turns it back on.
    val textColor by animateColorAsState(
        if (active) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
        label = "bandText",
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            modifier = Modifier.width(56.dp),
        )
        Slider(
            value = levelMb.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            onValueChangeFinished = onChangeFinished,
            valueRange = minMb.toFloat()..maxMb.toFloat(),
            modifier = Modifier.weight(1f),
        )
        Text(
            text = formatGain(levelMb),
            style = MaterialTheme.typography.labelMedium,
            color = textColor,
            textAlign = TextAlign.End,
            modifier = Modifier.width(56.dp),
        )
    }
}

private fun formatFrequency(hz: Int): String = when {
    hz >= 10_000 -> "${(hz / 1000f).roundToInt()} kHz"
    hz >= 1_000 -> {
        val khz = hz / 1000f
        if (abs(khz - khz.roundToInt()) < 0.05f) "${khz.roundToInt()} kHz"
        else String.format(Locale.getDefault(), "%.1f kHz", khz)
    }
    else -> "$hz Hz"
}

private fun formatGain(levelMb: Int): String {
    val db = levelMb / 100f
    if (abs(db) < 0.05f) return "0 dB"
    return String.format(Locale.getDefault(), "%+.1f dB", db)
}
