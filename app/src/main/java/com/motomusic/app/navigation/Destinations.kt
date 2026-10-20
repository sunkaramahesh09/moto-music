package com.motomusic.app.navigation

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.ui.graphics.vector.ImageVector

/** Every screen the app can navigate to. */
object Routes {
    /** The Library tab. Kept as "home" so saved navigation state from older versions still resolves. */
    const val HOME = "home"
    const val BROWSE = "browse"
    const val SONGS = "songs"
    const val ALBUMS = "albums"
    const val ARTISTS = "artists"
    const val PLAYLISTS = "playlists"

    const val ALBUM_DETAILS = "album/{albumId}"
    const val ARTIST_DETAILS = "artist/{artistId}"
    const val PLAYLIST_DETAILS = "playlist/{playlistId}"
    const val FOLDER_DETAILS = "folder/{folderPath}"

    const val FOLDERS = "folders"

    /**
     * Favourites, recently played, frequently played and recently added are the same screen with
     * a different query behind it, so they share one route with the kind as its argument.
     */
    const val COLLECTION = "collection/{collectionType}"
    const val FAVORITES = "collection/favorites"
    const val RECENTLY_PLAYED = "collection/recently_played"
    const val MOST_PLAYED = "collection/most_played"
    const val RECENTLY_ADDED = "collection/recently_added"

    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val ABOUT = "about"

    const val ARG_ALBUM_ID = "albumId"
    const val ARG_ARTIST_ID = "artistId"
    const val ARG_PLAYLIST_ID = "playlistId"
    const val ARG_FOLDER_PATH = "folderPath"
    const val ARG_COLLECTION_TYPE = "collectionType"

    fun albumDetails(albumId: Long) = "album/$albumId"
    fun artistDetails(artistId: Long) = "artist/$artistId"
    fun playlistDetails(playlistId: Long) = "playlist/$playlistId"

    /** Folder paths contain slashes, so they are encoded before becoming a route segment. */
    fun folderDetails(path: String) = "folder/${Uri.encode(path)}"
}

enum class TopLevelDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    LIBRARY(Routes.HOME, "Library", Icons.Rounded.LibraryMusic),
    SEARCH(Routes.SEARCH, "Search", Icons.Rounded.Search),
    BROWSE(Routes.BROWSE, "Browse", Icons.Rounded.Explore),
}
