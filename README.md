# RecallDeck

A fully offline native Android flashcard app for university studying. No accounts, no cloud
sync, no analytics — the app requires no internet permission.

## Build

Requires JDK 17 and the Android SDK (compileSdk 35, build-tools 35.0.0).

```
./gradlew assembleDebug
```

## Verify

```
./gradlew testDebugUnitTest assembleDebug verifyPaparazziDebug
```

See `docs/SPEC.md` for the full specification and `AGENTS.md` for contributor rules.
