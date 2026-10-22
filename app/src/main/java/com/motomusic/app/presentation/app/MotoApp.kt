package com.motomusic.app.presentation.app

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import kotlinx.coroutines.flow.filterNotNull
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
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
import com.motomusic.app.presentation.audio.AudioOutputSheet
import com.motomusic.app.presentation.audio.AudioSheet
import com.motomusic.app.presentation.audio.AudioViewModel
import com.motomusic.app.presentation.audio.EqualizerSheet
import com.motomusic.app.presentation.common.LocalFavoriteIds
import com.motomusic.app.presentation.common.LocalNowPlayingId
import com.motomusic.app.presentation.common.LocalSongActions
import com.motomusic.app.presentation.common.SongInfoDialog
import com.motomusic.app.presentation.permission.PermissionScreen
import com.motomusic.app.presentation.player.PlayerOverlay
import com.motomusic.app.presentation.player.PlayerScreen
import com.motomusic.app.presentation.player.PlayerViewModel
import com.motomusic.app.presentation.queue.QueueScreen
import com.motomusic.app.presentation.player.SleepTimerSheet
import com.motomusic.app.presentation.player.SongMenuSheet
import com.motomusic.app.presentation.playlists.AddToPlaylistSheet
import com.motomusic.app.ui.components.GlassNavigationBar
import com.motomusic.app.ui.components.GlassTab
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
    audioViewModel: AudioViewModel = hiltViewModel(),
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
            MotoAppContent(
                mainViewModel = mainViewModel,
                playerViewModel = playerViewModel,
                audioViewModel = audioViewModel,
            )
        }
    }
}

