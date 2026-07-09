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
