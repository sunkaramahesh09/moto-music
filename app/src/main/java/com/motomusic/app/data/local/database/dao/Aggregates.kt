package com.motomusic.app.data.local.database.dao

import androidx.room.ColumnInfo

/** Album/artist/folder views are derived from the songs table rather than duplicated. */
data class AlbumAggregate(
    @ColumnInfo(name = "albumId") val albumId: Long,
    @ColumnInfo(name = "album") val album: String,
    @ColumnInfo(name = "artist") val artist: String,
    @ColumnInfo(name = "artistId") val artistId: Long,
    @ColumnInfo(name = "songCount") val songCount: Int,
    @ColumnInfo(name = "year") val year: Int,
    @ColumnInfo(name = "artworkSongUri") val artworkSongUri: String?,
)

data class ArtistAggregate(
    @ColumnInfo(name = "artistId") val artistId: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "songCount") val songCount: Int,
    @ColumnInfo(name = "albumCount") val albumCount: Int,
    @ColumnInfo(name = "artworkAlbumId") val artworkAlbumId: Long,
    @ColumnInfo(name = "artworkSongUri") val artworkSongUri: String?,
)

data class FolderAggregate(
    @ColumnInfo(name = "path") val path: String,
    @ColumnInfo(name = "songCount") val songCount: Int,
)

data class PlaylistAggregate(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "createdAt") val createdAt: Long,
    @ColumnInfo(name = "updatedAt") val updatedAt: Long,
    @ColumnInfo(name = "songCount") val songCount: Int,
    @ColumnInfo(name = "artworkAlbumId") val artworkAlbumId: Long?,
    @ColumnInfo(name = "artworkSongUri") val artworkSongUri: String?,
)

/** Minimal row used by the rescan diff so a full library never has to be materialised. */
data class SongSyncRow(
    @ColumnInfo(name = "id") val id: Long,
    @ColumnInfo(name = "date_modified_sec") val dateModifiedSec: Long,
)
