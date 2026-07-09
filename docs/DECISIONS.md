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
