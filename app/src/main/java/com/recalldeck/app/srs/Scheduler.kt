package com.recalldeck.app.srs

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.repo.AppSettings
import java.util.Locale
import kotlin.math.max

/** Predicted outcome for one grade button, used for button captions. */
data class IntervalPreview(
    /** Milliseconds until the card would next be due if this grade is chosen. */
    val dueInMillis: Long,
    /** Human-readable caption, e.g. "<3 min", "10 min", "3 d", "2.1 mo". */
    val caption: String,
)

/** Result of grading a card. */
data class GradeResult(
    /** Card with FSRS fields updated (identical to the input card in cram mode). */
    val updatedCard: CardEntity,
    /** Review log to insert (id = 0; the DAO assigns the real id). */
    val reviewLog: ReviewLogEntity,
    /** Predicted next intervals for all four buttons, computed from the pre-grade state. */
    val previews: Map<Grade, IntervalPreview>,
    /** Snapshot of the card before grading, for undo. */
    val previousCard: CardEntity,
)

/**
 * Pure FSRS-6 scheduler over [CardEntity]. State machine: NEW -> LEARNING -> REVIEW.
 * A REVIEW card graded Again lapses back to LEARNING.
 */
object Scheduler {

    private const val MINUTE_MS = 60_000L
    private const val DAY_MS = 86_400_000L

    /** Consecutive rating >= 3 reviews required for mastery. */
    const val MASTERY_STREAK = 3

    /** Minimum scheduled interval (days) for a review to count toward mastery. */
    const val MASTERY_MIN_SCHEDULED_DAYS = 21.0

    /**
     * Grades [card] with [grade] at [now] (epoch millis).
     *
     * In cram mode ([countedTowardSchedule] = false) the returned [GradeResult.updatedCard]
     * is the input card unchanged: FSRS fields are never mutated, only a log is produced.
     */
    fun grade(
        card: CardEntity,
        grade: Grade,
        now: Long,
        settings: AppSettings,
        countedTowardSchedule: Boolean = true,
        durationMs: Long = 0L,
    ): GradeResult {
        val fsrs = Fsrs(requestRetention = settings.retentionTarget)
        val previews = previewIntervals(card, now, settings)
        val chosen = previews.getValue(grade)
        val scheduledDays = chosen.dueInMillis.toDouble() / DAY_MS
        val elapsedDays = elapsedDays(card, now)

        val reviewLog = ReviewLogEntity(
            cardId = card.id,
            reviewedAt = now,
            rating = grade.fsrsRating,
            stateBefore = card.state,
            elapsedDays = elapsedDays,
            scheduledDays = scheduledDays,
            durationMs = durationMs,
            countedTowardSchedule = countedTowardSchedule,
        )

        if (!countedTowardSchedule) {
            return GradeResult(updatedCard = card, reviewLog = reviewLog, previews = previews, previousCard = card)
        }

        val nextMemory = nextMemoryState(fsrs, card, elapsedDays, grade)
        val nextState = nextCardState(card.state, grade)
        val updatedCard = card.copy(
            state = nextState,
            dueAt = now + chosen.dueInMillis,
            stability = nextMemory.stability,
            difficulty = nextMemory.difficulty,
            reps = card.reps + 1,
            lapses = card.lapses + if (card.state == CardState.REVIEW && grade == Grade.AGAIN) 1 else 0,
            lastReviewAt = now,
            updatedAt = now,
        )
        return GradeResult(updatedCard = updatedCard, reviewLog = reviewLog, previews = previews, previousCard = card)
    }

