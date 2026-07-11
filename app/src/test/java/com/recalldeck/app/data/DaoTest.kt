package com.recalldeck.app.data

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class DaoTest {

    private lateinit var db: RecallDeckDatabase

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDeckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        db.close()
    }

    private suspend fun seedSubject(name: String = "Biology"): Long =
        db.subjectDao().insert(
            SubjectEntity(name = name, colorHex = "#FF0000", position = 0, createdAt = 1L),
        )

    private suspend fun seedCategory(subjectId: Long, name: String = "Cells"): Long =
        db.categoryDao().insert(
            CategoryEntity(subjectId = subjectId, name = name, position = 0, createdAt = 1L),
        )

    private fun card(
        categoryId: Long,
        state: CardState = CardState.NEW,
        dueAt: Long = 0L,
        question: String = "Q",
    ) = CardEntity(
        categoryId = categoryId,
        type = CardType.BASIC,
        question = question,
        answer = "A",
        state = state,
        dueAt = dueAt,
        createdAt = 1L,
        updatedAt = 1L,
    )

    @Test
    fun subjectCrudAndOrdering() = runTest {
        db.subjectDao().insert(SubjectEntity(name = "B", colorHex = "#fff", position = 1, createdAt = 1))
        db.subjectDao().insert(SubjectEntity(name = "A", colorHex = "#fff", position = 0, createdAt = 2))
        val all = db.subjectDao().getAll()
        assertEquals(listOf("A", "B"), all.map { it.name })
        assertEquals(2, db.subjectDao().nextPosition())
    }

    @Test
    fun deletingSubjectCascadesToCategoriesAndCards() = runTest {
        val subjectId = seedSubject()
        val categoryId = seedCategory(subjectId)
        val cardId = db.cardDao().insert(card(categoryId))

        db.subjectDao().delete(db.subjectDao().getById(subjectId)!!)

        assertNull(db.categoryDao().getById(categoryId))
        assertNull(db.cardDao().getById(cardId))
    }

    @Test
    fun deletingCategoryCascadesToCards() = runTest {
        val subjectId = seedSubject()
        val categoryId = seedCategory(subjectId)
        val keptCategoryId = seedCategory(subjectId, "Kept")
        val cardId = db.cardDao().insert(card(categoryId))
        val keptCardId = db.cardDao().insert(card(keptCategoryId))

        db.categoryDao().delete(db.categoryDao().getById(categoryId)!!)

        assertNull(db.cardDao().getById(cardId))
        assertNotNull(db.subjectDao().getById(subjectId))
        assertNotNull(db.cardDao().getById(keptCardId))
    }

    @Test
    fun updatingSubjectNameAndColor() = runTest {
        val subjectId = seedSubject()
        val subject = db.subjectDao().getById(subjectId)!!

        db.subjectDao().update(subject.copy(name = "Chemistry", colorHex = "#29B6F6"))

        val updated = db.subjectDao().getById(subjectId)!!
        assertEquals("Chemistry", updated.name)
        assertEquals("#29B6F6", updated.colorHex)
    }

    @Test
    fun deletingCardCascadesToReviewLogs() = runTest {
        val categoryId = seedCategory(seedSubject())
        val cardId = db.cardDao().insert(card(categoryId))
        db.reviewLogDao().insert(
            ReviewLogEntity(
                cardId = cardId,
                reviewedAt = 10L,
                rating = 3,
                stateBefore = CardState.NEW,
                elapsedDays = 0.0,
                scheduledDays = 1.0,
                durationMs = 500,
            ),
        )
        db.cardDao().deleteByIds(listOf(cardId))
        assertTrue(db.reviewLogDao().getByCard(cardId).isEmpty())
    }

    @Test
    fun dueQueryReturnsOnlyLearningAndReviewDueCards() = runTest {
        val categoryId = seedCategory(seedSubject())
        db.cardDao().insert(card(categoryId, state = CardState.NEW, dueAt = 0))
        db.cardDao().insert(card(categoryId, state = CardState.LEARNING, dueAt = 50, question = "due-learning"))
        db.cardDao().insert(card(categoryId, state = CardState.REVIEW, dueAt = 100, question = "due-review"))
        db.cardDao().insert(card(categoryId, state = CardState.REVIEW, dueAt = 500, question = "future"))
        db.cardDao().insert(card(categoryId, state = CardState.SUSPENDED, dueAt = 0, question = "suspended"))

        val due = db.cardDao().getDue(now = 100)
        assertEquals(setOf("due-learning", "due-review"), due.map { it.question }.toSet())
        assertEquals(2, db.cardDao().observeDueCount(100).first())
    }

    @Test
    fun newCardLimitRespected() = runTest {
        val categoryId = seedCategory(seedSubject())
        repeat(5) { db.cardDao().insert(card(categoryId, question = "new-$it")) }
        assertEquals(3, db.cardDao().getNew(3).size)
    }

    @Test
    fun bulkStateUpdateAndMove() = runTest {
        val subjectId = seedSubject()
        val cat1 = seedCategory(subjectId, "One")
        val cat2 = seedCategory(subjectId, "Two")
        val id1 = db.cardDao().insert(card(cat1))
        val id2 = db.cardDao().insert(card(cat1))

        db.cardDao().updateState(listOf(id1, id2), CardState.SUSPENDED, updatedAt = 99)
        assertEquals(CardState.SUSPENDED, db.cardDao().getById(id1)!!.state)

        db.cardDao().moveToCategory(listOf(id1), cat2, updatedAt = 100)
        assertEquals(cat2, db.cardDao().getById(id1)!!.categoryId)
        assertEquals(cat1, db.cardDao().getById(id2)!!.categoryId)
    }

    @Test
    fun groupIdSequenceAndLookup() = runTest {
        val categoryId = seedCategory(seedSubject())
        val groupId = db.cardDao().nextGroupId()
        assertEquals(1L, groupId)
        db.cardDao().insertAll(
            listOf(
                card(categoryId, question = "c1").copy(groupId = groupId, type = CardType.CLOZE, clozeIndex = 1),
                card(categoryId, question = "c2").copy(groupId = groupId, type = CardType.CLOZE, clozeIndex = 2),
            ),
        )
        assertEquals(2, db.cardDao().getByGroup(groupId).size)
        assertEquals(2L, db.cardDao().nextGroupId())
    }

    @Test
    fun reviewLogQueriesBySinceAndCard() = runTest {
        val categoryId = seedCategory(seedSubject())
        val cardId = db.cardDao().insert(card(categoryId))
        fun log(at: Long, counted: Boolean = true) = ReviewLogEntity(
            cardId = cardId,
            reviewedAt = at,
            rating = 3,
            stateBefore = CardState.REVIEW,
            elapsedDays = 1.0,
            scheduledDays = 2.0,
            durationMs = 100,
            countedTowardSchedule = counted,
        )
        db.reviewLogDao().insert(log(10))
        db.reviewLogDao().insert(log(20, counted = false))
        db.reviewLogDao().insert(log(30))

        assertEquals(2, db.reviewLogDao().getSince(20).size)
        assertEquals(3, db.reviewLogDao().getByCard(cardId).size)
        val notCounted = db.reviewLogDao().getAll().first { !it.countedTowardSchedule }
        assertNotNull(notCounted)
        assertEquals(20L, notCounted.reviewedAt)
    }

    @Test
    fun undoDeletesReviewLogById() = runTest {
        val categoryId = seedCategory(seedSubject())
        val cardId = db.cardDao().insert(card(categoryId))
        val logId = db.reviewLogDao().insert(
            ReviewLogEntity(
                cardId = cardId,
                reviewedAt = 10L,
                rating = 1,
                stateBefore = CardState.NEW,
                elapsedDays = 0.0,
                scheduledDays = 0.0,
                durationMs = 100,
            ),
        )
        db.reviewLogDao().deleteById(logId)
        assertTrue(db.reviewLogDao().getByCard(cardId).isEmpty())
    }
}
