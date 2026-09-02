# Moto Music — Runbook

A local-only Android music player: it plays audio files already on the device, holds no
INTERNET permission, and keeps playlists, favourites and history in a local Room database.
Published as an open-source project under the GPL-3.0 — see README.md and CONTRIBUTING.md, which
are what the outside world reads; this runbook is the working memory behind them.

Last updated: 2026-09-02 (session 4d).

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

- **only the release build is installed**: `com.motomusic.app`, launcher name "Moto Music",
  version `0.1.0-debugsigned`. The debug build was uninstalled after it was opened by mistake
  twice; reinstall it with `adb install -r -g app/build/outputs/apk/debug/app-debug.apk` only
  when something actually needs it, and expect a burnt-orange icon.
- the generated test tracks are **gone**; the phone has only its own 19 audio files, of which
  the library shows **1** — the WhatsApp voice notes and recorder files are filtered by folder,
  and the two the folder rules could not catch are in the release build's hidden list
  (Settings → Library → "Hidden songs"), which is app-local and per build,
- listening history in the release build from the verification run above,
- theme "Follow system"; media volume still at 5 from the silent testing.

---

## 2c. The public repository

Live at **https://github.com/sunkaramahesh09/moto-music**, public, GPL-3.0, pushed
2026-09-02 (session 4d). `origin` is the HTTPS URL; pushes authenticate through the `gh` CLI
logged in as `sunkaramahesh09`.

