# DECISIONS

Ambiguous spec details resolved with the simplest reasonable choice.

- **Repo name**: The GitHub repo is `RecallQ`, but the app/product name is **RecallDeck**
  (package `com.recalldeck.app`) per the spec.
- **Importer preset auto-detection**: each preset parses the whole text; the one yielding the
  most cards wins, with structured presets (Q:/A:, numbered) breaking ties over looser ones
  ("?"-lines, Term - Definition). Explicit preset selection is still supported.
- **CSV delimiter**: auto-detected from the first non-blank lines (`;` preferred over `,` on a
  tie, matching the spec's `question;answer` example); RFC-4180 quoting with `""` escapes and
  an optional `question;answer`-style header row are supported.
- **Term - Definition separator**: requires spaces around the dash (`Term - Definition`) so
  hyphenated words aren't split; en/em dashes accepted too.
- **Robolectric test dep**: PdfBox-Android needs an Android `Context`
  (`PDFBoxResourceLoader.init`) even for text extraction, so PDF unit tests run under
  Robolectric + androidx.test:core (test-only dependencies).
- **Backup schema version**: restore accepts files with `schemaVersion <=` the app's current
  version (currently 1) and rejects newer ones with a friendly error; unknown JSON keys are
  ignored for forward tolerance.
- **Backup ids**: JSON backup preserves original row ids so foreign keys and `groupId`
  cloze grouping survive restore verbatim (restore is replace-all, so no id collisions).
- **CSV export**: includes a `question;answer` header row; fields containing `;`, `"` or
  newlines are quoted with `""` escaping (RFC-4180 style with `;` delimiter).
- **Streak definition**: consecutive days with >=1 counted review ending today, or ending
  yesterday if today has no review yet (a streak only breaks after a full missed day).
  Cram reviews (`countedTowardSchedule=false`) never extend the streak.
- **Heatmap**: counts ALL reviews (cram included) — it visualises study activity, while
  streak/retention use counted reviews only per the spec.
- **Due forecast**: next 30 days starting today; overdue LEARNING/REVIEW cards are bucketed
  into today. NEW and SUSPENDED cards are excluded.
- **Retention window**: counted reviews with `reviewedAt` in the last 30 x 24h from `now`
  (rolling window, not calendar days); returns null (shown as "—") when the window is empty.
- **Reminder scheduling**: a unique `PeriodicWorkRequest` (24h period) with an initial delay
  to the next occurrence of the user-set time, enqueued with `ExistingPeriodicWorkPolicy.UPDATE`
  so re-setting the time reschedules cleanly. If no cards are due, no notification is posted.
- **Notification deep link**: the reminder's content intent launches `MainActivity` with the
  boolean extra `com.recalldeck.app.OPEN_DUE_REVIEW`; Phase 3 UI reads this extra to navigate
  to the due review flow (runtime POST_NOTIFICATIONS request is also Phase 3).

- **Configurable learning steps**: the short (sub-day) delays are user-configurable in
  Settings (Again for all states, Hard/Good for NEW, Hard for LEARNING; 1 min–24 h).
  Good/Easy graduation intervals stay FSRS-driven — fixed day intervals there would defeat
  the scheduler. Defaults match the previous fixed values (3/5/10/10 min).

## SRS (srs/ package)

- **FSRS parity vectors**: the reference repo (open-spaced-repetition/FSRS-Kotlin) ships no
  test vectors, so `FsrsTest` uses vectors independently computed from the reference formulas
  with FSRS-6 default parameters (the reference exposes none).
- **Deviations from literal reference code** (documented bugs in the reference, fixed in the
  port): initial stability uses a lower bound (`coerceAtLeast`) instead of the reference's
  `coerceAtMost(0.1)` (which would cap all initial stabilities at 0.1); linear damping uses `(10 - D) / 9`
  (reference has a precedence bug `10 - D/9`); retrievability uses the FSRS-6 power
  forgetting curve `(1 + FACTOR * t / S)^DECAY` (the reference defines DECAY/FACTOR but uses
  a legacy exponential curve); button-interval ordering uses
  `good = max(good, hard + 1)` / `easy = max(easy, good + 1)` (reference uses `min`, which
  would invert the ordering). Two-decimal rounding of stability/difficulty is kept from the
  reference (via `round`, not locale-dependent `String.format`).
- **Interval fuzzing** is deterministic-off by default (fuzz factor is injectable) so
  scheduling is reproducible and testable; the reference seeds a RNG with wall-clock time.
- **Learning steps** mirror the reference: NEW -> Again 3 min / Hard 5 min / Good 10 min /
  Easy 1 day; LEARNING -> Again 3 min / Hard 10 min / Good & Easy graduate with day
  intervals. NEW graded Easy goes straight to REVIEW; LEARNING graduates on Good/Easy;
  REVIEW graded Again lapses to LEARNING (+1 lapse).
- **Cram mode**: `Scheduler.grade(countedTowardSchedule = false)` returns the card unchanged
  and only produces a review log; mastery detection ignores cram logs.
- **Queue scoping** (all / subject / category) is done by the caller passing an
  already-scoped card list; `QueueBuilder` stays a set of pure functions over lists.
- **Category interleaving**: greedy pick from the category with the most remaining cards,
  never repeating the previous category when avoidable; deterministic under a seeded
  `Random`.
- **Again re-queue**: re-inserted 10 positions after the next card (clamped to queue end).
- **Undo**: `StudySession` (immutable) records pre-grade card snapshot + review-log id;
  `undo()` restores queue/position and tells the caller which card to write back and which
  log row to delete.

## Study flow UI (Phase 3)

- **Type-answer "almost" threshold**: Levenshtein edit distance on normalized strings
  (lowercased, punctuation stripped, whitespace collapsed) <= max(1, 20% of the expected
  answer length) counts as ALMOST; the user's self-grade is always final regardless of
  the verdict.
- **Cloze question rendering**: the target cloze index is masked as `[...]`; other cloze
  indices are shown revealed (Anki-style). The answer reveal shows the full text with all
  clozes filled in.
- **Multi-cloze save**: editing an existing cloze card edits just that row; creating a new
  cloze card inserts one row per distinct `cN` index sharing a fresh `groupId`.
- **Auto-suspend on mastery** (setting, default off): checked after each counted REVIEW
  grade using the card's full log history; never triggered in cram mode.
- **Study route args**: study config (scope, mode, count, order, cram, type-answer) is
  passed via navigation query args rather than a shared ViewModel, keeping destinations
  deep-linkable and state restorable.
- **User-facing state names**: scheduling states are shown as "Not studied" (NEW),
  "Kind of know" (LEARNING), "Know" (REVIEW), and "Never ask" (SUSPENDED) so they match the
  grade buttons ("Don't know" / "Kind of" / "Know it" / "Know 100%"). Enum names and the
  DB schema are unchanged.
- **Grade button order**: easiest ("Know 100%") on the left through "Don't know" on the
  right, per user request.
- **Skip in study**: moves the current card to the end of the session queue without grading
  (no scheduling change, not undoable since nothing was persisted).
- **Again-at-session-end** (setting, default off): "Don't know" re-inserts the card at the
  end of the current session queue instead of ~10 positions later; the stored dueAt delay
  is unchanged.
- **In-detail explanation**: after the answer is revealed, a card's elaboration is no
  longer shown automatically; an "In detail" chip appears (only when the elaboration is
  non-blank) and expands the text on tap. Applies identically in cram and type-answer
  modes. The expanded state resets per card. Mnemonic display is unchanged.
- **CSV explanation column**: the CSV import format gains an optional third column
  (`question;answer;explanation`) mapped to the card's existing `elaboration` field; a
  blank third column means no elaboration. Two-column files keep working unchanged.
  CSV export now always writes the three-column header `question;answer;explanation`
  with an empty third field for cards without an elaboration. Extra columns beyond the
  third are still ignored.
