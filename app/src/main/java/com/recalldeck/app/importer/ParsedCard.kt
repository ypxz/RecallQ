package com.recalldeck.app.importer

/**
 * A single question/answer pair produced by an import parser, shown on the
 * editable preview screen before being saved as a real card.
 */
data class ParsedCard(
    val question: String,
    val answer: String,
    /** Optional longer explanation, saved to the card's `elaboration` field. */
    val elaboration: String? = null,
    val enabled: Boolean = true,
    /** Optional hint about where in the source this card came from (e.g. line number). */
    val sourceHint: String? = null,
)
