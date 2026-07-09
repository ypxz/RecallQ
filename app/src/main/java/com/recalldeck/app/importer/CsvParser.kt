package com.recalldeck.app.importer

/**
 * Parser for `question;answer` CSV files. Supports `;` and `,` delimiters
 * (auto-detected or explicit), RFC-4180 style double-quote quoting with
 * escaped quotes (`""`), and an optional header row.
 */
object CsvParser {

    fun parse(text: String, delimiter: Char? = null): ImportResult {
        if (text.isBlank()) {
            return ImportResult.Failure("The file contains no readable text.")
        }
        val delim = delimiter ?: detectDelimiter(text)
            ?: return ImportResult.Failure(
                "Couldn't detect a CSV delimiter. Use \";\" or \",\" between question and answer."
            )

        val rows = try {
            parseRows(text, delim)
        } catch (e: MalformedCsvException) {
            return ImportResult.Failure(e.message ?: "The CSV file is malformed.")
        }

        val cards = mutableListOf<ParsedCard>()
        rows.forEachIndexed { index, row ->
            if (row.all { it.isBlank() }) return@forEachIndexed
            if (row.size < 2) {
                if (index == 0) return@forEachIndexed // tolerate an odd header line
                return@forEachIndexed
            }
            val question = row[0].trim()
            val answer = row[1].trim()
            if (question.isEmpty() || answer.isEmpty()) return@forEachIndexed
            if (index == 0 && isHeader(question, answer)) return@forEachIndexed
            cards += ParsedCard(question, answer, sourceHint = "row ${index + 1}")
        }

        return if (cards.isEmpty()) {
            ImportResult.Failure(
                "No question/answer rows found. Expected one \"question${delim}answer\" pair per line."
            )
        } else {
            ImportResult.Success(cards)
        }
    }

    private fun detectDelimiter(text: String): Char? {
        val firstLines = text.lineSequence().filter { it.isNotBlank() }.take(10).toList()
        if (firstLines.isEmpty()) return null
        val semicolons = firstLines.count { countUnquoted(it, ';') > 0 }
        val commas = firstLines.count { countUnquoted(it, ',') > 0 }
        return when {
            semicolons == 0 && commas == 0 -> null
            semicolons >= commas -> ';'
            else -> ','
        }
    }

    private fun countUnquoted(line: String, delim: Char): Int {
        var count = 0
        var inQuotes = false
        for (c in line) {
            when {
                c == '"' -> inQuotes = !inQuotes
                c == delim && !inQuotes -> count++
            }
        }
        return count
    }

    private fun isHeader(question: String, answer: String): Boolean {
        val q = question.lowercase()
        val a = answer.lowercase()
        return (q == "question" || q == "front" || q == "term") &&
            (a == "answer" || a == "back" || a == "definition")
    }

    private class MalformedCsvException(message: String) : Exception(message)

    private fun parseRows(text: String, delim: Char): List<List<String>> {
        val rows = mutableListOf<List<String>>()
        val fields = mutableListOf<String>()
        val field = StringBuilder()
        var inQuotes = false
        var i = 0

        fun endField() {
            fields += field.toString()
            field.clear()
        }

        fun endRow() {
            endField()
            rows += fields.toList()
            fields.clear()
        }

        while (i < text.length) {
            val c = text[i]
            when {
                inQuotes -> when {
                    c == '"' && i + 1 < text.length && text[i + 1] == '"' -> {
                        field.append('"')
                        i++
                    }
                    c == '"' -> inQuotes = false
                    else -> field.append(c)
                }
                c == '"' && field.isBlank() -> {
                    field.clear()
                    inQuotes = true
                }
                c == delim -> endField()
                c == '\r' -> if (i + 1 >= text.length || text[i + 1] != '\n') endRow()
                c == '\n' -> endRow()
                else -> field.append(c)
            }
            i++
        }
        if (inQuotes) {
            throw MalformedCsvException("The CSV file has an unclosed quote.")
        }
        if (field.isNotEmpty() || fields.isNotEmpty()) endRow()
        return rows
    }
}