**Commit identity is fixed and not negotiable.** `user.name = mahesh`,
`user.email = sunkaramahesh494@gmail.com`. The user's other address
(`victorybazarspvtltd.software@gmail.com`, which is what this machine's tooling reports) must
never appear in a commit — it was explicitly ruled out.

**No assistant attribution in commit messages, ever.** The first four commits shipped with
`Co-Authored-By: Claude` trailers, which made GitHub list "claude" as a second contributor.
The user did not want that, so history was rewritten and force-pushed:

```bash
git filter-branch -f --msg-filter 'sed -e "/^Co-Authored-By: Claude/d" -e "/^Claude-Session:/d"' -- --all
git push --force origin main
```

To stop it recurring there is a **`commit-msg` hook** at `.git/hooks/commit-msg` that strips
those trailers from every message. Hooks live inside `.git/`, so they are *not* committed and
*do not survive a fresh clone* — if this repository is ever re-cloned, write the hook again
before the first commit (it is six lines; the text is in the file itself, and in the session-4d
transcript).

Verification after the rewrite, which is the check to repeat if it is ever in doubt:

```bash
gh api repos/sunkaramahesh09/moto-music/contributors --jq '.[] | "\(.login) \(.contributions)"'
git log --format='%an <%ae>%n%b' origin/main | grep -i claude   # expect no output
git ls-remote --heads --tags origin                             # expect only refs/heads/main
```

All three came back clean — and the Contributors sidebar **still showed "claude" anyway**,
in a fresh incognito window, so it was not a browser or page cache. The reason:

> **A force-push does not delete the old commits.** GitHub keeps unreachable objects and never
> garbage-collects them for you, so every pre-rewrite commit still resolved at its old SHA
> (`https://github.com/…/commit/cbd1e97…` → HTTP 200, still showing the co-author trailer).
> The `/contributors` API and the `/graphs/contributors` page rebuilt correctly, but the repo
> **overview sidebar** reads a different index that still counted those orphans.

The only two real fixes are to delete the repository and push the clean history to a new one
(cheap while nothing depends on it — check first that issues, PRs, releases, forks and stars are
all zero and the tree is in sync), or to ask GitHub Support to garbage-collect the unreachable
objects and rebuild the contributors index. Waiting does not work.

**So: never let the trailer reach GitHub in the first place.** That is what the hook is for, and
why re-creating it after any fresh clone matters.

**CI**: `.github/workflows/build.yml` ran for the first time on the initial push and **passed**
(assemble + unit tests + lint on `ubuntu-latest`). The worry that AGP 9.3.2 with compileSdk 37.1
would need an explicit SDK package on the runner turned out to be unfounded.

**No release exists yet** — see section 6, item 0. Do not cut one from a `-debugsigned` APK.

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
- **Two builds, two settings stores, one launcher icon.** The debug build kept its own
  DataStore, so songs hidden in the release build were still listed in the debug one — and the
  user, opening the wrong app, reasonably reported the hiding as broken. Renaming the debug build
  to "Moto Music (debug)" (`app/src/debug/res/values/strings.xml`) **did not fix it**: people tap
  icons, not names, and it happened a second time. The debug icon now has its own background
  colour (`app/src/debug/res/values/colors.xml`, burnt orange) and the debug build was
  uninstalled from the phone. Lesson: if two builds can be installed side by side, they must be
  distinguishable *at a glance*, and the default should be not to leave both on a user's phone.
- **Screenshots cannot use the user's own library.** Their downloads carry the source site's
  name in the title tags *and burned into the cover art itself* — editing a watermark out of
  copyrighted film artwork so it can be published is not a fix. Tags were cleaned with `mutagen`
  (a genuine improvement to their library), but the covers are why the README ships a wholly
  generated demo library instead. `pm clear` before a screenshot run also wipes the hidden-songs
  list and every setting: re-hide and re-check afterwards.
- **A hidden song must also leave the queue.** Hiding only rewrote the library, so the file the
  user had just hidden carried on playing in the mini player.
  `PlaybackConnection.removeSongFromQueue` now runs first.
- **`gh api -f 'names[]=x'` breaks under zsh.** The brackets glob, and the command dies with
  `(eval):2: no matches found`. Build the JSON and pipe it in with `--input -` instead.
- **A force-push does not remove a commit from GitHub — only from the branch.** The old objects
  stay reachable by SHA indefinitely, which is why rewriting history did not take "claude" off
  the Contributors sidebar even after every API said the repository was clean. Rewriting again
  cannot help; deleting the repository or asking Support to gc it are the only fixes. Corollary
  for anything sensitive: once it is pushed, treat it as published. See section 2c.
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

0. **Create the release keystore, then cut v0.1.0.** This is the one thing blocking a release;
   the repository is public and CI is green. The user runs the `keytool` step themselves:

   ```bash
   keytool -genkeypair -v -keystore ~/moto-music-release.jks -alias moto \
     -keyalg RSA -keysize 2048 -validity 10000
   ```

   Then a `keystore.properties` in the project root (git-ignored) with `storeFile`,
   `storePassword`, `keyAlias`, `keyPassword`; `./gradlew :app:assembleRelease` must then produce
   a version name of plain `0.1.0` with **no `-debugsigned` suffix** — that suffix is the signal
   the APK is unpublishable. **Back the `.jks` and its passwords up somewhere that survives this
   laptop**: losing them means never being able to update the app for anyone who installed it.
   Publish with `gh release create v0.1.0 <apk> --title … --notes …`.

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

**2026-09-02 (session 4d)** — published the repository. Created
`sunkaramahesh09/moto-music` and pushed the four commits; the first CI run passed unchanged.
The user then objected to the `Co-Authored-By: Claude` trailers GitHub was showing as a second
contributor, so history was rewritten with `filter-branch --msg-filter` and force-pushed, and a
local `commit-msg` hook now strips those trailers from every future commit. Verified from the
API that the remote carries one contributor, no trailers and a single ref; the Contributors
sidebar kept showing the old entry for a while because it is cached, not because anything was
left behind. No app code changed this session. See section 2c.

**2026-09-01 (session 4c)** — README screenshots. Cleaned the site branding out of the user's
own tags with `mutagen` (backed up first), then found the same branding burned into the cover
art, so the screenshots were shot against a generated demo library instead: 10 tracks, 3 invented
artists, generated covers, pushed and then removed, with the user's own files parked under a
`.nomedia` folder in the meantime and restored afterwards. Eight screenshots at 360 px in
`docs/screenshots/`, both themes, taken in SystemUI demo mode for a clean status bar.

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
