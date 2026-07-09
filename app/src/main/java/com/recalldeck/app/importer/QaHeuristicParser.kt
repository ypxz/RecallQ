package com.recalldeck.app.importer

/** Parsing strategies for plain text extracted from PDF/TXT files. */
enum class ParserPreset {
    /** `Q: ...` line(s) followed by `A: ...` line(s). */
    QA_PAIRS,

    /** Numbered questions like `1. What is X?` or `1) What is X?` followed by answer lines. */
    NUMBERED,

    /** A line ending in `?` followed by an answer block until the next question. */
    QUESTION_MARK,

    /** `Term - Definition` on a single line (also accepts en/em dashes). */
    TERM_DEFINITION,
}

/**
 * Heuristic question/answer parser for plain text. Pass an explicit [ParserPreset]
 * or leave it null to auto-detect the preset that yields the best result.
 */
object QaHeuristicParser {

    fun parse(text: String, preset: ParserPreset? = null): ImportResult {
        if (text.isBlank()) {
            return ImportResult.Failure("The file contains no readable text.")
        }
        return if (preset != null) {
            val cards = parseWithPreset(text, preset)
            if (cards.isEmpty()) {
                ImportResult.Failure(
                    "No cards could be found using the \"${presetLabel(preset)}\" format. " +
                        "Try a different preset."
                )
            } else {
                ImportResult.Success(cards, preset)
            }
        } else {
            autoDetect(text)
        }
    }

    private fun autoDetect(text: String): ImportResult {
        val best = ParserPreset.entries
            .map { it to parseWithPreset(text, it) }
            .maxByOrNull { (preset, cards) -> score(preset, cards) }

        return if (best == null || best.second.isEmpty()) {
            ImportResult.Failure(
                "Couldn't find any question/answer pairs in this file. " +
                    "Supported formats: Q:/A: pairs, numbered questions, " +
                    "questions ending in \"?\", and \"Term - Definition\" lines."
            )
        } else {
            ImportResult.Success(best.second, best.first)
        }
    }

    /**
     * Score a preset's output for auto-detection. More cards is better; the more
     * structured presets win ties over the looser QUESTION_MARK/TERM_DEFINITION ones.
     */
    private fun score(preset: ParserPreset, cards: List<ParsedCard>): Double {
        if (cards.isEmpty()) return 0.0
        val specificity = when (preset) {
            ParserPreset.QA_PAIRS -> 1.0
            ParserPreset.NUMBERED -> 0.9
            ParserPreset.QUESTION_MARK -> 0.6
            ParserPreset.TERM_DEFINITION -> 0.5
        }
        return cards.size + specificity
    }

    private fun parseWithPreset(text: String, preset: ParserPreset): List<ParsedCard> =
        when (preset) {
            ParserPreset.QA_PAIRS -> parseQaPairs(text)
            ParserPreset.NUMBERED -> parseNumbered(text)
            ParserPreset.QUESTION_MARK -> parseQuestionMark(text)
            ParserPreset.TERM_DEFINITION -> parseTermDefinition(text)
        }

    private val qaQuestionRegex = Regex("""^\s*Q\s*[:.]\s*(.*)$""", RegexOption.IGNORE_CASE)
    private val qaAnswerRegex = Regex("""^\s*A\s*[:.]\s*(.*)$""", RegexOption.IGNORE_CASE)

    private fun parseQaPairs(text: String): List<ParsedCard> {
        val cards = mutableListOf<ParsedCard>()
        var question: StringBuilder? = null
        var answer: StringBuilder? = null
        var questionLine = 0

        fun flush() {
            val q = question?.toString()?.trim()
            val a = answer?.toString()?.trim()
            if (!q.isNullOrBlank() && !a.isNullOrBlank()) {
                cards += ParsedCard(q, a, sourceHint = "line $questionLine")
            }
            question = null
            answer = null
        }

        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            val qMatch = qaQuestionRegex.matchEntire(line)
            val aMatch = qaAnswerRegex.matchEntire(line)
            when {
                qMatch != null -> {
                    flush()
                    question = StringBuilder(qMatch.groupValues[1].trim())
                    questionLine = index + 1
                }
                aMatch != null && question != null -> {
                    answer = StringBuilder(aMatch.groupValues[1].trim())
                }
                line.isNotEmpty() && answer != null -> {
                    answer!!.append('\n').append(line)
                }
                line.isNotEmpty() && question != null && answer == null -> {
                    question!!.append('\n').append(line)
                }
                else -> {}
            }
        }
        flush()
        return cards
    }

    private val numberedRegex = Regex("""^\s*(\d{1,4})\s*[.)]\s+(.+)$""")

    private fun parseNumbered(text: String): List<ParsedCard> {
        val cards = mutableListOf<ParsedCard>()
        var question: String? = null
        var questionLine = 0
        val answer = StringBuilder()

        fun flush() {
            val q = question
            val a = answer.toString().trim()
            if (!q.isNullOrBlank() && a.isNotBlank()) {
                cards += ParsedCard(q, a, sourceHint = "line $questionLine")
            }
            question = null
            answer.clear()
        }

        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            val match = numberedRegex.matchEntire(line)
            if (match != null) {
                flush()
                question = match.groupValues[2].trim()
                questionLine = index + 1
            } else if (line.isNotEmpty() && question != null) {
                if (answer.isNotEmpty()) answer.append('\n')
                answer.append(line)
            }
        }
        flush()
        return cards
    }

    private fun parseQuestionMark(text: String): List<ParsedCard> {
        val cards = mutableListOf<ParsedCard>()
        var question: String? = null
        var questionLine = 0
        val answer = StringBuilder()

        fun flush() {
            val q = question
            val a = answer.toString().trim()
            if (!q.isNullOrBlank() && a.isNotBlank()) {
                cards += ParsedCard(q, a, sourceHint = "line $questionLine")
            }
            question = null
            answer.clear()
        }

        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.endsWith("?")) {
                flush()
                question = line
                questionLine = index + 1
            } else if (line.isNotEmpty() && question != null) {
                if (answer.isNotEmpty()) answer.append('\n')
                answer.append(line)
            }
        }
        flush()
        return cards
    }

    private val termDefinitionRegex = Regex("""^\s*(.+?)\s+[-–—]\s+(.+)$""")

    private fun parseTermDefinition(text: String): List<ParsedCard> {
        val cards = mutableListOf<ParsedCard>()
        text.lineSequence().forEachIndexed { index, raw ->
            val line = raw.trim()
            if (line.isEmpty()) return@forEachIndexed
            val match = termDefinitionRegex.matchEntire(line) ?: return@forEachIndexed
            val term = match.groupValues[1].trim()
            val definition = match.groupValues[2].trim()
            if (term.isNotEmpty() && definition.isNotEmpty()) {
                cards += ParsedCard(term, definition, sourceHint = "line ${index + 1}")
            }
        }
        return cards
    }

    private fun presetLabel(preset: ParserPreset): String = when (preset) {
        ParserPreset.QA_PAIRS -> "Q:/A: pairs"
        ParserPreset.NUMBERED -> "Numbered questions"
        ParserPreset.QUESTION_MARK -> "Questions ending in ?"
        ParserPreset.TERM_DEFINITION -> "Term - Definition"
    }
}
