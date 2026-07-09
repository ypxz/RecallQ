package com.recalldeck.app.srs

/**
 * Utilities for cloze-deletion text using `{{c1::hidden text}}` syntax. A single text may
 * contain multiple indices; each index becomes its own card row sharing a groupId.
 */
object Cloze {

    private val PATTERN = Regex("""\{\{c(\d+)::(.*?)\}\}""")

    /** Distinct cloze indices present in [text], sorted ascending. */
    fun indices(text: String): List<Int> =
        PATTERN.findAll(text).map { it.groupValues[1].toInt() }.distinct().sorted().toList()

    /** True if [text] contains at least one valid cloze deletion. */
    fun isValid(text: String): Boolean = PATTERN.containsMatchIn(text)

    /**
     * Question side for cloze [index]: the target cloze is masked with `[...]`, all other
     * clozes are revealed.
     */
    fun renderQuestion(text: String, index: Int): String =
        PATTERN.replace(text) { m ->
            if (m.groupValues[1].toInt() == index) "[...]" else m.groupValues[2]
        }

    /** Answer side: every cloze revealed. */
    fun renderAnswer(text: String): String =
        PATTERN.replace(text) { m -> m.groupValues[2] }

    /** The hidden texts for cloze [index], joined with ", " (for type-answer comparison). */
    fun answerFor(text: String, index: Int): String =
        PATTERN.findAll(text)
            .filter { it.groupValues[1].toInt() == index }
            .joinToString(", ") { it.groupValues[2] }
}
