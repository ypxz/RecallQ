package com.recalldeck.app.srs

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.repo.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class QueueBuilderTest {

    private val now = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    private fun card(
        id: Long,
        categoryId: Long = 1,
        state: CardState = CardState.REVIEW,
        dueAt: Long = now - 1,
        createdAt: Long = id,
        difficulty: Double = 5.0,
        lastReviewAt: Long? = null,
    ) = CardEntity(
        id = id,
        categoryId = categoryId,
        type = CardType.BASIC,
        question = "q$id",
        answer = "a$id",
        state = state,
        dueAt = dueAt,
        stability = 1.0,
        difficulty = difficulty,
        reps = 0,
        lapses = 0,
        lastReviewAt = lastReviewAt,
        createdAt = createdAt,
        updatedAt = 0,
    )

    @Test
    fun `due mode includes only due learning and review cards plus new up to limit`() {
        val cards = listOf(
            card(1, state = CardState.REVIEW, dueAt = now - 1),
            card(2, state = CardState.LEARNING, dueAt = now),
            card(3, state = CardState.REVIEW, dueAt = now + dayMs), // not due
            card(4, state = CardState.SUSPENDED, dueAt = now - 1), // suspended
            card(5, state = CardState.NEW),
            card(6, state = CardState.NEW),
            card(7, state = CardState.NEW),
        )
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.DUE, newLimit = 2), now, Random(1),
        )
        val ids = queue.map { it.id }.toSet()
        assertEquals(4, queue.size)
        assertTrue(ids.containsAll(listOf(1L, 2L)))
        assertTrue(ids.none { it == 3L || it == 4L })
        assertEquals(2, queue.count { it.state == CardState.NEW })
    }

    @Test
    fun `due mode picks oldest new cards first`() {
        val cards = listOf(
            card(10, state = CardState.NEW, createdAt = 300),
            card(11, state = CardState.NEW, createdAt = 100),
            card(12, state = CardState.NEW, createdAt = 200),
        )
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.DUE, newLimit = 2), now, Random(1),
        )
        assertEquals(setOf(11L, 12L), queue.map { it.id }.toSet())
    }

    @Test
    fun `due mode with zero new limit excludes new cards`() {
        val cards = listOf(card(1, state = CardState.NEW), card(2, state = CardState.REVIEW))
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.DUE, newLimit = 0), now, Random(1),
        )
        assertEquals(listOf(2L), queue.map { it.id })
    }

    @Test
    fun `random mode ignores dueAt but excludes suspended`() {
        val cards = listOf(
            card(1, dueAt = now + 100 * dayMs),
            card(2, state = CardState.NEW),
            card(3, state = CardState.SUSPENDED),
        )
        val queue = QueueBuilder.buildQueue(cards, QueueOptions(mode = QueueMode.RANDOM), now, Random(1))
        assertEquals(setOf(1L, 2L), queue.map { it.id }.toSet())
    }

    @Test
    fun `random mode respects count cap`() {
        val cards = (1L..20L).map { card(it) }
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.RANDOM, count = 5), now, Random(1),
        )
        assertEquals(5, queue.size)
    }

    @Test
    fun `custom mode oldest orders by last review ascending with never-reviewed first`() {
        val cards = listOf(
            card(1, lastReviewAt = now - dayMs),
            card(2, lastReviewAt = now - 10 * dayMs),
            card(3, lastReviewAt = null),
        )
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.CUSTOM, order = CustomOrder.OLDEST), now, Random(1),
        )
        assertEquals(listOf(3L, 2L, 1L), queue.map { it.id })
    }

    @Test
    fun `custom mode hardest orders by difficulty descending`() {
        val cards = listOf(
            card(1, difficulty = 3.0),
            card(2, difficulty = 9.0),
            card(3, difficulty = 6.0),
        )
        val queue = QueueBuilder.buildQueue(
            cards, QueueOptions(mode = QueueMode.CUSTOM, order = CustomOrder.HARDEST), now, Random(1),
        )
        assertEquals(listOf(2L, 3L, 1L), queue.map { it.id })
    }

    @Test
    fun `interleaving avoids consecutive same-category cards when possible`() {
        val cards = (1L..6L).map { card(it, categoryId = 1) } +
            (7L..12L).map { card(it, categoryId = 2) }
        val queue = QueueBuilder.interleaveByCategory(cards, Random(42))
        assertEquals(12, queue.size)
        for (i in 1 until queue.size) {
            assertTrue(
                "positions ${i - 1} and $i share category",
                queue[i].categoryId != queue[i - 1].categoryId,
            )
        }
    }

    @Test
    fun `interleaving keeps all cards when one category dominates`() {
        val cards = (1L..5L).map { card(it, categoryId = 1) } + listOf(card(6, categoryId = 2))
        val queue = QueueBuilder.interleaveByCategory(cards, Random(1))
        assertEquals(cards.map { it.id }.toSet(), queue.map { it.id }.toSet())
    }

    @Test
    fun `interleaving is deterministic for a fixed seed`() {
        val cards = (1L..10L).map { card(it, categoryId = it % 3) }
        assertEquals(
            QueueBuilder.interleaveByCategory(cards, Random(7)).map { it.id },
            QueueBuilder.interleaveByCategory(cards, Random(7)).map { it.id },
        )
    }

    @Test
    fun `requeue inserts card about ten positions later`() {
        val queue = (1L..20L).map { card(it) }
        val again = card(99)
        val requeued = QueueBuilder.requeueAgain(queue, again, position = 0)
        assertEquals(21, requeued.size)
        assertEquals(99L, requeued[10].id)
    }

    @Test
    fun `requeue clamps to queue end`() {
        val queue = (1L..3L).map { card(it) }
        val requeued = QueueBuilder.requeueAgain(queue, card(99), position = 2)
        assertEquals(99L, requeued.last().id)
    }

    @Test
    fun `session again re-queues card and undo restores everything`() {
        val settings = AppSettings()
        val queue = (1L..15L).map { card(it, state = CardState.REVIEW, lastReviewAt = now - dayMs) }
        var session = StudySession(queue)
        val first = session.currentCard!!

        val result = Scheduler.grade(first, Grade.AGAIN, now, settings)
        session = session.afterGrade(result, Grade.AGAIN, reviewLogId = 42)

        assertEquals(1, session.position)
        assertEquals(16, session.queue.size)
        assertEquals(first.id, session.queue[11].id) // re-inserted ~10 positions later

        val (restored, undoAction) = session.undo()!!
        assertEquals(0, restored.position)
        assertEquals(15, restored.queue.size)
        assertEquals(first, restored.currentCard)
        assertEquals(first, undoAction.restoreCard)
        assertEquals(42L, undoAction.deleteReviewLogId)
        assertTrue(restored.history.isEmpty())
    }

    @Test
    fun `session good advances without requeue`() {
        val settings = AppSettings()
        val queue = (1L..5L).map { card(it, state = CardState.REVIEW, lastReviewAt = now - dayMs) }
        var session = StudySession(queue)
        val result = Scheduler.grade(session.currentCard!!, Grade.GOOD, now, settings)
        session = session.afterGrade(result, Grade.GOOD, reviewLogId = 1)
        assertEquals(1, session.position)
        assertEquals(5, session.queue.size)
    }

    @Test
    fun `session finishes after grading all cards`() {
        val settings = AppSettings()
        var session = StudySession(listOf(card(1, state = CardState.REVIEW, lastReviewAt = now - dayMs)))
        val result = Scheduler.grade(session.currentCard!!, Grade.GOOD, now, settings)
        session = session.afterGrade(result, Grade.GOOD, reviewLogId = 1)
        assertTrue(session.isFinished)
        assertNull(session.currentCard)
    }

    @Test
    fun `undo on empty history returns null`() {
        assertNull(StudySession(emptyList()).undo())
        assertNotNull(StudySession(listOf(card(1))).currentCard)
    }
}
