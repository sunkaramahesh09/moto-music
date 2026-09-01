package com.motomusic.app.domain.model

/** Progress of a MediaStore rescan, surfaced to the UI so it never blocks the main thread. */
sealed interface ScanState {
    data object Idle : ScanState
    data class Scanning(val processed: Int, val total: Int) : ScanState
    data class Finished(val added: Int, val updated: Int, val removed: Int) : ScanState
    data class Failed(val message: String) : ScanState
}
