package com.recalldeck.app.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.RecallDeckDatabase
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.db.SubjectEntity
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BackupManagerTest {

    private lateinit var db: RecallDeckDatabase
    private lateinit var backupManager: BackupManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDeckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        backupManager = BackupManager(db)
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedFullDeck(): Triple<Long, Long, Long> {
        val subjectId = db.subjectDao().insert(
            SubjectEntity(name = "Biology", colorHex = "#00FF00", position = 0, createdAt = 100L),
        )
        val categoryId = db.categoryDao().insert(
            CategoryEntity(subjectId = subjectId, name = "Cells", position = 0, createdAt = 101L),
        )
        val cardId = db.cardDao().insert(
            CardEntity(
                categoryId = categoryId,
                groupId = 7L,
                type = CardType.CLOZE,
                clozeIndex = 2,
                question = "The {{c2::mitochondria}} is the powerhouse",
                answer = "mitochondria",
                hint = "organelle",
                mnemonic = "mighty",
                elaboration = "produces ATP",
                imagePath = "images/cell.png",
                state = CardState.REVIEW,
                dueAt = 1_700_000_000_000L,
                stability = 12.345,
                difficulty = 6.789,
                reps = 9,
                lapses = 2,
                lastReviewAt = 1_690_000_000_000L,
                createdAt = 102L,
                updatedAt = 103L,
            ),
        )
        db.reviewLogDao().insert(
            ReviewLogEntity(
                cardId = cardId,
                reviewedAt = 1_690_000_000_000L,
                rating = 3,
                stateBefore = CardState.LEARNING,
                elapsedDays = 1.5,
                scheduledDays = 3.25,
                durationMs = 4200,
                countedTowardSchedule = false,
            ),
        )
        return Triple(subjectId, categoryId, cardId)
    }

    @Test
    fun roundTripPreservesAllTablesAndSchedulingState() = runTest {
        seedFullDeck()
        val exported = backupManager.exportToJson(now = 999L)

        val originalSubjects = db.subjectDao().getAll()
        val originalCategories = db.categoryDao().getAll()
        val originalCards = db.cardDao().getAll()
        val originalLogs = db.reviewLogDao().getAll()

        // Wipe and restore into the same database.
        backupManager.restoreFromJson(exported)

        assertEquals(originalSubjects, db.subjectDao().getAll())
        assertEquals(originalCategories, db.categoryDao().getAll())
        assertEquals(originalCards, db.cardDao().getAll())
        assertEquals(originalLogs, db.reviewLogDao().getAll())

        val card = db.cardDao().getAll().single()
        assertEquals(12.345, card.stability, 0.0)
        assertEquals(6.789, card.difficulty, 0.0)
        assertEquals(1_700_000_000_000L, card.dueAt)
        assertEquals(CardState.REVIEW, card.state)
        assertEquals(9, card.reps)
        assertEquals(2, card.lapses)
        assertEquals(1_690_000_000_000L, card.lastReviewAt)
    }

    @Test
    fun restoreReplacesExistingData() = runTest {
        seedFullDeck()
        val exported = backupManager.exportToJson()

        // Add extra data that must be gone after restore.
        val extraSubjectId = db.subjectDao().insert(
            SubjectEntity(name = "Extra", colorHex = "#FFFFFF", position = 1, createdAt = 200L),
        )
        val extraCategoryId = db.categoryDao().insert(
            CategoryEntity(subjectId = extraSubjectId, name = "Extra cat", position = 0, createdAt = 201L),
        )
        db.cardDao().insert(
            CardEntity(
                categoryId = extraCategoryId,
                type = CardType.BASIC,
                question = "extra?",
                answer = "extra",
                dueAt = 0L,
                createdAt = 202L,
                updatedAt = 202L,
            ),
        )

        backupManager.restoreFromJson(exported)

        assertEquals(listOf("Biology"), db.subjectDao().getAll().map { it.name })
        assertEquals(1, db.categoryDao().getAll().size)
        assertEquals(1, db.cardDao().getAll().size)
        assertEquals(1, db.reviewLogDao().getAll().size)
    }

    @Test
    fun exportContainsSchemaVersion() = runTest {
        seedFullDeck()
        val exported = backupManager.exportToJson()
        assertTrue(exported.contains("\"schemaVersion\": $BACKUP_SCHEMA_VERSION"))
    }

    @Test
    fun restoreEmptyBackupClearsDatabase() = runTest {
        val exportedEmpty = backupManager.exportToJson()
        seedFullDeck()
        backupManager.restoreFromJson(exportedEmpty)
        assertTrue(db.subjectDao().getAll().isEmpty())
        assertTrue(db.cardDao().getAll().isEmpty())
    }

    @Test
    fun restoreRejectsMalformedJson() = runTest {
        seedFullDeck()
        val thrown = runCatching { backupManager.restoreFromJson("not json at all") }
            .exceptionOrNull()
        assertTrue(thrown is BackupFormatException)
        // Existing data untouched.
        assertEquals(1, db.subjectDao().getAll().size)
    }

    @Test
    fun restoreRejectsNewerSchemaVersion() = runTest {
        val exported = backupManager.exportToJson()
            .replace("\"schemaVersion\": $BACKUP_SCHEMA_VERSION", "\"schemaVersion\": 999")
        val thrown = runCatching { backupManager.restoreFromJson(exported) }
            .exceptionOrNull()
        assertTrue(thrown is BackupFormatException)
    }
}
