package com.motomusic.app.presentation.audio

import androidx.lifecycle.ViewModel
import com.motomusic.app.playback.AudioOutputController
import com.motomusic.app.playback.AudioOutputState
import com.motomusic.app.playback.EqualizerController
import com.motomusic.app.playback.EqualizerPreset
import com.motomusic.app.playback.EqualizerUiState
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The two audio sheets the player and Settings can open. */
enum class AudioSheet { EQUALIZER, OUTPUTS }

/**
 * The equalizer and the output picker. Both controllers live as long as the process and are
 * shared with the [com.motomusic.app.playback.MusicService]; this only forwards the user's
 * changes and tracks which sheet is open. Like the player, it is created once at the app root.
 */
@HiltViewModel
class AudioViewModel @Inject constructor(
    private val equalizerController: EqualizerController,
    private val audioOutputController: AudioOutputController,
) : ViewModel() {

    val equalizer: StateFlow<EqualizerUiState> = equalizerController.state
    val outputs: StateFlow<AudioOutputState> = audioOutputController.state

    val canShowSystemSwitcher: Boolean get() = audioOutputController.canShowSystemSwitcher

    private val _sheet = MutableStateFlow<AudioSheet?>(null)
    val sheet: StateFlow<AudioSheet?> = _sheet.asStateFlow()

    fun openEqualizer() {
        _sheet.value = AudioSheet.EQUALIZER
    }

    fun openOutputs() {
        audioOutputController.refresh()
        _sheet.value = AudioSheet.OUTPUTS
    }

    fun dismissSheet() {
        _sheet.value = null
    }

    // --- Equalizer ------------------------------------------------------------------------

    fun setEqualizerEnabled(enabled: Boolean) = equalizerController.setEnabled(enabled)
    fun selectPreset(preset: EqualizerPreset) = equalizerController.selectPreset(preset)
    fun setBandLevel(band: Int, levelMb: Int) = equalizerController.setBandLevel(band, levelMb)
    fun commitBandLevels() = equalizerController.commitCustomLevels()
    fun resetEqualizer() = equalizerController.resetToFlat()

    // --- Outputs --------------------------------------------------------------------------

    fun selectOutput(outputId: Int?) = audioOutputController.select(outputId)

    /** Hands over to Android's own switcher; false when this Android version has none. */
    fun showSystemSwitcher(): Boolean {
        val shown = audioOutputController.showSystemSwitcher()
        if (shown) _sheet.value = null
        return shown
    }
}
