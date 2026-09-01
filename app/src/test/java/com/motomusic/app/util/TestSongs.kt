package com.motomusic.app.util

import com.motomusic.app.domain.model.Song

/** A song with sensible defaults, so a test only states the fields it cares about. */
fun testSong(
    id: Long,
    title: String = "Song $id",
    artist: String = "Artist",
    artistId: Long = 1L,
    album: String = "Album",
    albumId: Long = 1L,
    durationMs: Long = 3 * 60_000L,
    folderPath: String = "Music",
): Song = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    trackNumber = 1,
    year = 2020,
    dateAddedSec = 0L,
    dateModifiedSec = 0L,
    sizeBytes = 5_000_000L,
    mimeType = "audio/mpeg",
    folderPath = folderPath,
    fileName = "song-$id.mp3",
    filePath = "/storage/emulated/0/Music/song-$id.mp3",
    bitrateBps = 320_000,
    uri = "content://media/external/audio/media/$id",
)
