# Moto Music — Runbook

A local-only Android music player: it plays audio files already on the device, holds no
INTERNET permission, and keeps playlists, favourites and history in a local Room database.
Published as an open-source project under the GPL-3.0 — see README.md and CONTRIBUTING.md, which
are what the outside world reads; this runbook is the working memory behind them.

Last updated: 2026-09-01 (session 4).

---

## 0. Test music on the device

**The 11 generated test tracks were deleted on 2026-09-01 (session 4)** at the user's request,
and the phone is back to its own 19 audio files. There is no generated library on it any more.

To put one back, write m4a files into `/sdcard/Music/<Artist>/<Album>/` (macOS `say` +
`afconvert`, tagged with `mutagen`), then force a metadata scan.
**`MEDIA_SCANNER_SCAN_FILE` is a no-op on Android 10+** — it creates filename-only rows with
NULL artist/album. Use this instead, after adding *or* removing files:

```bash
adb shell "content call --uri content://media --method scan_volume --arg external_primary"
```

Put a generated library under a folder the voice-note filter ignores — anything matching
`Recordings/`, `Recorder/`, `records/` or the WhatsApp media directory is now left out of the
library by default (see `MediaStoreScanner.isVoiceRecording`) — and note that individually
hidden files (Settings → Library → "Hidden songs") are filtered too.

---

## 1. Environment

The Android SDK had vanished from this machine and was reinstalled on 2026-08-31. If it goes
missing again (`SDK location not found` from Gradle), redo this:

```bash
mkdir -p ~/Library/Android/sdk/cmdline-tools && cd ~/Library/Android/sdk/cmdline-tools
curl -sSL -o clt.zip https://dl.google.com/android/repository/commandlinetools-mac-13114758_latest.zip
unzip -q clt.zip && mv cmdline-tools latest && rm clt.zip

export ANDROID_HOME=~/Library/Android/sdk
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME --licenses
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --sdk_root=$ANDROID_HOME \
  "platform-tools" "platforms;android-37.1" "build-tools;37.0.0"
```

`local.properties` already points at `sdk.dir=/Users/mahesh/Library/Android/sdk`. Gradle needs
`ANDROID_HOME` exported (or the `sdk.dir` line) — every command below assumes it.

Toolchain: AGP 9.3.2 · Kotlin 2.4.10 · KSP · Hilt 2.60.1 · compileSdk 37 (minor 1) · minSdk 26 ·
Java 17 target · Compose BOM 2026.08.00 · Media3 1.11.0 · Room 2.8.4 · DataStore 1.2.1.

### Commands

```bash
export ANDROID_HOME=$HOME/Library/Android/sdk

./gradlew :app:compileDebugKotlin          # fastest feedback; keep it warning-free
./gradlew :app:testDebugUnitTest           # 55 unit tests
./gradlew :app:lintDebug                   # currently 0 issues
./gradlew :app:assembleDebug               # app/build/outputs/apk/debug/app-debug.apk (24 MB)
./gradlew :app:assembleRelease             # R8 + resource shrinking, debug-signed (3.0 MB)
./gradlew :app:assembleDebugAndroidTest    # instrumentation APK compiles; nothing to run it on yet
```

Warnings are treated as things to fix, not noise: the last full rebuild had **zero** Kotlin
warnings and **zero** lint issues. Keep it that way.

---

## 2. Where the project stands

### Complete and building

| Layer | State |
| --- | --- |
| `data/` | MediaStore scanner, artwork loader, audio probe, Room database + DAOs + migrations, DataStore settings, all five repositories |
| `domain/` | Models (Song, Album, Artist, Folder, Playlist, ScanState, SortOrder, ThemeMode/UserPreferences) and repository interfaces |
| `playback/` | `MusicService` (Media3 MediaSessionService), `PlaybackConnection`, media item mapping, bitmap loader, sleep timer, event bus |
| `ui/` | Theme (dynamic colour + light/dark), artwork, song rows and lists, library items, mini player, empty/loading states, collection header |
| `presentation/` | Every screen and ViewModel (see map below) |
| `navigation/` | `Routes`, `TopLevelDestination`, full `MotoNavHost` |
| Shell | `MotoApp` (theme, permission gate, bottom bar, mini player, snackbar, sheet hosting) wired into `MainActivity` |
| Tests | 55 JVM unit tests, `MainDispatcherRule`, fakes for the song/settings/playlist repositories, `testSong` factory, `HiltTestRunner` |

