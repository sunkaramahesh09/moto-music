package com.motomusic.app.data.local.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.motomusic.app.domain.model.Song

/**
 * Cached MediaStore metadata. Only metadata is stored — audio files are never copied,
 * the app always plays back through the original MediaStore content URI.
 */
@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey val id: Long,
    val title: String,
    val artist: String,
    @ColumnInfo(name = "artist_id") val artistId: Long,
    val album: String,
    @ColumnInfo(name = "album_id") val albumId: Long,
    @ColumnInfo(name = "duration_ms") val durationMs: Long,
    @ColumnInfo(name = "track_number") val trackNumber: Int,
    val year: Int,
    @ColumnInfo(name = "date_added_sec") val dateAddedSec: Long,
    @ColumnInfo(name = "date_modified_sec") val dateModifiedSec: Long,
    @ColumnInfo(name = "size_bytes") val sizeBytes: Long,
    @ColumnInfo(name = "mime_type") val mimeType: String,
    @ColumnInfo(name = "folder_path") val folderPath: String,
    @ColumnInfo(name = "file_name") val fileName: String,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "bitrate_bps") val bitrateBps: Int,
    val uri: String,
)

fun SongEntity.toDomain() = Song(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    trackNumber = trackNumber,
    year = year,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderPath = folderPath,
    fileName = fileName,
    filePath = filePath,
    bitrateBps = bitrateBps,
    uri = uri,
)

fun Song.toEntity() = SongEntity(
    id = id,
    title = title,
    artist = artist,
    artistId = artistId,
    album = album,
    albumId = albumId,
    durationMs = durationMs,
    trackNumber = trackNumber,
    year = year,
    dateAddedSec = dateAddedSec,
    dateModifiedSec = dateModifiedSec,
    sizeBytes = sizeBytes,
    mimeType = mimeType,
    folderPath = folderPath,
    fileName = fileName,
    filePath = filePath,
    bitrateBps = bitrateBps,
    uri = uri,
)

fun List<SongEntity>.toDomain(): List<Song> = map { it.toDomain() }
