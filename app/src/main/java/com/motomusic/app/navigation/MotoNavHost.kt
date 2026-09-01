package com.motomusic.app.navigation

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.motomusic.app.presentation.albums.AlbumDetailsScreen
import com.motomusic.app.presentation.albums.AlbumDetailsViewModel
import com.motomusic.app.presentation.albums.AlbumsScreen
import com.motomusic.app.presentation.albums.AlbumsViewModel
import com.motomusic.app.presentation.artists.ArtistDetailsScreen
import com.motomusic.app.presentation.artists.ArtistDetailsViewModel
import com.motomusic.app.presentation.artists.ArtistsScreen
import com.motomusic.app.presentation.artists.ArtistsViewModel
import com.motomusic.app.presentation.collection.SongCollectionScreen
import com.motomusic.app.presentation.collection.SongCollectionViewModel
import com.motomusic.app.presentation.folders.FolderDetailsScreen
import com.motomusic.app.presentation.folders.FolderDetailsViewModel
import com.motomusic.app.presentation.folders.FoldersScreen
import com.motomusic.app.presentation.folders.FoldersViewModel
import com.motomusic.app.presentation.home.HomeScreen
import com.motomusic.app.presentation.home.HomeViewModel
import com.motomusic.app.presentation.player.PlayerScreen
import com.motomusic.app.presentation.player.PlayerViewModel
import com.motomusic.app.presentation.playlists.PlaylistDetailsScreen
import com.motomusic.app.presentation.playlists.PlaylistDetailsViewModel
import com.motomusic.app.presentation.playlists.PlaylistsScreen
import com.motomusic.app.presentation.playlists.PlaylistsViewModel
import com.motomusic.app.presentation.queue.QueueScreen
import com.motomusic.app.presentation.search.SearchScreen
import com.motomusic.app.presentation.search.SearchViewModel
import com.motomusic.app.presentation.settings.AboutScreen
import com.motomusic.app.presentation.settings.SettingsScreen
import com.motomusic.app.presentation.settings.SettingsViewModel
import com.motomusic.app.presentation.songs.SongsScreen
import com.motomusic.app.presentation.songs.SongsViewModel

/**
 * Every destination in the app.
 *
 * [playerViewModel] is passed down rather than resolved per screen: playback is one shared thing,
 * and the sheets it drives are hosted by the app scaffold above this graph.
 */