### Verified on 2026-09-01

- `compileDebugKotlin` — success, no warnings
- `testDebugUnitTest` — 55/55 pass
- `lintDebug` — 0 issues
- `assembleDebug` and `assembleRelease` (R8) — both succeed
- `assembleDebugAndroidTest` — compiles

### Verified on real hardware — 2026-09-01

Installed and walked end to end on a **moto g54 5G, Android 15 (API 35), arm64**, with 28 tracks
(17 of the device's own voice notes plus the 11 generated test files). **No crash anywhere in the
walkthrough** — `logcat -b crash` stayed empty throughout. The test files have since been
deleted and the voice notes are filtered out, so that walkthrough would now run against 3 songs;
redo it against a real library before trusting it again.

Confirmed working: Home (shortcuts, recently/frequently played), Songs (sort, search, play,
shuffle), Albums + album detail, Artists + artist detail, Folders + folder detail, Search
(matches title *and* artist, auto-focused), Favourites, full player (transport, seek, repeat,
shuffle, favourite), queue (reorder, remove, clear), sleep-timer sheet, song menu, add-to-playlist,
the playlist-scoped "Remove from this playlist", song details (bitrate/sample rate/channels all
populated), playlist create/rename/delete, Settings, About, light/dark themes, dynamic colour,
and the media notification (`moto_music_playback`, category=transport).

Also verified on the release build on 2026-09-01 (session 4): "Hide from library" from a song's
menu (including hiding the song that was playing, which leaves the queue and hands playback on),
the restore dialog per file, and the library counts moving 3 → 1 → 2 → 1 as files were hidden
and restored. Cold-start (`am force-stop` then relaunch) keeps them hidden — the setting is in
DataStore, not in memory.

Re-verified on the release build on 2026-09-01 (session 4): playback, the fade-out on pause
(425 ms from media key to PAUSED), rapid play/pause/play with no stuck-silent player, both new
settings, and the library going 3 → 18 → 3 songs as the voice-note switch is flipped. No crash.

Still unverified: lock-screen and Bluetooth controls, headphone-disconnect pause, sleep timer
actually firing, "resume last session" across a cold start, and behaviour on a large library.

### Performance: judge it on the release build only — 2026-09-01

Same code, same phone, same scripted drag across the seek bar, `dumpsys gfxinfo`:

| Build | Janky frames | 50th | 90th | 99th |
| --- | --- | --- | --- | --- |
| debug (`com.motomusic.app.debug`) | 395/609 — **64.9%** | 27 ms | 34 ms | 46 ms |
| release (`com.motomusic.app`) | 99/859 — **11.5%** | 9 ms | 13 ms | 24 ms |

The screen runs at 120 Hz, so the budget is 8.3 ms. A debug build is not slow by accident: it is
not R8'd, it is `debuggable` (which disables several ART optimisations), and Compose's bundled
baseline profiles are not installed. **Never judge smoothness from `assembleDebug`** — that is
what made the app feel like it dropped frames. Release is signed with the local debug key
(see `app/build.gradle.kts`) precisely so it can be installed and felt.

Ten seconds of playback with nobody touching the screen renders 40 frames — four a second, the
position tick — which is the intended shape: only the seek bar and the elapsed label redraw.

The debug build's application id is **`com.motomusic.app.debug`** (`applicationIdSuffix`), so
`adb shell pm grant com.motomusic.app …` fails with "package not found" — use the `.debug` id.
The launcher activity is still `com.motomusic.app.MainActivity`:

```bash
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
adb shell am start -n com.motomusic.app.debug/com.motomusic.app.MainActivity

# the one to test smoothness on:
adb install -r -g app/build/outputs/apk/release/app-release.apk
adb shell am start -n com.motomusic.app/com.motomusic.app.MainActivity
```

**The emulator is a dead end on this machine.** `avdmanager create avd` writes
`hw.gpu.enabled=no`, and the guest then never executes (black screen, ~1% CPU, no kernel output).
Setting `hw.gpu.enabled=yes` / `hw.gpu.mode=host` in `~/.android/avd/<name>.avd/config.ini` is the
fix, but it was never confirmed to boot — the physical phone is faster and has a real library.
Note also that a `nohup`-ed emulator still dies when the launching tool call is interrupted, since
`nohup` blocks SIGHUP and not SIGTERM.

---

## 2b. Device and tooling state

**Test phone**: moto g54 5G (`ZD222FJ7P3`), Android 15 / API 35, arm64, connected over USB with
USB debugging. `adb devices` should show it before anything else here works.

**UI driver**: `scratchpad/ui.py` drives the phone by element text and content-description via
`uiautomator dump`, rather than fixed coordinates. Use `safe_tap` / `type_text`, not the raw
versions — they refuse to act unless Moto Music is the resumed activity and, for typing, a real
`EditText` has focus.

> Learned the hard way: a mistargeted tap opened the quick-settings shade, and the text sent
> afterwards toggled **aeroplane mode** on the phone. Nothing in the app caused it; it was the
> automation. Never send `input text` without confirming focus first, and prefer exact text
> matching for navigation — substring matching once made "Songs" match "0 songs".

**UI driver**: rewritten each session into the session scratchpad; it is not checked in.

**State deliberately left on the device** after 2026-09-01 (session 4):

- **both builds are installed**: `com.motomusic.app` (release, the fast one, launcher name
  "Moto Music") and `com.motomusic.app.debug` ("Moto Music (debug)"). The debug one can go with
  `adb uninstall com.motomusic.app.debug`; as of this session it holds no favourites, only test
  play history.
- the generated test tracks are **gone**; the phone has only its own 19 audio files, of which
  the library shows **1** — the WhatsApp voice notes and recorder files are filtered by folder,
  and the two the folder rules could not catch are in the release build's hidden list
  (Settings → Library → "Hidden songs"), which is app-local and per build,
- listening history in the release build from the verification run above,
- theme "Follow system"; media volume still at 5 from the silent testing.

---

## 3. File map (`app/src/main/java/com/motomusic/app/`)

```
MainActivity.kt              provides LocalArtworkLoader, hosts MotoApp, refreshes permission on resume
MotoMusicApplication.kt      @HiltAndroidApp

core/                        MediaPermission, TimeFormat (formatDuration/FileSize/Bitrate/SampleRate/
                             TotalDuration, pluralise)
data/…                       mediastore/, local/database/, local/prefs/, repository/
di/                          CoroutinesModule, DatabaseModule, RepositoryModule, Qualifiers
domain/                      model/, repository/
playback/                    MusicService, PlaybackConnection, PlaybackState, MediaItemMapper,
                             MotoBitmapLoader, SleepTimerController, PlaybackEventBus,
                             FadingPlayer (play/pause volume ramp)

navigation/
  Destinations.kt            Routes + TopLevelDestination (Home, Songs, Albums, Artists, Playlists)
  MotoNavHost.kt             every composable destination; navigateTo / navigateToTopLevel

presentation/
  app/        MotoApp, MainViewModel, Greeting
  common/     SongActions + LocalSongActions/LocalFavoriteIds/LocalNowPlayingId,
              CollectionScreen (shared "title + song list"), CollectionSummary,
              SongInfoDialog, SongInfoViewModel
  home/       HomeScreen, HomeViewModel
  songs/      SongsScreen, SongsViewModel
  search/     SearchScreen, SearchViewModel
  albums/     AlbumsScreen/VM, AlbumDetailsScreen/VM
  artists/    ArtistsScreen/VM, ArtistDetailsScreen/VM
  folders/    FoldersScreen/VM, FolderDetailsScreen/VM
  playlists/  PlaylistsScreen/VM, PlaylistDetailsScreen/VM, PlaylistDialogs, AddToPlaylistSheet
  collection/ SongCollection (enum), SongCollectionScreen/VM  ← favourites, recently played,
              frequently played, recently added all share this one screen
  player/     PlayerScreen, PlayerViewModel, SongMenuSheet, SongMenuTarget, SleepTimerSheet
  queue/      QueueScreen
  settings/   SettingsScreen, SettingsViewModel, AboutScreen
  permission/ PermissionScreen
ui/components/, ui/theme/
```

Tests live in `app/src/test/java/com/motomusic/app/` (`core/`, `data/mediastore/`, `domain/model/`,
`presentation/{app,common,collection,folders,player,playlists,settings,songs}/`, `util/`) and the Hilt runner in
`app/src/androidTest/java/com/motomusic/app/HiltTestRunner.kt`.

---

## 4. Conventions to keep following

- **Screens are stateless.** A screen takes `state: XUiState`, `contentPadding: PaddingValues` and
  callbacks. ViewModels are resolved in `MotoNavHost`, never inside a screen (the two exceptions
  are `SongInfoDialog`, which owns a probe, and `MotoApp` itself).
- **`contentPadding`** comes from the outer `Scaffold` and is used only for
  `calculateBottomPadding()` — each screen has its own `Scaffold`/`TopAppBar` for the top inset.
- **Playback is one object.** `PlayerViewModel` is created once in `MotoApp`, implements
  `SongActions`, and is published through `LocalSongActions`. Any list can call
  `LocalSongActions.current.play(songs, index)`.
- **The three-dot menu is hosted by the shell**, driven by `PlayerViewModel.menuTarget`. To add a
  context-specific entry (as playlists do for "Remove from this playlist"), override
  `openMenu` in a scoped `SongActions` — see `PlaylistDetailsScreen`.
- **"Title + list of songs" screens use `CollectionScreen`.** Only the header differs.
- **UI text uses British spelling** ("Favourites"); code identifiers use `Favorite`.
- **State flows**: `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`, with
  `isLoading` in the initial value.
- **The playing position is never unwrapped high up.** `MotoApp` and `MotoNavHost` collect it
  with `collectAsStateWithLifecycle()` and pass the `State` (or a `() -> Float`) down; only
  `PlayerScreen.SeekSection` and the mini player's progress bar read `.value`. Writing
  `val position by …` in the shell puts the whole app — nav host included — on a 4 Hz
  recomposition loop whenever music plays.
- Routes carrying an id use `NavType.LongType`; folder paths are `Uri.encode`d by
  `Routes.folderDetails`.

---

## 5. Gotchas already paid for

- **ViewModels in unit tests must be built lazily** (`private val viewModel by lazy { … }`).
  JUnit constructs test fields *before* rules run, so an eagerly-built ViewModel captures
  `Dispatchers.Main` before `MainDispatcherRule` installs the test dispatcher, and
  `advanceUntilIdle()` then drives nothing. This cost three failing tests.
- **Share the scheduler**: write `runTest(mainDispatcherRule.testDispatcher) { … }`.
- **Artwork cache keys must fall back to the song URI** when MediaStore gave the file no album
  (`albumId <= 0`). Keying those on the album id alone made every album-less song on the device
  share one cache entry, so the first cover decoded would have been shown for all of them.
- **Every screen needs a permanent way in.** The Folders screen was reachable only from a Home
  shortcut that was swapped out for "Most played" as soon as any play history existed, so Folders
  and FolderDetails became permanently unreachable after the first play. The Home grid now always
  offers Folders. Worth re-checking whenever a shortcut is made conditional.
- **Edge-to-edge means owning the system bar icons.** `enableEdgeToEdge()` alone left white status
  bar icons on the light theme's white background. `MotoMusicTheme` now sets
  `isAppearanceLightStatusBars`/`isAppearanceLightNavigationBars` from the resolved `darkTheme`,
  which also handles the user switching theme at runtime.
- **Dialogs that exist to collect text must focus the field.** Both playlist dialogs opened with
  the keyboard down. They now request focus, and the rename dialog pre-selects the existing name
  so typing replaces it; the IME "Done" key confirms.
- **Do not try to silence Media3's "Failed to load bitmap" warnings.** A track with no cover art
  logs a burst of ~10 of them on first play. `BitmapLoader.loadBitmapFromMetadata` is called
  exactly *once* per track; Media3 caches the failed future against the metadata and replays it
  internally, so it never asks again — a synchronous "known missing" short-circuit is never
  consulted and is dead code. Verified empirically. The only way to zero them would be to return
  a placeholder bitmap instead of no artwork, which changes what the notification looks like.
- **Stale resource cache**: after renaming a `res/` folder, `processDebugResources` can fail with
  "resource not found". `--rerun-tasks` on that task clears it; it is not a real error. It bit
  `processReleaseResources` too, on the first release build after the rename
  (`mipmap/ic_launcher not found`) — same fix.
- **Fading has to pause *after* the ramp, not before.** `FadingPlayer` defers `pause()` until the
  volume has reached zero; pausing first would leave nothing to fade. The cost is that the
  session reports PAUSED about 300 ms late (measured: 425 ms from a media-key press, fade plus
  IPC), which is the intended feel, not a lag.
- **A volume ramp is an event storm.** Every step fires `EVENT_VOLUME_CHANGED`, and
  `PlaybackConnection` used to republish the entire session state — remapping every queue item —
  on any event at all. It now ignores events the UI does not draw and only rebuilds the queue
  list when the timeline actually changes.
- **MediaStore marks WhatsApp voice notes as music** (`is_music = 1`), which is why a phone with
  no music still shows a full library. They are filtered by path, not by duration or by the
  `is_recording` flag alone: that flag is only ever set for the system Recordings directory.
- **Folder rules cannot catch a recording that moved.** Two files on the test phone proved it: a
  recorder file copied into `Download/` and a call recording trimmed into
  `AudioEditorCutter/Trimmed/`. Nothing about either location says "recording", and a filename
  rule (`record*`) would eventually hide somebody's music. That is what "Hide from library" is
  for — the answer to "the filter missed one" is the manual list, not a wider heuristic.
- **Two builds, two settings stores, one launcher name.** The debug build kept its own
  DataStore, so songs hidden in the release build were still listed in the debug one — and with
  identical names and icons the user opened the wrong app and reasonably reported the hiding as
  broken. `app/src/debug/res/values/strings.xml` now overrides `app_name` to
  "Moto Music (debug)". Any future per-build divergence needs the same treatment: if two builds
  can be installed side by side, they must be *tellable* apart.
- **A hidden song must also leave the queue.** Hiding only rewrote the library, so the file the
  user had just hidden carried on playing in the mini player.
  `PlaybackConnection.removeSongFromQueue` now runs first.
- **`hiltViewModel`** now lives in `androidx.hilt.lifecycle.viewmodel.compose`, not
  `androidx.hilt.navigation.compose`.
- **Auto-mirrored icons**: `Icons.AutoMirrored.Rounded.{Sort, QueueMusic, TrendingUp, ArrowBack,
  PlaylistAdd, PlaylistPlay}` — the non-mirrored ones are deprecated.
- **Media3 1.11** deprecated the 2-arg `onPlaybackResumption`; the 3-arg overload with
  `isForPlayback` is the one to override.
- **Release signing** comes from `keystore.properties` (git-ignored, not yet created). Without
  it the release build falls back to the debug key *and* appends `-debugsigned` to the version
  name, so such a build identifies itself: publishing one would permanently lock those users out
  of properly signed updates.

---

## 6. Next up (rough order)

*(Walking every screen is done — 2026-09-01. See "Verified on real hardware".)*

0. **Before the repository goes public**: create a real release keystore and back it up (losing
   it means never updating the app again); take screenshots for the README with a *neutral*
   library — the user's own library is one downloaded film track whose cover art is copyrighted
   and whose file name carries a piracy-site name, so it cannot be published; and watch the first
   CI run, since `.github/workflows/build.yml` has never executed (AGP 9.3.2 with compileSdk 37.1
   may need an explicit SDK package on the runner).

1. **Test the paths a walkthrough cannot reach**: lock-screen and Bluetooth transport controls,
   pause on headphone disconnect, the sleep timer actually firing, and "resume last session"
   across a cold start.
2. **Compose UI tests** for the flows worth locking down (song row → playback, playlist creation,
   permission gate) using the instrumentation APK that already builds.
3. **More unit tests**: `MediaStoreScanner` reconciliation and Room migrations with
   `room-testing` — both need Robolectric or instrumentation, which is why they are still open.
   (`SongsViewModel` debounce, `SettingsViewModel`, `PlayerViewModel` session restore and
   `FolderDetailsViewModel` are done.)
4. **Polish**: playlist drag-reorder (queue reorder currently uses up/down buttons on purpose),
   track numbers in album detail, a signing config for release, app icon review.

---

## 7. Session log

**2026-09-01 (session 4b)** — prepared the project for open source at the user's request: `git
init` (there had been no version control at all), `.gitignore`, GPL-3.0 licence, README,
CONTRIBUTING, a GitHub Actions workflow, keystore-driven release signing with a self-identifying
debug-signed fallback, version 1.0 → 0.1.0, and licence/disclaimer paragraphs in the About screen.
The "Moto" name was kept after the trademark risk was raised — the user's call — so a
"not affiliated with Motorola or Lenovo" line goes in both the README and the app.

**2026-09-01 (session 4)** — smoothness, fades, and getting WhatsApp out of the library.
Deleted the 11 generated test tracks from the phone. Fixed the two real sources of jank: the
position tick was being unwrapped in `MotoApp`, recomposing the whole shell and the nav host
four times a second, and the seek-bar drag was recomposing all of `PlayerScreen` — artwork
included — on every frame of the drag; both now read the position through a `State` inside the
one composable that draws it, and `PlaybackConnection` stopped republishing the whole session on
every player event. Added `FadingPlayer`, a `ForwardingPlayer` that ramps the volume up on play
and down on pause so every route into playback fades, not just the in-app buttons. Added two
settings: "Fade in and out" and "Hide voice notes and recordings" (both default on; flipping the
second rescans the library), then — once the folder rules turned out to miss a recorder file
copied into `Download/` and a call recording trimmed into another app's folder — a per-file
"Hide from library" action with a restore dialog in Settings. Signed release with the debug key, which is what finally showed
that most of the "frame drops" were the debug build itself — 64.9% janky frames versus 11.5%
for the same code released. Tests 49 → 55, lint clean (its only warning is that AGP 9.4.0 now
exists).

**2026-09-01 (session 3)** — first run on real hardware, then a full walkthrough. Abandoned the
emulator (see below), installed on a moto g54 5G, pushed the generated test library and walked
every screen. Four bugs fixed: Folders unreachable after any play, light-theme status bar icons
invisible, artwork cache-key collision for album-less files, and both playlist dialogs opening
with the keyboard down. Test count 24 → 49 (`SongsViewModel` debounce, `PlayerViewModel` session
restore, `SettingsViewModel`, `FolderDetailsViewModel`, plus `FakeSongRepository` and
`FakeSettingsRepository`). Lint 0 throughout. One failed experiment recorded in the gotchas: an
attempt to silence Media3's artwork warnings, reverted after instrumenting proved it unreachable.

**2026-08-31 (session 2)** — reconstructed state after context loss, then wrote the whole
presentation layer that was missing: Search, Albums, Artists, Playlists, Folders + their detail
screens, the shared `CollectionScreen`/`CollectionHeader`, the four-in-one `SongCollectionScreen`,
the full Player and Queue, the song menu / add-to-playlist / info / sleep-timer sheets, Settings
and About, then `MotoApp` + `MotoNavHost` + `MainActivity` wiring (it had been a placeholder
`Text("Moto Music")`). Extended `PlayerViewModel` with playlist actions, sleep-timer sheet state
and the playlist-scoped menu. Added the unit-test source set (24 tests), `HiltTestRunner`, the
missing `hilt-android-testing` dependency and `proguard-rules.pro`. Reinstalled the Android SDK,
then cleared every compiler deprecation and lint finding.

**2026-08-31 (session 1)** — data, playback, DI, theme, shared components, Home, Songs and
Permission screens.
