# RecallDeck — Full Specification

RecallDeck is a native Android flashcard app for university studying. Fully offline: no
accounts, no cloud sync, no analytics, English UI, NO internet permission.

## Tech stack (fixed)
- Kotlin 2.0.x, AGP 8.7+, Gradle wrapper, JDK 17
- compileSdk 35, minSdk 26, targetSdk 35, single `:app` module, package `com.recalldeck.app`
- Jetpack Compose (latest stable BOM) + Material 3, Navigation Compose
- Room (KSP) + DataStore Preferences, WorkManager, kotlinx-serialization-json
- PdfBox-Android (`com.tom-roush:pdfbox-android:2.0.27.0`) for PDF text extraction, Coil for images
- MVVM: Compose screens -> ViewModel (StateFlow) -> Repository -> Room DAO. Manual DI via an
  `AppContainer` in the Application class. NO Hilt, NO multi-module, NO extra libraries without a
  one-line justification in the PR description.
- Tests: JUnit4 unit tests; Paparazzi (`app.cash.paparazzi`) for JVM screenshot tests of key screens.

## Data model (Room; frozen — extend only, never rename)
```
subjects(id PK, name, colorHex, position, createdAt)
categories(id PK, subjectId FK CASCADE, name, position, createdAt)
cards(id PK, categoryId FK CASCADE, groupId?, type[BASIC|CLOZE], clozeIndex?, question, answer,
      hint?, mnemonic?, elaboration?, imagePath?, state[NEW|LEARNING|REVIEW|SUSPENDED], dueAt,
      stability REAL, difficulty REAL, reps, lapses, lastReviewAt?, createdAt, updatedAt)
review_logs(id PK, cardId FK CASCADE, reviewedAt, rating[1-4], stateBefore, elapsedDays REAL,
            scheduledDays REAL, durationMs, countedTowardSchedule BOOL)
```
Indices: `cards(categoryId)`, `cards(state,dueAt)`, `review_logs(cardId)`, `review_logs(reviewedAt)`.
Card images are copied to `filesDir/images/` (store relative path).

## Core behaviors

### Review flow
- Show question only -> "Show answer" reveals answer in large type -> 4 grade buttons, each
  captioned with its predicted next interval:
  Again(=don't know)->FSRS rating 1, Hard(=kind of)->2, Good(=know it)->3, Easy->4.
- "Never ask again" lives in an overflow menu -> sets state SUSPENDED (reversible in browser).
- Again re-inserts the card ~10 positions later in the same session.
- Undo restores the previous card snapshot and deletes the review log.

### Scheduling
- FSRS-6 algorithm (port of the MIT-licensed reference from
  github.com/open-spaced-repetition/FSRS-Kotlin in `srs/Fsrs.kt`; unit-tested against its vectors).
- Due queue = cards with state IN (LEARNING, REVIEW) AND dueAt <= now, plus NEW cards up to a
  daily limit (default 20, setting). Target retention setting default 0.9.
- Queue shuffled with category interleaving.

### Study modes
- Due review (all), scoped (subject/category), random mix (ignores dueAt), custom session
  (scope, count, order random/oldest/hardest, cram toggle).
- Cram mode logs reviews with `countedTowardSchedule=false` and NEVER mutates FSRS state.

### Mastery ("successive relearning")
- 3 consecutive rating >= 3 with scheduledDays >= 21 -> "mastered" badge; auto-suspend only if
  the setting is enabled (default off).

### Cloze
- `{{c1::text}}` syntax; saving a multi-cloze card creates one card row per cloze index sharing
  `groupId`.
- Type-answer mode: normalized comparison + Levenshtein "almost" hint; the user's self-grade is
  always final.

### Import
- User picks PDF/TXT/CSV via SAF -> offline text extraction (PdfBox for PDF) -> heuristic parser
  presets: "Q:/A:" pairs, numbered questions (`1.` / `1)`), lines ending in "?" followed by an
  answer block, "Term - Definition", CSV `question;answer`.
- ALWAYS an editable preview screen (toggle/edit each parsed card, pick target subject+category)
  before saving. Malformed files show a friendly error, never crash. No OCR, no LLM.

### Stats
- Current streak, 12-week review heatmap, 30-day due forecast, retention % (ratings >= 2 vs
  total, last 30 days), per-subject state breakdown. Drawn with Compose Canvas, no chart library.

### Reminder
- WorkManager daily worker at user-set time posts "N cards due" notification (runtime
  POST_NOTIFICATIONS permission on API 33+), deep-links to due review.

### Backup
- Versioned JSON export/import of all tables via SAF (replace-all on import, with confirmation
  dialog); CSV export per subject.

### Settings
- Retention target, new/day limit, reminder time on/off, theme system/light/dark, auto-suspend
  toggle, backup actions.

## Screens (9)
1. **Home** — subject grid + global due count + streak + "Study all due".
2. **Subject detail** — categories + due counts.
3. **Card browser** — search, state filter, multi-select move/suspend/delete.
4. **Card editor** — basic/cloze toggle, optional hint/mnemonic/elaboration/image, live cloze preview.
5. **Study setup** — mode + custom options.
6. **Study** — progress bar, reveal flow, grade buttons with intervals, hint reveal, undo,
   session summary.
7. **Import** — file pick -> preset -> editable preview -> save.
8. **Stats**.
9. **Settings**.

## Verification (no emulator in CI)
`./gradlew testDebugUnitTest assembleDebug verifyPaparazziDebug` must be green; CI uploads the
debug APK as an artifact. Never claim UI works without Paparazzi PNGs.

## Hard rules
- main stays green: every PR passes CI before merge. Tests: extend, never weaken or delete.
- Small focused PRs with a checklist of what was verified and (for UI) Paparazzi PNGs attached.
- No cloud sync, accounts, analytics, LLM calls, or any network permission.
- If a spec detail is ambiguous, make the simplest reasonable choice and record it in
  `docs/DECISIONS.md` instead of blocking.

## Definition of done
- All 9 screens functional; acceptance flow works end to end: create subject -> category ->
  basic + cloze cards -> study due queue -> grade -> Again re-queues in session -> suspend via
  overflow -> import fixture PDF -> preview -> save -> stats reflect reviews -> JSON export ->
  wipe -> import restores everything including scheduling state.
- CI green; FSRS parity tests pass; debug APK downloadable from the latest CI run.
