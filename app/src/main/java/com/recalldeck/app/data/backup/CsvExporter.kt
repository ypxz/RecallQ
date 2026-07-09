package com.recalldeck.app.data.backup

import com.recalldeck.app.data.db.CardDao
import com.recalldeck.app.data.db.CardEntity

/**
 * Exports the cards of a single subject as CSV with `question;answer` rows,
 * matching the import preset of the same format.
 */
class CsvExporter(private val cardDao: CardDao) {

    suspend fun exportSubject(subjectId: Long): String =
        toCsv(cardDao.getBySubject(subjectId))

    fun toCsv(cards: List<CardEntity>): String =
        buildString {
            append("question;answer\n")
            for (card in cards) {
                append(escape(card.question))
                append(';')
                append(escape(card.answer))
                append('\n')
            }
        }

    private fun escape(field: String): String =
        if (field.any { it == ';' || it == '"' || it == '\n' || it == '\r' }) {
            "\"" + field.replace("\"", "\"\"") + "\""
        } else {
            field
        }
}
