package com.recalldeck.app.importer

/**
 * Outcome of parsing an imported file. Parsers never throw for bad input;
 * they return [Failure] with a user-friendly message instead.
 */
sealed class ImportResult {
    data class Success(
        val cards: List<ParsedCard>,
        /** The preset that produced the cards (useful when auto-detected). */
        val presetUsed: ParserPreset? = null,
    ) : ImportResult()

    data class Failure(val message: String) : ImportResult()
}