    /**
     * Predicted next interval for each of the four buttons, from the card's current state.
     */
    fun previewIntervals(card: CardEntity, now: Long, settings: AppSettings): Map<Grade, IntervalPreview> {
        val fsrs = Fsrs(requestRetention = settings.retentionTarget)
        return when (card.state) {
            CardState.NEW -> mapOf(
                Grade.AGAIN to minutesPreview(settings.againDelayMinutes * MINUTE_MS),
                Grade.HARD to minutesPreview(settings.newHardDelayMinutes * MINUTE_MS),
                Grade.GOOD to minutesPreview(settings.newGoodDelayMinutes * MINUTE_MS),
                Grade.EASY to daysPreview(1),
            )

            CardState.LEARNING -> {
                val good = fsrs.nextIntervalDays(memoryFor(fsrs, card, Grade.GOOD).stability)
                val easyRaw = fsrs.nextIntervalDays(memoryFor(fsrs, card, Grade.EASY).stability)
                val easy = max(easyRaw, good + 1)
                mapOf(
                    Grade.AGAIN to minutesPreview(settings.againDelayMinutes * MINUTE_MS),
                    Grade.HARD to minutesPreview(settings.learningHardDelayMinutes * MINUTE_MS),
                    Grade.GOOD to daysPreview(good),
                    Grade.EASY to daysPreview(easy),
                )
            }

            CardState.REVIEW, CardState.SUSPENDED -> {
                val elapsed = elapsedDays(card, now)
                var hard = fsrs.nextIntervalDays(fsrs.nextState(memoryOf(card), elapsed, Grade.HARD).stability)
                var good = fsrs.nextIntervalDays(fsrs.nextState(memoryOf(card), elapsed, Grade.GOOD).stability)
                var easy = fsrs.nextIntervalDays(fsrs.nextState(memoryOf(card), elapsed, Grade.EASY).stability)
                hard = minOf(hard, good)
                good = max(good, hard + 1)
                easy = max(easy, good + 1)
                mapOf(
                    Grade.AGAIN to minutesPreview(settings.againDelayMinutes * MINUTE_MS),
                    Grade.HARD to daysPreview(hard),
                    Grade.GOOD to daysPreview(good),
                    Grade.EASY to daysPreview(easy),
                )
            }
        }
    }

    /**
     * Mastery ("successive relearning"): the last [MASTERY_STREAK] counted reviews all have
     * rating >= 3 and scheduledDays >= [MASTERY_MIN_SCHEDULED_DAYS]. [logs] must be in
     * chronological order (as returned by ReviewLogDao.getByCard).
     */
    fun isMastered(logs: List<ReviewLogEntity>): Boolean {
        val counted = logs.filter { it.countedTowardSchedule }
        if (counted.size < MASTERY_STREAK) return false
        return counted.takeLast(MASTERY_STREAK).all {
            it.rating >= 3 && it.scheduledDays >= MASTERY_MIN_SCHEDULED_DAYS
        }
    }

    private fun nextMemoryState(fsrs: Fsrs, card: CardEntity, elapsedDays: Double, grade: Grade): Fsrs.MemoryState =
        when (card.state) {
            CardState.NEW -> fsrs.initialState(grade)
            CardState.LEARNING -> memoryFor(fsrs, card, grade)
            CardState.REVIEW, CardState.SUSPENDED -> fsrs.nextState(memoryOf(card), elapsedDays, grade)
        }

    private fun memoryFor(fsrs: Fsrs, card: CardEntity, grade: Grade): Fsrs.MemoryState =
        if (card.difficulty == 0.0) fsrs.initialState(grade) else fsrs.shortTermState(memoryOf(card), grade)

    private fun nextCardState(state: CardState, grade: Grade): CardState = when (state) {
        CardState.NEW -> if (grade == Grade.EASY) CardState.REVIEW else CardState.LEARNING
        CardState.LEARNING -> if (grade.fsrsRating >= 3) CardState.REVIEW else CardState.LEARNING
        CardState.REVIEW, CardState.SUSPENDED ->
            if (grade == Grade.AGAIN) CardState.LEARNING else CardState.REVIEW
    }

    private fun memoryOf(card: CardEntity): Fsrs.MemoryState =
        Fsrs.MemoryState(stability = card.stability, difficulty = card.difficulty)

    private fun elapsedDays(card: CardEntity, now: Long): Double {
        val last = card.lastReviewAt ?: return 0.0
        return max(0.0, (now - last).toDouble() / DAY_MS)
    }

    private fun minutesPreview(millis: Long): IntervalPreview =
        IntervalPreview(dueInMillis = millis, caption = "${millis / MINUTE_MS} min")

    private fun daysPreview(days: Int): IntervalPreview {
        val caption = when {
            days >= 365 -> String.format(Locale.US, "%.1f yr", days / 365.0)
            days >= 30 -> String.format(Locale.US, "%.1f mo", days / 30.0)
            else -> "$days d"
        }
        return IntervalPreview(dueInMillis = days * DAY_MS, caption = caption)
    }
}
