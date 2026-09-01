package com.motomusic.app.data.local.database

import androidx.room.migration.Migration

/**
 * Schema history for [MotoMusicDatabase].
 *
 * The schema is exported to `app/schemas/`, so every future change must bump
 * [MotoMusicDatabase.VERSION] and append a [Migration] here. Destructive fallback is
 * deliberately not enabled: playlists and favourites are user-created data that cannot
 * be regenerated from MediaStore, unlike the cached song rows.
 */
val MOTO_MUSIC_MIGRATIONS: Array<Migration> = emptyArray()
