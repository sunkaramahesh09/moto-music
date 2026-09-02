# Contributing

Thanks for looking. This is a small project with a deliberately small surface: an offline music
player that holds no internet permission. Anything that would change either of those two things
needs a conversation first.

## Before you start

Open an issue for anything larger than a fix. It saves you writing code that does not fit, and it
gives other people somewhere to say "I want that too".

## How to send a change

If you have not done this on GitHub before, the whole flow is:

1. **Fork this repository** — the Fork button, top right. You now have your own copy at
   `github.com/<you>/moto-music`, which you can push to freely.

2. **Clone your fork and branch off `main`:**

   ```bash
   git clone https://github.com/<you>/moto-music.git
   cd moto-music
   git checkout -b short-name-for-the-change
   ```

3. **Make the change**, running the checks in the next section as you go.

4. **Commit and push to your fork:**

   ```bash
   git commit -am "Explain why, not what"
   git push -u origin short-name-for-the-change
   ```

5. **Open the pull request.** GitHub offers a "Compare & pull request" button on your fork right
   after you push; the target is `sunkaramahesh09/moto-music`, branch `main`.

CI builds every pull request — unit tests, lint and a debug APK — so nobody has to take your word
for it compiling on a clean machine, and you get a downloadable APK of your own change out of it.
A red cross is not a rejection: open the log, fix what it found, and push again to the same
branch. The pull request updates itself; do not open a second one.

If review takes a while and `main` moves on:

```bash
git remote add upstream https://github.com/sunkaramahesh09/moto-music.git
git fetch upstream
git rebase upstream/main
git push --force-with-lease
```

**You do not need an Android phone to be useful here.** Tests, documentation, translations and
review all happen on a laptop. What is genuinely hardest to get is the opposite — this app has
been exercised on exactly one phone, so "I ran it on a Pixel and here is what broke" is worth
more than most patches.

## Building and checking

```bash
./gradlew :app:compileDebugKotlin    # fastest feedback
./gradlew :app:testDebugUnitTest     # unit tests
./gradlew :app:lintDebug             # lint
./gradlew :app:assembleDebug         # installable APK
```

A pull request is expected to keep all four green, and to keep the build **warning-free**: the
project sits at zero Kotlin warnings and zero lint issues, and the only way that stays true is by
not letting the first one in.

## What the code expects of you

[RUNBOOK.md](RUNBOOK.md) is the real documentation — architecture, the conventions that keep the
screens consistent, and a list of mistakes already made and paid for. Read section 4
("Conventions to keep following") and section 5 ("Gotchas already paid for") before your first
change. The short version:

- **Screens are stateless.** A screen takes a UI state, `contentPadding` and callbacks. ViewModels
  are resolved in `MotoNavHost`, not inside screens.
- **Playback is one object.** `PlayerViewModel` is created once and published through
  `LocalSongActions`; any list can call `LocalSongActions.current.play(songs, index)`.
- **The playing position is never unwrapped high in the tree.** Pass the `State` down and read it
  in the smallest composable that draws it, or you put the whole app on a recomposition loop
  whenever music plays.
- **UI text uses British spelling** ("Favourites"); code identifiers use American ("Favorite").
- **New dependencies need a reason.** Every one is a thing users have to trust. Nothing that talks
  to the network will be merged.

## Tests

Logic that can be tested on the JVM should be: ViewModels, mappers, and pure functions like the
voice-recording matcher. Fakes for the repositories live in `app/src/test/java/…/util/`. Anything
needing a real device (the `MediaStore` scan, Room migrations) is not yet covered — help there is
especially welcome.

## Commits and pull requests

Explain *why* in the commit message; the diff already says what. Keep one concern per pull request.
If you change behaviour a user can see, say how you checked it on a real device — including which
Android version, since this has only been exercised on Android 15 so far.

## Licence

By contributing you agree that your work is licensed under the
[GNU General Public License v3.0](LICENSE), like the rest of the project.
