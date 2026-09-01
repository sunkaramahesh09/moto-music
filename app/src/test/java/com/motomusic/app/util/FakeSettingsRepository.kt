package com.motomusic.app.util

import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.model.UserPreferences
import com.motomusic.app.domain.repository.LastSession
import com.motomusic.app.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/** In-memory settings, so a test can read back what a ViewModel wrote. */
class FakeSettingsRepository(
    initial: UserPreferences = UserPreferences(),
) : SettingsRepository {

    val state = MutableStateFlow(initial)
    override val preferences: Flow<UserPreferences> = state

    var lastSession: LastSession? = null
        private set

    override suspend fun setThemeMode(mode: ThemeMode) = update { copy(themeMode = mode) }

    override suspend fun setDynamicColor(enabled: Boolean) = update { copy(useDynamicColor = enabled) }

    override suspend fun setSongSortOrder(order: SortOrder) = update { copy(songSortOrder = order) }

    override suspend fun setResumeLastSession(enabled: Boolean) = update { copy(resumeLastSession = enabled) }

    override suspend fun setSkipSilence(enabled: Boolean) = update { copy(skipSilence = enabled) }

    override suspend fun setPauseOnHeadphonesDisconnect(enabled: Boolean) =
        update { copy(pauseOnHeadphonesDisconnect = enabled) }

    override suspend fun setFadeOnPlayPause(enabled: Boolean) = update { copy(fadeOnPlayPause = enabled) }

    override suspend fun setHideVoiceRecordings(enabled: Boolean) =
        update { copy(hideVoiceRecordings = enabled) }

    override suspend fun setSongHidden(hideKey: String, hidden: Boolean) = update {
        copy(hiddenSongKeys = if (hidden) hiddenSongKeys + hideKey else hiddenSongKeys - hideKey)
    }

    override suspend fun unhideAllSongs() = update { copy(hiddenSongKeys = emptySet()) }

    override suspend fun setWelcomeCompleted(completed: Boolean) =
        update { copy(hasCompletedWelcome = completed) }

    override suspend fun saveLastSession(songIds: List<Long>, index: Int, positionMs: Long) {
        lastSession = LastSession(songIds, index, positionMs)
    }

    override suspend fun loadLastSession(): LastSession? = lastSession

    private inline fun update(block: UserPreferences.() -> UserPreferences) {
        state.value = state.value.block()
    }
}
