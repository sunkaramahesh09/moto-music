package com.motomusic.app.playback

import android.media.audiofx.Equalizer
import android.util.Log
import androidx.compose.runtime.Immutable
import com.motomusic.app.di.ApplicationScope
import com.motomusic.app.domain.model.EqualizerSettings
import com.motomusic.app.domain.repository.SettingsRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.plus

@Immutable
data class EqualizerUiState(
    /** False until the service has attached to a player, or when the device offers no equalizer. */
    val available: Boolean = false,
    val enabled: Boolean = false,
    val preset: EqualizerPreset = EqualizerPreset.FLAT,
    val bandCentersHz: List<Int> = emptyList(),
    val levelsMb: List<Int> = emptyList(),
    val minLevelMb: Int = -1500,
    val maxLevelMb: Int = 1500,
)

/**
 * The platform equalizer, attached to the player's audio session.
 *
 * The [MusicService] calls [attach] whenever its player gets a new audio session and [release]
 * when it goes away; the screens only ever read [state] and send changes. Everything is applied
 * immediately, and slider drags are persisted only once the finger lifts ([commitCustomLevels]),
 * so a drag does not turn into sixty settings writes a second.
 */
@Singleton
class EqualizerController @Inject constructor(
    private val settingsRepository: SettingsRepository,
    @param:ApplicationScope private val scope: CoroutineScope,
) {

    private val _state = MutableStateFlow(EqualizerUiState())
    val state: StateFlow<EqualizerUiState> = _state.asStateFlow()

    private var equalizer: Equalizer? = null
    private var settings = EqualizerSettings()

    init {
        settingsRepository.preferences
            .map { it.equalizer }
            .distinctUntilChanged()
            .onEach { stored ->
                settings = stored
                apply()
            }
            .launchIn(scope + Dispatchers.Main.immediate)
    }

    /** Must be called on the main thread, from the player's own looper. */
    fun attach(audioSessionId: Int) {
        release()
        if (audioSessionId <= 0) return
        equalizer = runCatching { Equalizer(EFFECT_PRIORITY, audioSessionId) }
            .onFailure { Log.w(TAG, "No equalizer for session $audioSessionId", it) }
            .getOrNull()
        apply()
    }

    fun release() {
        equalizer?.let { runCatching { it.release() } }
        equalizer = null
        _state.value = _state.value.copy(available = false)
    }

    fun setEnabled(enabled: Boolean) {
        settings = settings.copy(enabled = enabled)
        apply()
        scope.launch { settingsRepository.setEqualizerEnabled(enabled) }
    }

    fun selectPreset(preset: EqualizerPreset) {
        settings = settings.copy(presetKey = preset.key, enabled = true)
        apply()
        scope.launch {
            settingsRepository.setEqualizerEnabled(true)
            settingsRepository.setEqualizerPreset(preset.key)
        }
    }

    /** Moves one band, turning whatever curve is showing into the custom preset. */
    fun setBandLevel(band: Int, levelMb: Int) {
        val current = _state.value
        if (band !in current.levelsMb.indices) return
        val levels = current.levelsMb.toMutableList()
        levels[band] = levelMb.coerceIn(current.minLevelMb, current.maxLevelMb)
        settings = settings.copy(enabled = true, presetKey = EqualizerPreset.CUSTOM.key, customLevelsMb = levels)
        apply()
    }

    fun commitCustomLevels() {
        val snapshot = settings
        scope.launch {
            settingsRepository.setEqualizerEnabled(snapshot.enabled)
            settingsRepository.setEqualizerPreset(snapshot.presetKey, snapshot.customLevelsMb)
        }
    }

    fun resetToFlat() = selectPreset(EqualizerPreset.FLAT)

    private fun apply() {
        val eq = equalizer
        val preset = EqualizerPreset.fromKey(settings.presetKey)
        if (eq == null) {
            _state.value = _state.value.copy(enabled = settings.enabled, preset = preset)
            return
        }
        runCatching {
            val bandCount = eq.numberOfBands.toInt()
            val range = eq.bandLevelRange
            val minMb = range[0].toInt()
            val maxMb = range[1].toInt()
            val centers = (0 until bandCount).map { eq.getCenterFreq(it.toShort()) / 1000 }

            val custom = settings.customLevelsMb
            val levels = if (preset == EqualizerPreset.CUSTOM && custom.size == bandCount) {
                custom.map { it.coerceIn(minMb, maxMb) }
            } else {
                // A custom curve from another device (or none at all) has nothing to stand on.
                val base = if (preset == EqualizerPreset.CUSTOM) EqualizerPreset.FLAT else preset
                base.levelsFor(centers, minMb, maxMb)
            }

            levels.forEachIndexed { band, level -> eq.setBandLevel(band.toShort(), level.toShort()) }
            eq.enabled = settings.enabled

            _state.value = EqualizerUiState(
                available = true,
                enabled = settings.enabled,
                preset = preset,
                bandCentersHz = centers,
                levelsMb = levels,
                minLevelMb = minMb,
                maxLevelMb = maxMb,
            )
        }.onFailure {
            Log.w(TAG, "Could not apply equalizer settings", it)
        }
    }

    private companion object {
        const val TAG = "EqualizerController"

        /** Normal priority: a system-wide effect the user set up elsewhere still takes precedence. */
        const val EFFECT_PRIORITY = 0
    }
}
