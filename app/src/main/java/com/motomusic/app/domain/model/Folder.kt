package com.motomusic.app.domain.model

/** A directory that contains at least one audio file, derived from MediaStore paths. */
data class Folder(
    val path: String,
    val name: String,
    val songCount: Int,
)
