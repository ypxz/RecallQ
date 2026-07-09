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
