package com.motomusic.app.presentation.common

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.motomusic.app.data.mediastore.AudioDetails
import com.motomusic.app.data.mediastore.AudioDetailsProbe
import com.motomusic.app.di.IoDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Reads the technical details of one song for the info dialog. Kept out of [com.motomusic.app
 * .presentation.player.PlayerViewModel] because the work only ever happens while the dialog is up.
 */
@HiltViewModel
class SongInfoViewModel @Inject constructor(
    private val probe: AudioDetailsProbe,
    @param:IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _details = MutableStateFlow<AudioDetails?>(null)
    val details: StateFlow<AudioDetails?> = _details.asStateFlow()

    fun load(uri: String) {
        _details.value = null
        viewModelScope.launch {
            _details.value = withContext(ioDispatcher) { probe.probe(uri) }
        }
    }
}
