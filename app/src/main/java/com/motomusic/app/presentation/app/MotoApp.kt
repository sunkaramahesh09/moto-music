package com.motomusic.app.presentation.app

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.motomusic.app.navigation.MotoNavHost
import com.motomusic.app.navigation.Routes
import com.motomusic.app.navigation.TopLevelDestination
import com.motomusic.app.navigation.navigateTo
import com.motomusic.app.navigation.navigateToTopLevel
import com.motomusic.app.presentation.common.LocalFavoriteIds
import com.motomusic.app.presentation.common.LocalNowPlayingId
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.presentation.common.SongInfoDialog
import com.motomusic.app.presentation.permission.PermissionScreen
import com.motomusic.app.presentation.player.PlayerViewModel
import com.motomusic.app.presentation.player.SleepTimerSheet
import com.motomusic.app.presentation.player.SongMenuSheet
import com.motomusic.app.presentation.playlists.AddToPlaylistSheet
import com.motomusic.app.ui.components.MiniPlayer
import com.motomusic.app.ui.theme.MotoMusicTheme

/**
 * The app shell: theme, the permission gate, the bottom bar, the mini player and every sheet the
 * song menu can open. Screens themselves live in [MotoNavHost].
 */
@Composable
fun MotoApp(
    mainViewModel: MainViewModel = hiltViewModel(),
    playerViewModel: PlayerViewModel = hiltViewModel(),
) {
    val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()

    MotoMusicTheme(
        themeMode = preferences.themeMode,
        useDynamicColor = preferences.useDynamicColor,
    ) {
        val hasPermission by mainViewModel.hasAudioPermission.collectAsStateWithLifecycle()

        if (!hasPermission) {
            PermissionScreen(onPermissionResult = mainViewModel::onAudioPermissionResult)
        } else {
            MotoAppContent(mainViewModel = mainViewModel, playerViewModel = playerViewModel)
        }
    }
}

@Composable
private fun MotoAppContent(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route

    val playback by playerViewModel.playback.collectAsStateWithLifecycle()
    // Collected without reading: the position ticks several times a second, and unwrapping it
    // here would recompose the whole shell — bottom bar and nav host included — on every tick.
    val position = playerViewModel.position.collectAsStateWithLifecycle()
    val favoriteIds by playerViewModel.favoriteIds.collectAsStateWithLifecycle()
    val menuTarget by playerViewModel.menuTarget.collectAsStateWithLifecycle()
    val addToPlaylistSong by playerViewModel.addToPlaylistSong.collectAsStateWithLifecycle()
    val infoSong by playerViewModel.infoSong.collectAsStateWithLifecycle()
    val playlists by playerViewModel.playlists.collectAsStateWithLifecycle()
    val sleepTimer by playerViewModel.sleepTimer.collectAsStateWithLifecycle()
    val sleepTimerOpen by playerViewModel.sleepTimerSheetOpen.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        mainViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    // The player fills the screen on its own; everywhere else keeps the strip and the bar.
    val isPlayerRoute = currentRoute == Routes.PLAYER
    val topLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }

    CompositionLocalProvider(
        LocalSongActions provides playerViewModel,
        LocalFavoriteIds provides favoriteIds,
        LocalNowPlayingId provides playback.current?.songId,
    ) {
        Scaffold(
            snackbarHost = { SnackbarHost(snackbarHostState) },
            bottomBar = {
                Column {
                    if (!isPlayerRoute) {
                        MiniPlayer(
                            track = playback.current,
                            isPlaying = playback.isPlaying,
                            progress = { position.value.progress },
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onExpand = { navController.navigateTo(Routes.PLAYER) },
                            onDismiss = playerViewModel::clearQueue,
                        )
                    }
                    if (topLevel != null) {
                        NavigationBar {
                            TopLevelDestination.entries.forEach { destination ->
                                NavigationBarItem(
                                    selected = destination == topLevel,
                                    onClick = { navController.navigateToTopLevel(destination.route) },
                                    icon = {
                                        Icon(destination.icon, contentDescription = destination.label)
                                    },
                                    label = { Text(destination.label) },
                                )
                            }
                        }
                    }
                }
            },
        ) { innerPadding ->
            MotoNavHost(
                navController = navController,
                contentPadding = innerPadding,
                playerViewModel = playerViewModel,
                onRescan = mainViewModel::refreshLibrary,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }

    menuTarget?.let { target ->
        SongMenuSheet(
            target = target,
            isFavorite = target.song.id in favoriteIds,
            onDismiss = playerViewModel::dismissMenu,
            onPlayNext = playerViewModel::playNext,
            onAddToQueue = { song -> playerViewModel.addToQueue(listOf(song)) },
            onToggleFavorite = playerViewModel::toggleFavorite,
            onAddToPlaylist = playerViewModel::requestAddToPlaylist,
            onRemoveFromPlaylist = { song, playlistId ->
                playerViewModel.removeFromPlaylist(playlistId, song.id)
            },
            onGoToAlbum = { song -> navController.navigateTo(Routes.albumDetails(song.albumId)) },
            onGoToArtist = { song -> navController.navigateTo(Routes.artistDetails(song.artistId)) },
            onShowInfo = playerViewModel::requestSongInfo,
            onHideFromLibrary = playerViewModel::hideFromLibrary,
        )
    }

    addToPlaylistSong?.let { song ->
        AddToPlaylistSheet(
            song = song,
            playlists = playlists,
            onDismiss = playerViewModel::dismissAddToPlaylist,
            onAddTo = { playlistId -> playerViewModel.addToPlaylist(playlistId, listOf(song.id)) },
            onCreateWith = { name -> playerViewModel.createPlaylistWith(name, listOf(song.id)) },
        )
    }

    infoSong?.let { song ->
        SongInfoDialog(song = song, onDismiss = playerViewModel::dismissSongInfo)
    }

    if (sleepTimerOpen) {
        SleepTimerSheet(
            state = sleepTimer,
            onDismiss = playerViewModel::dismissSleepTimer,
            onStart = playerViewModel::startSleepTimer,
            onEndOfTrack = playerViewModel::sleepAfterCurrentTrack,
            onCancel = playerViewModel::cancelSleepTimer,
        )
    }
}
