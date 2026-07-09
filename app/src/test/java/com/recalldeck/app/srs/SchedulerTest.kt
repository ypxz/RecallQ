package com.recalldeck.app.srs

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.repo.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SchedulerTest {

    private val settings = AppSettings()
    private val now = 1_700_000_000_000L
    private val dayMs = 86_400_000L

    private fun card(
        state: CardState = CardState.NEW,
        stability: Double = 0.0,
        difficulty: Double = 0.0,
        dueAt: Long = now,
        lastReviewAt: Long? = null,
        reps: Int = 0,
        lapses: Int = 0,
    ) = CardEntity(
        id = 1,
        categoryId = 1,
        type = CardType.BASIC,
        question = "q",
        answer = "a",
        state = state,
        dueAt = dueAt,
        stability = stability,
        difficulty = difficulty,
        reps = reps,
        lapses = lapses,
        lastReviewAt = lastReviewAt,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun `new card graded good enters learning with initial fsrs state`() {
        val result = Scheduler.grade(card(), Grade.GOOD, now, settings)
        val updated = result.updatedCard
        assertEquals(CardState.LEARNING, updated.state)
        assertEquals(2.31, updated.stability, 1e-9)
        assertEquals(2.12, updated.difficulty, 1e-9)
        assertEquals(1, updated.reps)
        assertEquals(0, updated.lapses)
        assertEquals(now, updated.lastReviewAt)
        assertEquals(now + 10 * 60_000L, updated.dueAt)
    }

    @Test
    fun `new card graded easy jumps straight to review`() {
        val result = Scheduler.grade(card(), Grade.EASY, now, settings)
        assertEquals(CardState.REVIEW, result.updatedCard.state)
        assertEquals(now + dayMs, result.updatedCard.dueAt)
    }

    @Test
    fun `new card graded again stays in learning shortly`() {
        val result = Scheduler.grade(card(), Grade.AGAIN, now, settings)
        assertEquals(CardState.LEARNING, result.updatedCard.state)
        assertEquals(now + 3 * 60_000L, result.updatedCard.dueAt)
        assertEquals(0, result.updatedCard.lapses)
    }

    @Test
    fun `learning card graded good graduates to review with day interval`() {
        val learning = card(state = CardState.LEARNING, stability = 2.31, difficulty = 2.12, lastReviewAt = now)
        val result = Scheduler.grade(learning, Grade.GOOD, now, settings)
        assertEquals(CardState.REVIEW, result.updatedCard.state)
        assertTrue(result.updatedCard.dueAt >= now + dayMs)
    }

    @Test
    fun `learning card graded again stays learning`() {
        val learning = card(state = CardState.LEARNING, stability = 2.31, difficulty = 2.12, lastReviewAt = now)
        val result = Scheduler.grade(learning, Grade.AGAIN, now, settings)
        assertEquals(CardState.LEARNING, result.updatedCard.state)
        assertEquals(now + 3 * 60_000L, result.updatedCard.dueAt)
    }

    @Test
    fun `review card graded again lapses back to learning and increments lapses`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs, reps = 5, lapses = 1,
        )
        val result = Scheduler.grade(review, Grade.AGAIN, now, settings)
        assertEquals(CardState.LEARNING, result.updatedCard.state)
        assertEquals(2, result.updatedCard.lapses)
        assertEquals(1.39, result.updatedCard.stability, 1e-9)
        assertEquals(8.35, result.updatedCard.difficulty, 1e-9)
    }

    @Test
    fun `review card graded good stays in review with fsrs state`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs, reps = 5,
        )
        val result = Scheduler.grade(review, Grade.GOOD, now, settings)
        assertEquals(CardState.REVIEW, result.updatedCard.state)
        assertEquals(32.03, result.updatedCard.stability, 1e-9)
        assertEquals(5.0, result.updatedCard.difficulty, 1e-9)
        assertEquals(6, result.updatedCard.reps)
        assertEquals(0, result.updatedCard.lapses)
    }

    @Test
    fun `review log records state before and elapsed days`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs,
        )
        val result = Scheduler.grade(review, Grade.GOOD, now, settings, durationMs = 1234)
        val log = result.reviewLog
        assertEquals(1L, log.cardId)
        assertEquals(now, log.reviewedAt)
        assertEquals(3, log.rating)
        assertEquals(CardState.REVIEW, log.stateBefore)
        assertEquals(10.0, log.elapsedDays, 1e-9)
        assertTrue(log.scheduledDays > 1.0)
        assertEquals(1234L, log.durationMs)
        assertTrue(log.countedTowardSchedule)
    }

    @Test
    fun `cram mode never mutates fsrs fields`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs, reps = 5, lapses = 2,
        )
        val result = Scheduler.grade(review, Grade.AGAIN, now, settings, countedTowardSchedule = false)
        assertEquals(review, result.updatedCard)
        assertFalse(result.reviewLog.countedTowardSchedule)
        assertEquals(1, result.reviewLog.rating)
    }

    @Test
    fun `previews cover all four grades and are ordered`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs,
        )
        val previews = Scheduler.previewIntervals(review, now, settings)
        assertEquals(Grade.entries.toSet(), previews.keys)
        assertTrue(previews.getValue(Grade.AGAIN).dueInMillis < previews.getValue(Grade.HARD).dueInMillis)
        assertTrue(previews.getValue(Grade.HARD).dueInMillis < previews.getValue(Grade.GOOD).dueInMillis)
        assertTrue(previews.getValue(Grade.GOOD).dueInMillis < previews.getValue(Grade.EASY).dueInMillis)
    }

    @Test
    fun `new card previews match learning steps`() {
        val previews = Scheduler.previewIntervals(card(), now, settings)
        assertEquals(3 * 60_000L, previews.getValue(Grade.AGAIN).dueInMillis)
        assertEquals(5 * 60_000L, previews.getValue(Grade.HARD).dueInMillis)
        assertEquals(10 * 60_000L, previews.getValue(Grade.GOOD).dueInMillis)
        assertEquals(dayMs, previews.getValue(Grade.EASY).dueInMillis)
        assertEquals("3 min", previews.getValue(Grade.AGAIN).caption)
        assertEquals("1 d", previews.getValue(Grade.EASY).caption)
    }

    @Test
    fun `higher retention target shortens intervals`() {
        val review = card(
            state = CardState.REVIEW, stability = 10.0, difficulty = 5.0,
            lastReviewAt = now - 10 * dayMs,
        )
        val default = Scheduler.previewIntervals(review, now, settings)
        val strict = Scheduler.previewIntervals(review, now, settings.copy(retentionTarget = 0.95))
        assertTrue(
            strict.getValue(Grade.GOOD).dueInMillis < default.getValue(Grade.GOOD).dueInMillis,
        )
    }

    private fun log(rating: Int, scheduledDays: Double, counted: Boolean = true, at: Long = 0) =
        ReviewLogEntity(
            cardId = 1, reviewedAt = at, rating = rating, stateBefore = CardState.REVIEW,
            elapsedDays = 0.0, scheduledDays = scheduledDays, durationMs = 0,
            countedTowardSchedule = counted,
        )

    @Test
    fun `mastery requires three consecutive good reviews with 21 day intervals`() {
        val mastered = listOf(log(3, 25.0), log(4, 30.0), log(3, 21.0))
        assertTrue(Scheduler.isMastered(mastered))
    }

    @Test
    fun `mastery fails with short intervals or low ratings`() {
        assertFalse(Scheduler.isMastered(listOf(log(3, 25.0), log(3, 10.0), log(3, 30.0))))
        assertFalse(Scheduler.isMastered(listOf(log(3, 25.0), log(2, 30.0), log(3, 30.0))))
        assertFalse(Scheduler.isMastered(listOf(log(3, 25.0), log(3, 30.0))))
        assertFalse(Scheduler.isMastered(emptyList()))
    }

    @Test
    fun `mastery ignores cram logs`() {
        val logs = listOf(log(3, 25.0), log(4, 30.0), log(1, 0.0, counted = false), log(3, 21.0))
        assertTrue(Scheduler.isMastered(logs))
        val broken = listOf(log(3, 25.0), log(1, 0.5), log(3, 30.0), log(3, 22.0))
        assertFalse(Scheduler.isMastered(broken))
    }

    @Test
    fun `mastery uses only the most recent counted reviews`() {
        val logs = listOf(log(1, 0.5), log(3, 25.0), log(3, 30.0), log(4, 22.0))
        assertTrue(Scheduler.isMastered(logs))
    }
}
