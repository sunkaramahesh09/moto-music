package com.motomusic.app.playback

import android.content.Context
import android.media.AudioDeviceCallback
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.MediaRouter2
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Immutable
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The broad kind of an output, which is all the UI needs to pick an icon and a name. */
enum class AudioOutputKind { SPEAKER, WIRED, BLUETOOTH, USB, HDMI, HEARING_AID, OTHER }

@Immutable
data class AudioOutput(val id: Int, val name: String, val kind: AudioOutputKind)

@Immutable
data class AudioOutputState(
    val outputs: List<AudioOutput> = emptyList(),
    /** The output the user picked, or null to let Android route as it normally would. */
    val selectedId: Int? = null,
    /** Where audio is going right now, when the platform can tell (Android 13+). */
    val activeId: Int? = null,
) {
    val selected: AudioOutput? get() = outputs.firstOrNull { it.id == selectedId }
    val active: AudioOutput? get() = selected ?: outputs.firstOrNull { it.id == activeId }
}

/**
 * Lists the connected outputs music can be played through and remembers which one the user
 * chose. [MusicService] watches [preferredDevice] and routes its player there with
 * `ExoPlayer.setPreferredAudioDevice`, which leaves every other app's audio alone.
 *
 * Only devices that are already connected can be chosen: pairing or connecting a Bluetooth
 * device is the system's job, which [canShowSystemSwitcher] / [showSystemSwitcher] hand off to.
 */
@Singleton
class AudioOutputController @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {

    private val audioManager = context.getSystemService(AudioManager::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _state = MutableStateFlow(AudioOutputState())
    val state: StateFlow<AudioOutputState> = _state.asStateFlow()

    private val _preferredDevice = MutableStateFlow<AudioDeviceInfo?>(null)
    val preferredDevice: StateFlow<AudioDeviceInfo?> = _preferredDevice.asStateFlow()

    private var devices: List<AudioDeviceInfo> = emptyList()

    private val callback = object : AudioDeviceCallback() {
        override fun onAudioDevicesAdded(addedDevices: Array<out AudioDeviceInfo>) = refresh()
        override fun onAudioDevicesRemoved(removedDevices: Array<out AudioDeviceInfo>) = refresh()
    }

    init {
        audioManager.registerAudioDeviceCallback(callback, mainHandler)
        refresh()
    }

    /** Picks an output; null goes back to automatic routing. */
    fun select(outputId: Int?) {
        val device = devices.firstOrNull { it.id == outputId }
        _preferredDevice.value = device
        _state.value = _state.value.copy(selectedId = device?.id)
        refreshActive()
    }

    /** Re-reads the active route; cheap, and called whenever the picker is shown. */
    fun refresh() {
        devices = audioManager.getDevices(AudioManager.GET_DEVICES_OUTPUTS)
            .filter { kindOf(it.type) != null }
            .distinctBy { kindOf(it.type) to displayName(it) }

        // A chosen device that went away (headphones unplugged, Bluetooth off) hands routing
        // back to the system rather than leaving the player pointed at nothing.
        val selected = _state.value.selectedId?.takeIf { id -> devices.any { it.id == id } }
        if (selected == null && _preferredDevice.value != null) _preferredDevice.value = null

        _state.value = AudioOutputState(
            outputs = devices.map { AudioOutput(it.id, displayName(it), kindOf(it.type)!!) }
                .sortedBy { it.kind.ordinal },
            selectedId = selected,
            activeId = activeDeviceId(),
        )
    }

    private fun refreshActive() {
        _state.value = _state.value.copy(activeId = activeDeviceId())
    }

    val canShowSystemSwitcher: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE

    /** Opens Android's own output switcher, which can also connect devices that are paired but idle. */
    fun showSystemSwitcher(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) return false
        return runCatching { MediaRouter2.getInstance(context).showSystemOutputSwitcher() }.getOrDefault(false)
    }

    private fun activeDeviceId(): Int? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return null
        val attributes = android.media.AudioAttributes.Builder()
            .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MUSIC)
            .build()
        val routed = runCatching { audioManager.getAudioDevicesForAttributes(attributes) }
            .getOrNull()
            ?.firstOrNull()
            ?: return null
        // The routed entry is a lightweight description; match it back to a listed device.
        return devices.firstOrNull { it.type == routed.type && it.address == routed.address }?.id
            ?: devices.firstOrNull { it.type == routed.type }?.id
    }

    private fun displayName(device: AudioDeviceInfo): String {
        val product = device.productName?.toString()?.trim().orEmpty()
        return when (kindOf(device.type)) {
            AudioOutputKind.SPEAKER -> "This phone"
            AudioOutputKind.WIRED -> "Wired headphones"
            AudioOutputKind.HDMI -> product.ifEmpty { "HDMI" }
            AudioOutputKind.USB -> product.ifEmpty { "USB audio" }
            AudioOutputKind.BLUETOOTH -> product.ifEmpty { "Bluetooth device" }
            AudioOutputKind.HEARING_AID -> product.ifEmpty { "Hearing aid" }
            else -> product.ifEmpty { "Audio output" }
        }
    }

    private companion object {
        /** Outputs that can carry music; the earpiece, call paths and internal buses are left out. */
        fun kindOf(type: Int): AudioOutputKind? = when (type) {
            AudioDeviceInfo.TYPE_BUILTIN_SPEAKER -> AudioOutputKind.SPEAKER
            AudioDeviceInfo.TYPE_WIRED_HEADPHONES,
            AudioDeviceInfo.TYPE_WIRED_HEADSET,
            AudioDeviceInfo.TYPE_LINE_ANALOG,
            AudioDeviceInfo.TYPE_AUX_LINE -> AudioOutputKind.WIRED
            AudioDeviceInfo.TYPE_BLUETOOTH_A2DP,
            AudioDeviceInfo.TYPE_BLE_HEADSET,
            AudioDeviceInfo.TYPE_BLE_SPEAKER,
            AudioDeviceInfo.TYPE_BLE_BROADCAST -> AudioOutputKind.BLUETOOTH
            AudioDeviceInfo.TYPE_USB_DEVICE,
            AudioDeviceInfo.TYPE_USB_HEADSET,
            AudioDeviceInfo.TYPE_USB_ACCESSORY,
            AudioDeviceInfo.TYPE_DOCK,
            AudioDeviceInfo.TYPE_DOCK_ANALOG -> AudioOutputKind.USB
            AudioDeviceInfo.TYPE_HDMI,
            AudioDeviceInfo.TYPE_HDMI_ARC,
            AudioDeviceInfo.TYPE_HDMI_EARC -> AudioOutputKind.HDMI
            AudioDeviceInfo.TYPE_HEARING_AID -> AudioOutputKind.HEARING_AID
            else -> null
        }
    }
}
