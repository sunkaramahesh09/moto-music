package com.motomusic.app.domain.model

data class Album(
    val id: Long,
    val name: String,
    val artist: String,
    val artistId: Long,
    val songCount: Int,
    val year: Int,
    /** Any song of the album, used as the artwork source. */
    val artworkSongUri: String?,
)
