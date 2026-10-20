package com.motomusic.app.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
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
import com.motomusic.app.presentation.browse.BrowseScreen
import com.motomusic.app.presentation.browse.BrowseViewModel
import com.motomusic.app.presentation.collection.SongCollectionScreen
import com.motomusic.app.presentation.collection.SongCollectionViewModel
import com.motomusic.app.presentation.folders.FolderDetailsScreen
import com.motomusic.app.presentation.folders.FolderDetailsViewModel
import com.motomusic.app.presentation.folders.FoldersScreen
import com.motomusic.app.presentation.folders.FoldersViewModel
import com.motomusic.app.presentation.home.HomeScreen
import com.motomusic.app.presentation.home.HomeViewModel
import com.motomusic.app.presentation.player.PlayerViewModel
import com.motomusic.app.presentation.playlists.PlaylistDetailsScreen
import com.motomusic.app.presentation.playlists.PlaylistDetailsViewModel
import com.motomusic.app.presentation.playlists.PlaylistsScreen
import com.motomusic.app.presentation.playlists.PlaylistsViewModel
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
    onOpenEqualizer: () -> Unit,
    onOpenOutputs: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Bumped when the Library's search bar sends the user to the Search tab to type.
    var searchFocusRequest by remember { mutableIntStateOf(0) }

    NavHost(
        navController = navController,
        startDestination = Routes.HOME,
        modifier = modifier,
        enterTransition = { if (isTopLevelSwitch()) fadeThroughIn() else sharedAxisIn(forward = true) },
        exitTransition = { if (isTopLevelSwitch()) fadeThroughOut() else sharedAxisOut(forward = true) },
        popEnterTransition = { if (isTopLevelSwitch()) fadeThroughIn() else sharedAxisIn(forward = false) },
        popExitTransition = { if (isTopLevelSwitch()) fadeThroughOut() else sharedAxisOut(forward = false) },
        // Every screen fills the same space; animating the size would only make lists jump.
        sizeTransform = null,
    ) {
        composable(Routes.HOME) {
            val viewModel: HomeViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            HomeScreen(
                state = state,
                contentPadding = contentPadding,
                onOpenSearch = {
                    searchFocusRequest++
                    navController.navigateToTopLevel(Routes.SEARCH)
                },
                onOpenSettings = { navController.navigateTo(Routes.SETTINGS) },
                onOpenOutputs = onOpenOutputs,
                onOpenSongs = { navController.navigateTo(Routes.SONGS) },
                onOpenAlbums = { navController.navigateTo(Routes.ALBUMS) },
                onOpenArtists = { navController.navigateTo(Routes.ARTISTS) },
                onOpenPlaylists = { navController.navigateTo(Routes.PLAYLISTS) },
                onOpenFolders = { navController.navigateTo(Routes.FOLDERS) },
                onOpenFavorites = { navController.navigateTo(Routes.FAVORITES) },
                onOpenRecentlyAdded = { navController.navigateTo(Routes.RECENTLY_ADDED) },
                onOpenMostPlayed = { navController.navigateTo(Routes.MOST_PLAYED) },
                onRescan = onRescan,
            )
        }

        composable(Routes.BROWSE) {
            val viewModel: BrowseViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            BrowseScreen(
                state = state,
                contentPadding = contentPadding,
                onShuffleEverything = { viewModel.shuffleEverything() },
                onPlayMix = { viewModel.playMix(it) },
                onAlbumClick = { navController.navigateTo(Routes.albumDetails(it.id)) },
                onOpenAlbums = { navController.navigateTo(Routes.ALBUMS) },
                onOpenMostPlayed = { navController.navigateTo(Routes.MOST_PLAYED) },
                onOpenEqualizer = onOpenEqualizer,
                onOpenOutputs = onOpenOutputs,
                onOpenSettings = { navController.navigateTo(Routes.SETTINGS) },
                onRescan = onRescan,
            )
        }

        composable(Routes.SONGS) {
            val viewModel: SongsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            SongsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
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
                onBack = navController::popBackStack,
                onAlbumClick = { navController.navigateTo(Routes.albumDetails(it.id)) },
            )
        }

        composable(Routes.ARTISTS) {
            val viewModel: ArtistsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            ArtistsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
                onArtistClick = { navController.navigateTo(Routes.artistDetails(it.id)) },
            )
        }

        composable(Routes.PLAYLISTS) {
            val viewModel: PlaylistsViewModel = hiltViewModel()
            val state by viewModel.state.collectAsStateWithLifecycle()
            PlaylistsScreen(
                state = state,
                contentPadding = contentPadding,
                onBack = navController::popBackStack,
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
                focusRequest = searchFocusRequest,
                onFocusHandled = { searchFocusRequest = 0 },
                onQueryChange = viewModel::onQueryChange,
                onClearQuery = viewModel::clearQuery,
                onRememberQuery = viewModel::rememberQuery,
                onRemoveRecent = viewModel::removeRecentSearch,
                onClearRecents = viewModel::clearRecentSearches,
                onOpenAlbums = { navController.navigateTo(Routes.ALBUMS) },
                onOpenArtists = { navController.navigateTo(Routes.ARTISTS) },
                onOpenPlaylists = { navController.navigateTo(Routes.PLAYLISTS) },
                onOpenFolders = { navController.navigateTo(Routes.FOLDERS) },
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
                onHighResOutputChange = { viewModel.setHighResOutput(it) },
                onOpenEqualizer = onOpenEqualizer,
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

/*
 * Transitions follow Material motion. Moving between the bottom-bar tabs is a fade-through:
 * the tabs are siblings, so nothing slides. Going deeper (an album, Settings) is a shared-axis
 * step: the new screen comes in a little from the side it lives on and the old one drifts the
 * other way, which tells the user which way Back will go.
 */

private val topLevelRoutes = TopLevelDestination.entries.map { it.route }.toSet()

private fun AnimatedContentTransitionScope<NavBackStackEntry>.isTopLevelSwitch(): Boolean =
    initialState.destination.route in topLevelRoutes && targetState.destination.route in topLevelRoutes

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val EmphasizedAccelerate = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)

private const val SHARED_AXIS_MS = 350
private const val FADE_THROUGH_MS = 300

/** Fraction of the width a screen travels: enough to read as direction, not as a page flip. */
private const val SHARED_AXIS_TRAVEL = 0.15f

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisIn(forward: Boolean): EnterTransition =
    slideIntoContainer(
        towards = if (forward) SlideDirection.Start else SlideDirection.End,
        animationSpec = tween(SHARED_AXIS_MS, easing = EmphasizedDecelerate),
        initialOffset = { (it * SHARED_AXIS_TRAVEL).toInt() },
    ) + fadeIn(tween(SHARED_AXIS_MS / 2, delayMillis = SHARED_AXIS_MS / 6))

private fun AnimatedContentTransitionScope<NavBackStackEntry>.sharedAxisOut(forward: Boolean): ExitTransition =
    slideOutOfContainer(
        towards = if (forward) SlideDirection.Start else SlideDirection.End,
        animationSpec = tween(SHARED_AXIS_MS, easing = EmphasizedAccelerate),
        targetOffset = { (it * SHARED_AXIS_TRAVEL).toInt() },
    ) + fadeOut(tween(SHARED_AXIS_MS / 3))

private fun fadeThroughIn(): EnterTransition =
    fadeIn(tween(FADE_THROUGH_MS * 7 / 10, delayMillis = FADE_THROUGH_MS * 3 / 10)) +
        scaleIn(tween(FADE_THROUGH_MS * 7 / 10, delayMillis = FADE_THROUGH_MS * 3 / 10), initialScale = 0.94f)

private fun fadeThroughOut(): ExitTransition = fadeOut(tween(FADE_THROUGH_MS * 3 / 10))

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