@Composable
fun MotoNavHost(
    navController: NavHostController,
    contentPadding: PaddingValues,
    playerViewModel: PlayerViewModel,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                contentPadding = contentPadding,
                onOpenSearch = { navController.navigateTo(Routes.SEARCH) },
                onOpenSettings = { navController.navigateTo(Routes.SETTINGS) },
                onOpenSongs = { navController.navigateTo(Routes.SONGS) },
                onOpenFavorites = { navController.navigateTo(Routes.FAVORITES) },
                onOpenRecentlyAdded = { navController.navigateTo(Routes.RECENTLY_ADDED) },
                onOpenMostPlayed = { navController.navigateTo(Routes.MOST_PLAYED) },
                onOpenFolders = { navController.navigateTo(Routes.FOLDERS) },
                onRescan = onRescan,
            )
        }

        composable(Routes.SONGS) {
            val viewModel: SongsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SongsScreen(
                state = state,
                contentPadding = contentPadding,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onSortOrderChange = viewModel::setSortOrder,
                onRescan = viewModel::rescan,
            )
        }

        composable(Routes.ALBUMS) {
            val viewModel: AlbumsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            AlbumsScreen(
                state = state,
                contentPadding = contentPadding,
                onAlbumClick = { navController.navigateTo(Routes.albumDetails(it.id)) },
            )
        }

        composable(Routes.ARTISTS) {
            val viewModel: ArtistsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArtistsScreen(
                state = state,
                contentPadding = contentPadding,
                onArtistClick = { navController.navigateTo(Routes.artistDetails(it.id)) },
            )
        }

        composable(Routes.PLAYLISTS) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistsScreen(
                state = state,
                contentPadding = contentPadding,
                onPlaylistClick = { navController.navigateTo(Routes.playlistDetails(it.id)) },
                onCreate = viewModel::createPlaylist,
                onRename = viewModel::renamePlaylist,
                onDelete = viewModel::deletePlaylist,
            )
        }

        composable(
            route = Routes.ALBUM_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_ALBUM_ID) { type = NavType.LongType }),
        ) {
            val viewModel: AlbumDetailsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            AlbumDetailsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onArtistClick = { navController.navigateTo(Routes.artistDetails(it)) },
            )
        }

        composable(
            route = Routes.ARTIST_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_ARTIST_ID) { type = NavType.LongType }),
        ) {
            val viewModel: ArtistDetailsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArtistDetailsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onAlbumClick = { navController.navigateTo(Routes.albumDetails(it.id)) },
            )
        }

        composable(
            route = Routes.PLAYLIST_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_PLAYLIST_ID) { type = NavType.LongType }),
        ) {
            val viewModel: PlaylistDetailsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistDetailsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onRename = viewModel::rename,
                onDelete = { viewModel.delete() },
                onOpenSongMenu = { song -> playerViewModel.openMenuInPlaylist(song, viewModel.playlistId) },
            )
        }

        composable(Routes.FOLDERS) {
            val viewModel: FoldersViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            FoldersScreen(
                state = state,
                contentPadding = contentPadding,
                onFolderClick = { navController.navigateTo(Routes.folderDetails(it.path)) },
                onBack = navController::popBackStack,
            )
        }

        composable(
            route = Routes.FOLDER_DETAILS,
            arguments = listOf(navArgument(Routes.ARG_FOLDER_PATH) { type = NavType.StringType }),
        ) {
            val viewModel: FolderDetailsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            FolderDetailsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
            )
        }

        composable(
            route = Routes.COLLECTION,
            arguments = listOf(navArgument(Routes.ARG_COLLECTION_TYPE) { type = NavType.StringType }),
        ) {
            val viewModel: SongCollectionViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SongCollectionScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onClearHistory = { viewModel.clearHistory() },
            )
        }

        composable(Routes.SEARCH) {
            val viewModel: SearchViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SearchScreen(
                state = state,
                contentPadding = contentPadding,
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onBack = navController::popBackStack,
            )
        }

        composable(Routes.PLAYER) {
            val playback by playerViewModel.playback.collectAsStateWithLifecycle()
            val position = playerViewModel.position.collectAsStateWithLifecycle()
            val favoriteIds by playerViewModel.favoriteIds.collectAsStateWithLifecycle()
            val sleepTimer by playerViewModel.sleepTimer.collectAsStateWithLifecycle()
            val currentSongId = playback.current?.songId
            PlayerScreen(
                playback = playback,
                position = position,
                isFavorite = currentSongId != null && currentSongId in favoriteIds,
                sleepTimer = sleepTimer,
                onBack = navController::popBackStack,
                onPlayPause = playerViewModel::togglePlayPause,
                onNext = playerViewModel::next,
                onPrevious = playerViewModel::previous,
                onSeek = playerViewModel::seekTo,
                onToggleShuffle = playerViewModel::toggleShuffle,
                onCycleRepeat = playerViewModel::cycleRepeatMode,
                onToggleFavorite = playerViewModel::toggleFavoriteForCurrent,
                onOpenQueue = { navController.navigateTo(Routes.QUEUE) },
                onOpenSleepTimer = playerViewModel::openSleepTimer,
                onOpenMenu = { playerViewModel.openMenuForCurrentSong() },
            )
        }

        composable(Routes.QUEUE) {
            val playback by playerViewModel.playback.collectAsStateWithLifecycle()
            QueueScreen(
                playback = playback,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onPlayIndex = playerViewModel::skipToQueueIndex,
                onRemove = playerViewModel::removeFromQueue,
                onMove = playerViewModel::moveQueueItem,
                onClear = playerViewModel::clearQueue,
            )
        }

        composable(Routes.SETTINGS) {
            val viewModel: SettingsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SettingsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onThemeModeChange = { viewModel.setThemeMode(it) },
                onDynamicColorChange = { viewModel.setDynamicColor(it) },
                onResumeLastSessionChange = { viewModel.setResumeLastSession(it) },
                onSkipSilenceChange = { viewModel.setSkipSilence(it) },
                onPauseOnHeadphonesChange = { viewModel.setPauseOnHeadphonesDisconnect(it) },
                onFadeOnPlayPauseChange = { viewModel.setFadeOnPlayPause(it) },
                onHideVoiceRecordingsChange = { viewModel.setHideVoiceRecordings(it) },
                onUnhideSong = { viewModel.unhideSong(it) },
                onUnhideAllSongs = { viewModel.unhideAllSongs() },
                onRescan = { viewModel.rescan() },
                onClearHistory = { viewModel.clearHistory() },
                onOpenAbout = { navController.navigateTo(Routes.ABOUT) },
            )
        }

        composable(Routes.ABOUT) {
            AboutScreen(contentPadding = contentPadding, onBack = navController::popBackStack)
        }
    }
}

/** Ordinary forward navigation: one copy of a destination at a time, state kept on the way back. */
fun NavHostController.navigateTo(route: String) {
    navigate(route) { launchSingleTop = true }
}

/** Bottom-bar navigation: pops back to the start destination so the back stack cannot grow. */
fun NavHostController.navigateToTopLevel(route: String) {
    navigate(route) {
        popUpTo(graph.startDestinationId) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}
