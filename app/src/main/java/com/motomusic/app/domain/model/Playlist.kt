package com.motomusic.app.domain.model

data class Playlist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val createdAt: Long,
    val updatedAt: Long,
    /** Artwork source for the playlist tile: the first song that has one. */
    val artworkAlbumId: Long?,
    val artworkSongUri: String?,
)
