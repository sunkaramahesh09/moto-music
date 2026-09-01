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
)
