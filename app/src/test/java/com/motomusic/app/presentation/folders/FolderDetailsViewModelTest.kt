package com.motomusic.app.presentation.folders

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.motomusic.app.navigation.Routes
import com.motomusic.app.util.FakeSongRepository
import com.motomusic.app.util.MainDispatcherRule
import com.motomusic.app.util.testSong
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FolderDetailsViewModelTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val songRepository = FakeSongRepository(
        listOf(
            testSong(1, folderPath = "/storage/emulated/0/Music/Neon Harbour"),
            testSong(2, folderPath = "/storage/emulated/0/Music/Neon Harbour"),
            testSong(3, folderPath = "/storage/emulated/0/Download"),
        ),
    )

    private fun viewModelFor(path: String) = FolderDetailsViewModel(
        savedStateHandle = SavedStateHandle(mapOf(Routes.ARG_FOLDER_PATH to path)),
        songRepository = songRepository,
    )

    @Test
    fun `only the songs in that folder are shown`() = runTest(mainDispatcherRule.testDispatcher) {
        viewModelFor("/storage/emulated/0/Music/Neon Harbour").state.test {
            advanceUntilIdle()
            val state = expectMostRecentItem()

            assertEquals(listOf(1L, 2L), state.songs.map { it.id })
            assertEquals("Neon Harbour", state.name)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `the title is the last path segment`() {
        assertEquals("Download", FolderDetailsUiState(path = "/storage/emulated/0/Download").name)
    }

    @Test
    fun `a path with no separator is its own name`() {
        assertEquals("Music", FolderDetailsUiState(path = "Music").name)
    }

    @Test
    fun `a trailing separator does not leave the title blank`() {
        assertEquals("/storage/", FolderDetailsUiState(path = "/storage/").name)
    }
}
