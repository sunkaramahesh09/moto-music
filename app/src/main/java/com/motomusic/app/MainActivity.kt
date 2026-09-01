package com.motomusic.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import com.motomusic.app.data.mediastore.ArtworkLoader
import com.motomusic.app.presentation.app.MainViewModel
import com.motomusic.app.presentation.app.MotoApp
import com.motomusic.app.ui.components.LocalArtworkLoader
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * The only activity. Everything else is Compose.
 *
 * The artwork loader is published here rather than injected per screen so that every list row
 * shares one bitmap cache for the life of the process.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var artworkLoader: ArtworkLoader

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            CompositionLocalProvider(LocalArtworkLoader provides artworkLoader) {
                MotoApp(mainViewModel = viewModel)
            }
        }
    }

    /** The audio permission can be revoked from system settings while the app is backgrounded. */
    override fun onResume() {
        super.onResume()
        viewModel.onResume()
    }
}
