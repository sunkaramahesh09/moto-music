package com.motomusic.app.domain.model

enum class ThemeMode(val storageKey: String, val label: String) {
    SYSTEM("system", "Follow system"),
    LIGHT("light", "Light"),
    DARK("dark", "Dark");

    companion object {
        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.storageKey == key } ?: SYSTEM
    }
}

/** Everything the Settings screen can change. Persisted locally with DataStore. */
data class UserPreferences(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val useDynamicColor: Boolean = true,
    val songSortOrder: SortOrder = SortOrder.TITLE_ASC,
    val resumeLastSession: Boolean = true,
    val skipSilence: Boolean = false,
    val pauseOnHeadphonesDisconnect: Boolean = true,
    /** Ease the volume in and out instead of cutting playback on and off. */
    val fadeOnPlayPause: Boolean = true,
    /** Keep WhatsApp voice notes and voice-recorder files out of the library. */
    val hideVoiceRecordings: Boolean = true,
    /** Individual files the user hid by hand, by [Song.hideKey]. */
    val hiddenSongKeys: Set<String> = emptySet(),
    val hasCompletedWelcome: Boolean = false,
    /**
     * Send audio to the output in 32-bit float instead of squeezing it to 16-bit, so lossless
     * and hi-res files keep their full resolution. It bypasses Media3's own audio processors,
     * which is why it cannot be combined with [skipSilence].
     */
    val highResOutput: Boolean = true,
    val equalizer: EqualizerSettings = EqualizerSettings(),
) {
    /** What the player actually does: skip silence wins for anyone who had it on already. */
    val useHighResOutput: Boolean get() = highResOutput && !skipSilence
}

/**
 * The user's equalizer. [customLevelsMb] are per device band, in millibels, and only mean
 * something on the device that produced them — a mismatch in band count falls back to flat.
 */
data class EqualizerSettings(
    val enabled: Boolean = false,
    val presetKey: String = "flat",
    val customLevelsMb: List<Int> = emptyList(),
)
