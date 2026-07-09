# RecallDeck — Agent Rules

RecallDeck is a fully offline native Android flashcard app (no accounts, no cloud sync, no
analytics, NO internet permission). English UI. Full spec: `docs/SPEC.md`. Ambiguity decisions
go in `docs/DECISIONS.md` — make the simplest reasonable choice, never block.

## Tech stack (fixed — do not substitute)
- Kotlin 2.0.x, AGP 8.7+, Gradle wrapper, JDK 17
- compileSdk 35, minSdk 26, targetSdk 35, single `:app` module, package `com.recalldeck.app`
- Jetpack Compose (stable BOM) + Material 3, Navigation Compose
- Room (KSP) + DataStore Preferences, WorkManager, kotlinx-serialization-json
- PdfBox-Android (`com.tom-roush:pdfbox-android:2.0.27.0`) for PDF text, Coil for images
- MVVM: Compose screen -> ViewModel (StateFlow) -> Repository -> Room DAO.
  Manual DI via `AppContainer` in the Application class. NO Hilt, NO multi-module,
  NO extra libraries without a one-line justification in the PR description.
- Tests: JUnit4 unit tests; Paparazzi (`app.cash.paparazzi`) for JVM screenshot tests.

## Data model (Room; frozen — extend only, never rename)
See `docs/SPEC.md` for the full schema: `subjects`, `categories`, `cards`, `review_logs`.
Card images are copied to `filesDir/images/` (store relative path).

## Build & verify (no emulator)
```
./gradlew testDebugUnitTest assembleDebug verifyPaparazziDebug
```
Record golden screenshots with `./gradlew recordPaparazziDebug`. Never claim UI works without
attaching Paparazzi PNGs to the PR.

## Hard rules
- main stays green: every PR must pass CI before merge.
- Tests: extend, never weaken or delete.
- Small focused PRs with a checklist of what was verified; attach Paparazzi PNGs for UI changes.
- No cloud sync, accounts, analytics, LLM calls, OCR, or any network permission.
- Scheduling uses FSRS-6 (port of MIT-licensed open-spaced-repetition/FSRS-Kotlin in
  `srs/Fsrs.kt`, unit-tested against its vectors).
- Cram mode logs reviews with `countedTowardSchedule=false` and never mutates FSRS state.
