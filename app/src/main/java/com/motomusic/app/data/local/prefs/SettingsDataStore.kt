package com.motomusic.app.data.local.prefs

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/** Single local preferences file. Nothing here ever leaves the device. */
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "moto_music_settings")
