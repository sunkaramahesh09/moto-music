package com.motomusic.app.presentation.app

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.core.MediaPermission
import com.motomusic.app.domain.model.ScanState
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.model.UserPreferences
import com.motomusic.app.domain.repository.SettingsRepository
import com.motomusic.app.domain.repository.SongRepository
import com.motomusic.app.playback.PlaybackEventBus
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * App-shell state: theme, the media permission gate and library scanning.
 * Screen content is owned by the per-screen ViewModels.
 */
@HiltViewModel
class MainViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val songRepository: SongRepository,
    private val settingsRepository: SettingsRepository,
    eventBus: PlaybackEventBus,
) : ViewModel() {

    val preferences: StateFlow<UserPreferences> = settingsRepository.preferences
        .stateIn(viewModelScope, SharingStarted.Eagerly, UserPreferences())

    private val _hasAudioPermission = MutableStateFlow(MediaPermission.hasAudioAccess(context))
    val hasAudioPermission: StateFlow<Boolean> = _hasAudioPermission.asStateFlow()

    val scanState: StateFlow<ScanState> = songRepository.scanState

    val songCount: StateFlow<Int> = songRepository.observeSongCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    /** User-facing playback problems, surfaced as a snackbar wherever the user is. */
    val messages: SharedFlow<String> = eventBus.messages

    init {
        // The permission can be revoked from system settings while the app is in the background.
        if (_hasAudioPermission.value) refreshLibrary()
    }

    /** Re-checks the permission whenever the app returns to the foreground. */
    fun onResume() {
        val granted = MediaPermission.hasAudioAccess(context)
        val wasDenied = !_hasAudioPermission.value
        _hasAudioPermission.value = granted
        if (granted && wasDenied) refreshLibrary()
    }

    fun onAudioPermissionResult(granted: Boolean) {
        _hasAudioPermission.value = granted
        if (granted) {
            refreshLibrary()
            viewModelScope.launch { settingsRepository.setWelcomeCompleted(true) }
        }
    }

    fun refreshLibrary() {
        viewModelScope.launch { songRepository.rescan() }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setThemeMode(mode) }
    }
}
