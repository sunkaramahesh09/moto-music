## What this changes

<!-- And, more usefully, why. The diff already says what. -->

## How you checked it

<!-- Which screens or flows you actually exercised. If it is a UI change, a screenshot or a
     short screen recording says more than a paragraph. -->

- Tested on: <!-- e.g. moto g54 5G, Android 15 — or "not tested on a device" -->

## Checks

- [ ] `./gradlew :app:compileDebugKotlin` — no new warnings
- [ ] `./gradlew :app:testDebugUnitTest`
- [ ] `./gradlew :app:lintDebug` — still zero issues
- [ ] `./gradlew :app:assembleDebug`

<!-- CI runs all of these on the pull request too, so an unchecked box is not fatal — it just
     means the first person to find out will be the robot. -->

## Anything else

<!-- Trade-offs you weighed, things you were unsure about, follow-up work you deliberately left
     out. Uncertainty is fine to say out loud; it is faster than being guessed at in review. -->
