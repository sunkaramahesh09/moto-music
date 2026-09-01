package com.motomusic.app.domain.model

data class Artist(
    val id: Long,
    val name: String,
    val songCount: Int,
    val albumCount: Int,
    val artworkAlbumId: Long,
    val artworkSongUri: String?,
)