@Composable
private fun MotoAppContent(
    mainViewModel: MainViewModel,
    playerViewModel: PlayerViewModel,
    audioViewModel: AudioViewModel,
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
    val equalizer by audioViewModel.equalizer.collectAsStateWithLifecycle()
    val outputs by audioViewModel.outputs.collectAsStateWithLifecycle()
    val audioSheet by audioViewModel.sheet.collectAsStateWithLifecycle()

    // The full player sits over the app instead of being a screen in the graph, so whatever the
    // user was looking at stays put underneath and is revealed as the player slides away.
    var playerExpanded by rememberSaveable { mutableStateOf(false) }
    var queueOpen by rememberSaveable { mutableStateOf(false) }
    val collapsePlayer = {
        playerExpanded = false
        queueOpen = false
    }

    /** Leaves the player for a screen in the library, such as an album from the song menu. */
    val navigateFromPlayer: (String) -> Unit = { route ->
        collapsePlayer()
        navController.navigateTo(route)
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        mainViewModel.messages.collect { snackbarHostState.showSnackbar(it) }
    }

    val topLevel = TopLevelDestination.entries.firstOrNull { it.route == currentRoute }
    val tabs = remember { TopLevelDestination.entries.map { GlassTab(it.label, it.icon) } }
    val showTabs = topLevel != null
    val tabsTransition = updateTransition(showTabs, label = "tabs")
    var lastTopLevel by remember { mutableStateOf(TopLevelDestination.entries.first()) }
    LaunchedEffect(topLevel) { if (topLevel != null) lastTopLevel = topLevel }

    // The dock's live height is written every frame while it animates, but only read here, so
    // it never recomposes anything. The settled value is what the screens are laid out against.
    var dockHeightPx by remember { mutableIntStateOf(0) }
    var settledDockPx by remember { mutableIntStateOf(0) }
    LaunchedEffect(tabsTransition) {
        snapshotFlow { if (tabsTransition.currentState == tabsTransition.targetState) dockHeightPx else null }
            .filterNotNull()
            .collect { settledDockPx = it }
    }
    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current

    CompositionLocalProvider(
        LocalSongActions provides playerViewModel,
        LocalFavoriteIds provides favoriteIds,
        LocalNowPlayingId provides playback.current?.songId,
    ) {
        Box(Modifier.fillMaxSize()) {
            Scaffold(
                snackbarHost = { if (!playerExpanded) SnackbarHost(snackbarHostState) },
                bottomBar = {
                    Column(Modifier.onSizeChanged { dockHeightPx = it.height }) {
                        MiniPlayer(
                            track = playback.current,
                            isPlaying = playback.isPlaying,
                            progress = { position.value.progress },
                            onPlayPause = playerViewModel::togglePlayPause,
                            onPrevious = playerViewModel::previous,
                            onNext = playerViewModel::next,
                            onExpand = { playerExpanded = true },
                            onDismiss = playerViewModel::clearQueue,
                        )
                        // Detail screens give the bar's room back to their content, smoothly.
                        tabsTransition.AnimatedVisibility(
                            visible = { it },
                            enter = expandVertically(expandFrom = Alignment.Top) + fadeIn(),
                            exit = shrinkVertically(shrinkTowards = Alignment.Top) + fadeOut(),
                        ) {
                            GlassNavigationBar(
                                tabs = tabs,
                                // While the bar shrinks away, keep the tab it belonged to lit.
                                selectedIndex = TopLevelDestination.entries.indexOf(topLevel ?: lastTopLevel),
                                onSelect = { index ->
                                    navController.navigateToTopLevel(TopLevelDestination.entries[index].route)
                                },
                                windowInsets = WindowInsets(0),
                            )
                        }
                        // The strip behind the system navigation bar. It stays in place whether
                        // or not the tabs are showing, so the dock never jumps by the inset.
                        val stripColor by animateColorAsState(
                            targetValue = if (showTabs) MaterialTheme.colorScheme.surface else Color.Transparent,
                            label = "navStrip",
                        )
                        Spacer(
                            Modifier
                                .fillMaxWidth()
                                .drawBehind { drawRect(stripColor) }
                                .windowInsetsBottomHeight(WindowInsets.navigationBars),
                        )
                    }
                },
            ) { innerPadding ->
                // Screens get the dock's height only once it has settled. Passing the animating
                // height would re-layout both screens of a navigation transition every frame.
                val bottom = if (settledDockPx > 0) {
                    with(density) { settledDockPx.toDp() }
                } else {
                    innerPadding.calculateBottomPadding()
                }
                val contentPadding = PaddingValues(
                    start = innerPadding.calculateStartPadding(layoutDirection),
                    top = innerPadding.calculateTopPadding(),
                    end = innerPadding.calculateEndPadding(layoutDirection),
                    bottom = bottom,
                )
                MotoNavHost(
                    navController = navController,
                    contentPadding = contentPadding,
                    playerViewModel = playerViewModel,
                    onRescan = mainViewModel::refreshLibrary,
                    onOpenEqualizer = audioViewModel::openEqualizer,
                    onOpenOutputs = audioViewModel::openOutputs,
                    modifier = Modifier.fillMaxSize(),
                )
            }

            PlayerOverlay(
                expanded = playerExpanded,
                onCollapse = collapsePlayer,
                swipeEnabled = !queueOpen,
            ) {
                AnimatedContent(
                    targetState = queueOpen,
                    transitionSpec = {
                        // The queue rises over the player and sinks back into it.
                        if (targetState) {
                            (slideInVertically { it / 6 } + fadeIn()) togetherWith fadeOut()
                        } else {
                            fadeIn() togetherWith (slideOutVertically { it / 6 } + fadeOut())
                        }
                    },
                    label = "playerQueue",
                ) { showingQueue ->
                    if (showingQueue) {
                        // Inside the queue, Back returns to the player rather than closing both.
                        // Registered as the queue opens, so it outranks the handlers beneath it.
                        BackHandler { queueOpen = false }
                        QueueScreen(
                            playback = playback,
                            contentPadding = PaddingValues(),
                            onBack = { queueOpen = false },
                            onPlayIndex = playerViewModel::skipToQueueIndex,
                            onRemove = playerViewModel::removeFromQueue,
                            onMove = playerViewModel::moveQueueItem,
                            onClear = playerViewModel::clearQueue,
                        )
                    } else {
                        PlayerScreen(
                            playback = playback,
                            position = position,
                            isFavorite = playback.current?.songId?.let { it in favoriteIds } == true,
                            sleepTimer = sleepTimer,
                            onBack = collapsePlayer,
                            onPlayPause = playerViewModel::togglePlayPause,
                            onNext = playerViewModel::next,
                            onPrevious = playerViewModel::previous,
                            onSeek = playerViewModel::seekTo,
                            onToggleShuffle = playerViewModel::toggleShuffle,
                            onCycleRepeat = playerViewModel::cycleRepeatMode,
                            onToggleFavorite = playerViewModel::toggleFavoriteForCurrent,
                            onOpenQueue = { queueOpen = true },
                            onOpenSleepTimer = playerViewModel::openSleepTimer,
                            onOpenMenu = { playerViewModel.openMenuForCurrentSong() },
                            onOpenAlbum = {
                                playback.current?.let { navigateFromPlayer(Routes.albumDetails(it.albumId)) }
                            },
                            output = outputs.active,
                            outputPinned = outputs.selectedId != null,
                            equalizerOn = equalizer.enabled,
                            onOpenOutputs = audioViewModel::openOutputs,
                            onOpenEqualizer = audioViewModel::openEqualizer,
                        )
                    }
                }
            }

            if (playerExpanded) {
                SnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding(),
                )
            }
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
            onGoToAlbum = { song -> navigateFromPlayer(Routes.albumDetails(song.albumId)) },
            onGoToArtist = { song -> navigateFromPlayer(Routes.artistDetails(song.artistId)) },
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

    when (audioSheet) {
        AudioSheet.EQUALIZER -> EqualizerSheet(
            state = equalizer,
            onDismiss = audioViewModel::dismissSheet,
            onEnabledChange = audioViewModel::setEqualizerEnabled,
            onSelectPreset = audioViewModel::selectPreset,
            onBandLevelChange = audioViewModel::setBandLevel,
            onBandLevelChangeFinished = audioViewModel::commitBandLevels,
            onReset = audioViewModel::resetEqualizer,
        )
        AudioSheet.OUTPUTS -> AudioOutputSheet(
            state = outputs,
            canShowSystemSwitcher = audioViewModel.canShowSystemSwitcher,
            onDismiss = audioViewModel::dismissSheet,
            onSelect = audioViewModel::selectOutput,
            onShowSystemSwitcher = audioViewModel::showSystemSwitcher,
        )
        null -> Unit
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
