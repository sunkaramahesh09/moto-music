package com.motomusic.app.presentation.settings

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.model.UserPreferences
import com.motomusic.app.domain.repository.PlaybackHistoryRepository
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@Immutable
data class SettingsUiState(
    val preferences: UserPreferences = UserPreferences(),
    val songCount: Int = 0,
    val scanState: ScanState = ScanState.Idle,
) {
    val isScanning: Boolean get() = scanState is ScanState.Scanning
}

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val songRepository: SongRepository,
    private val historyRepository: PlaybackHistoryRepository,
) : ViewModel() {

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.preferences,
        songRepository.observeSongCount(),
        songRepository.scanState,
    ) { preferences, songCount, scanState ->
        SettingsUiState(preferences = preferences, songCount = songCount, scanState = scanState)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MS), SettingsUiState())

    fun setThemeMode(mode: ThemeMode) = viewModelScope.launch { settingsRepository.setThemeMode(mode) }

    fun setDynamicColor(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }

    fun setResumeLastSession(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setResumeLastSession(enabled) }

    fun setSkipSilence(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setSkipSilence(enabled) }

    fun setPauseOnHeadphonesDisconnect(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setPauseOnHeadphonesDisconnect(enabled) }

    fun setFadeOnPlayPause(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setFadeOnPlayPause(enabled) }

    /** The library rescans itself when this flips; the repository watches the same setting. */
    fun setHideVoiceRecordings(enabled: Boolean) =
        viewModelScope.launch { settingsRepository.setHideVoiceRecordings(enabled) }

    fun unhideSong(hideKey: String) =
        viewModelScope.launch { settingsRepository.setSongHidden(hideKey, hidden = false) }

    fun unhideAllSongs() = viewModelScope.launch { settingsRepository.unhideAllSongs() }

    fun rescan() = viewModelScope.launch { songRepository.rescan() }

    fun clearHistory() = viewModelScope.launch { historyRepository.clearHistory() }

    private companion object {
        const val STOP_TIMEOUT_MS = 5_000L
    }
}
