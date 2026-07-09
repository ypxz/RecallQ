package com.recalldeck.app.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.RecallDeckDatabase
import com.recalldeck.app.data.db.SubjectEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CsvExporterTest {

    private lateinit var db: RecallDeckDatabase
    private lateinit var exporter: CsvExporter

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDeckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        exporter = CsvExporter(db.cardDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun card(categoryId: Long, question: String, answer: String, createdAt: Long) =
        CardEntity(
            categoryId = categoryId,
            type = CardType.BASIC,
            question = question,
            answer = answer,
            dueAt = 0L,
            createdAt = createdAt,
            updatedAt = createdAt,
        )

    @Test
    fun toCsvProducesHeaderAndSemicolonRows() {
        val csv = exporter.toCsv(
            listOf(
                card(1, "What is DNA?", "Deoxyribonucleic acid", 1),
                card(1, "Cell wall?", "Plants only", 2),
            ),
        )
        assertEquals(
            "question;answer\nWhat is DNA?;Deoxyribonucleic acid\nCell wall?;Plants only\n",
            csv,
        )
    }

    @Test
    fun toCsvEscapesSemicolonsQuotesAndNewlines() {
        val csv = exporter.toCsv(
            listOf(card(1, "a;b", "say \"hi\"\nthere", 1)),
        )
        assertEquals(
            "question;answer\n\"a;b\";\"say \"\"hi\"\"\nthere\"\n",
            csv,
        )
    }

    @Test
    fun exportSubjectOnlyIncludesThatSubjectsCards() = runTest {
        val subjectA = db.subjectDao().insert(
            SubjectEntity(name = "A", colorHex = "#fff", position = 0, createdAt = 1),
        )
        val subjectB = db.subjectDao().insert(
            SubjectEntity(name = "B", colorHex = "#fff", position = 1, createdAt = 2),
        )
        val catA = db.categoryDao().insert(
            CategoryEntity(subjectId = subjectA, name = "cat", position = 0, createdAt = 1),
        )
        val catB = db.categoryDao().insert(
            CategoryEntity(subjectId = subjectB, name = "cat", position = 0, createdAt = 2),
        )
        db.cardDao().insert(card(catA, "qa1", "aa1", 1))
        db.cardDao().insert(card(catA, "qa2", "aa2", 2))
        db.cardDao().insert(card(catB, "qb1", "ab1", 3))

        assertEquals(
            "question;answer\nqa1;aa1\nqa2;aa2\n",
            exporter.exportSubject(subjectA),
        )
    }
}
