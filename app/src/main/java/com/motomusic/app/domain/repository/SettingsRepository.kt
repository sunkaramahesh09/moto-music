package com.motomusic.app.domain.repository

import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.model.UserPreferences
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val preferences: Flow<UserPreferences>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setDynamicColor(enabled: Boolean)
    suspend fun setSongSortOrder(order: SortOrder)
    suspend fun setResumeLastSession(enabled: Boolean)
    suspend fun setSkipSilence(enabled: Boolean)
    suspend fun setPauseOnHeadphonesDisconnect(enabled: Boolean)
    suspend fun setFadeOnPlayPause(enabled: Boolean)
    suspend fun setHideVoiceRecordings(enabled: Boolean)

    /** Hides or restores one file, keyed by `Song.hideKey`. */
    suspend fun setSongHidden(hideKey: String, hidden: Boolean)
    suspend fun unhideAllSongs()
    suspend fun setWelcomeCompleted(completed: Boolean)

    /** Queue restored on next launch when "resume last session" is on. */
    suspend fun saveLastSession(songIds: List<Long>, index: Int, positionMs: Long)
    suspend fun loadLastSession(): LastSession?
}

data class LastSession(val songIds: List<Long>, val index: Int, val positionMs: Long)
