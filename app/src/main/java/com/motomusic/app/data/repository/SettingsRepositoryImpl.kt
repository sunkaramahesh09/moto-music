package com.motomusic.app.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.motomusic.app.data.local.prefs.settingsDataStore
import com.motomusic.app.domain.model.SortOrder
import com.motomusic.app.domain.model.ThemeMode
import com.motomusic.app.domain.model.UserPreferences
import com.motomusic.app.domain.repository.LastSession
import com.motomusic.app.domain.repository.SettingsRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import androidx.datastore.preferences.core.emptyPreferences

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    @param:ApplicationContext private val context: Context,
) : SettingsRepository {

    private val dataStore = context.settingsDataStore

    override val preferences: Flow<UserPreferences> = dataStore.data
        .catch { throwable ->
            // A corrupt preferences file must not take the app down; fall back to defaults.
            if (throwable is IOException) {
                Log.w(TAG, "Could not read settings, using defaults", throwable)
                emit(emptyPreferences())
            } else {
                throw throwable
            }
        }
        .map { prefs ->
            UserPreferences(
                themeMode = ThemeMode.fromStorageKey(prefs[KEY_THEME]),
                useDynamicColor = prefs[KEY_DYNAMIC_COLOR] ?: true,
                songSortOrder = SortOrder.fromStorageKey(prefs[KEY_SORT_ORDER]),
                resumeLastSession = prefs[KEY_RESUME_SESSION] ?: true,
                skipSilence = prefs[KEY_SKIP_SILENCE] ?: false,
                pauseOnHeadphonesDisconnect = prefs[KEY_PAUSE_ON_DISCONNECT] ?: true,
                fadeOnPlayPause = prefs[KEY_FADE_PLAY_PAUSE] ?: true,
                hideVoiceRecordings = prefs[KEY_HIDE_VOICE_RECORDINGS] ?: true,
                hiddenSongKeys = prefs[KEY_HIDDEN_SONGS].orEmpty(),
                hasCompletedWelcome = prefs[KEY_WELCOME_DONE] ?: false,
            )
        }

    override suspend fun setThemeMode(mode: ThemeMode) = edit { it[KEY_THEME] = mode.storageKey }

    override suspend fun setDynamicColor(enabled: Boolean) = edit { it[KEY_DYNAMIC_COLOR] = enabled }

    override suspend fun setSongSortOrder(order: SortOrder) = edit { it[KEY_SORT_ORDER] = order.storageKey }

    override suspend fun setResumeLastSession(enabled: Boolean) = edit { it[KEY_RESUME_SESSION] = enabled }

    override suspend fun setSkipSilence(enabled: Boolean) = edit { it[KEY_SKIP_SILENCE] = enabled }

    override suspend fun setPauseOnHeadphonesDisconnect(enabled: Boolean) =
        edit { it[KEY_PAUSE_ON_DISCONNECT] = enabled }

    override suspend fun setFadeOnPlayPause(enabled: Boolean) = edit { it[KEY_FADE_PLAY_PAUSE] = enabled }

    override suspend fun setHideVoiceRecordings(enabled: Boolean) =
        edit { it[KEY_HIDE_VOICE_RECORDINGS] = enabled }

    override suspend fun setSongHidden(hideKey: String, hidden: Boolean) {
        if (hideKey.isEmpty()) return
        edit { prefs ->
            val current = prefs[KEY_HIDDEN_SONGS].orEmpty()
            prefs[KEY_HIDDEN_SONGS] = if (hidden) current + hideKey else current - hideKey
        }
    }

    override suspend fun unhideAllSongs() = edit { it[KEY_HIDDEN_SONGS] = emptySet() }

    override suspend fun setWelcomeCompleted(completed: Boolean) = edit { it[KEY_WELCOME_DONE] = completed }

    override suspend fun saveLastSession(songIds: List<Long>, index: Int, positionMs: Long) {
        // Only identifiers and a position are stored — never audio data.
        val trimmed = songIds.take(MAX_PERSISTED_QUEUE)
        edit {
            it[KEY_QUEUE_IDS] = trimmed.joinToString(",")
            it[KEY_QUEUE_INDEX] = index.coerceIn(0, (trimmed.size - 1).coerceAtLeast(0))
            it[KEY_QUEUE_POSITION] = positionMs.coerceAtLeast(0L)
        }
    }

    override suspend fun loadLastSession(): LastSession? {
        val prefs = preferences.first()
        if (!prefs.resumeLastSession) return null
        val raw = dataStore.data.catch { emit(emptyPreferences()) }.first()
        val ids = raw[KEY_QUEUE_IDS]
            ?.split(',')
            ?.mapNotNull { it.trim().toLongOrNull() }
            .orEmpty()
        if (ids.isEmpty()) return null
        return LastSession(
            songIds = ids,
            index = (raw[KEY_QUEUE_INDEX] ?: 0).coerceIn(0, ids.size - 1),
            positionMs = raw[KEY_QUEUE_POSITION] ?: 0L,
        )
    }

    private suspend fun edit(block: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        runCatching { dataStore.edit(block) }
            .onFailure { Log.w(TAG, "Could not persist setting", it) }
    }

    private companion object {
        const val TAG = "SettingsRepository"
        const val MAX_PERSISTED_QUEUE = 500

        val KEY_THEME = stringPreferencesKey("theme_mode")
        val KEY_DYNAMIC_COLOR = booleanPreferencesKey("dynamic_color")
        val KEY_SORT_ORDER = stringPreferencesKey("song_sort_order")
        val KEY_RESUME_SESSION = booleanPreferencesKey("resume_last_session")
        val KEY_SKIP_SILENCE = booleanPreferencesKey("skip_silence")
        val KEY_PAUSE_ON_DISCONNECT = booleanPreferencesKey("pause_on_disconnect")
        val KEY_FADE_PLAY_PAUSE = booleanPreferencesKey("fade_on_play_pause")
        val KEY_HIDE_VOICE_RECORDINGS = booleanPreferencesKey("hide_voice_recordings")
        val KEY_HIDDEN_SONGS = stringSetPreferencesKey("hidden_song_keys")
        val KEY_WELCOME_DONE = booleanPreferencesKey("welcome_completed")
        val KEY_QUEUE_IDS = stringPreferencesKey("last_queue_ids")
        val KEY_QUEUE_INDEX = intPreferencesKey("last_queue_index")
        val KEY_QUEUE_POSITION = longPreferencesKey("last_queue_position")
    }
}
