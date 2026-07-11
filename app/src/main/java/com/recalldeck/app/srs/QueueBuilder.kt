package com.recalldeck.app.srs

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import kotlin.random.Random

/** How the study queue is built. */
enum class QueueMode { DUE, RANDOM, CUSTOM }

/** Ordering for custom sessions. */
enum class CustomOrder { RANDOM, OLDEST, HARDEST }

/** Options for building a study queue. */
data class QueueOptions(
    val mode: QueueMode,
    /** Max NEW cards to introduce (DUE mode); remaining allowance for today. */
    val newLimit: Int = 20,
    /** Session size cap for RANDOM/CUSTOM modes; null = no cap. */
    val count: Int? = null,
    val order: CustomOrder = CustomOrder.RANDOM,
)

/**
 * Pure queue-building functions over lists of cards. Scoping (all / subject / category) is
 * done by the caller passing the already-scoped card list.
 */
object QueueBuilder {

    /** How many positions later an Again-graded card is re-inserted in the session. */
    const val AGAIN_REQUEUE_OFFSET = 10

    /**
     * Builds a study queue from [cards] at time [now].
     *
     * DUE mode: cards with state IN (LEARNING, REVIEW) and dueAt <= now, plus NEW cards up
     * to [QueueOptions.newLimit], shuffled with category interleaving.
     * RANDOM mode: all non-suspended cards regardless of dueAt, shuffled with interleaving,
     * capped at [QueueOptions.count].
     * CUSTOM mode: all non-suspended cards ordered per [QueueOptions.order], capped at
     * [QueueOptions.count].
     */
    fun buildQueue(
        cards: List<CardEntity>,
        options: QueueOptions,
        now: Long,
        random: Random = Random.Default,
    ): List<CardEntity> = when (options.mode) {
        QueueMode.DUE -> {
            val due = cards.filter {
                (it.state == CardState.LEARNING || it.state == CardState.REVIEW) && it.dueAt <= now
            }
            val newCards = cards.filter { it.state == CardState.NEW }
                .sortedBy { it.createdAt }
                .take(options.newLimit.coerceAtLeast(0))
            interleaveByCategory(due + newCards, random)
        }

        QueueMode.RANDOM -> {
            val pool = cards.filter { it.state != CardState.SUSPENDED }
            val queue = interleaveByCategory(pool, random)
            options.count?.let { queue.take(it) } ?: queue
        }

        QueueMode.CUSTOM -> {
            val pool = cards.filter { it.state != CardState.SUSPENDED }
            val ordered = when (options.order) {
                CustomOrder.RANDOM -> interleaveByCategory(pool, random)
                CustomOrder.OLDEST -> pool.sortedBy { it.lastReviewAt ?: Long.MIN_VALUE }
                CustomOrder.HARDEST -> pool.sortedByDescending { it.difficulty }
            }
            options.count?.let { ordered.take(it) } ?: ordered
        }
    }

    /**
     * Shuffles [cards] while interleaving categories: repeatedly picks from the category
     * with the most remaining cards (ties broken randomly), avoiding the previous category
     * when possible, so consecutive cards come from different categories.
     */
    fun interleaveByCategory(cards: List<CardEntity>, random: Random = Random.Default): List<CardEntity> {
        if (cards.size <= 1) return cards
        val buckets = cards.groupBy { it.categoryId }
            .mapValues { (_, v) -> v.shuffled(random).toMutableList() }
            .toMutableMap()
        val result = ArrayList<CardEntity>(cards.size)
        var lastCategory: Long? = null
        while (buckets.isNotEmpty()) {
            val candidates = buckets.keys.filter { it != lastCategory }.ifEmpty { buckets.keys.toList() }
            val maxSize = candidates.maxOf { buckets.getValue(it).size }
            val best = candidates.filter { buckets.getValue(it).size == maxSize }
            val pick = best[random.nextInt(best.size)]
            val bucket = buckets.getValue(pick)
            result.add(bucket.removeAt(0))
            if (bucket.isEmpty()) buckets.remove(pick)
            lastCategory = pick
        }
        return result
    }

    /**
     * Re-inserts [card] roughly [offset] positions after [position] in [queue] (clamped to
     * the queue end), for in-session Again re-queueing.
     */
    fun requeueAgain(
        queue: List<CardEntity>,
        card: CardEntity,
        position: Int,
        offset: Int = AGAIN_REQUEUE_OFFSET,
    ): List<CardEntity> {
        val insertAt = (position + offset).coerceIn(0, queue.size)
        return queue.toMutableList().apply { add(insertAt, card) }
    }
}

/** One graded step in a study session, kept for undo. */
data class SessionStep(
    /** Card snapshot before it was graded, to restore on undo. */
    val previousCard: CardEntity,
    /** Persisted review log id to delete on undo. */
    val reviewLogId: Long,
    /** Queue as it was before this grade was applied. */
    val previousQueue: List<CardEntity>,
    /** Position in the queue before this grade was applied. */
    val previousPosition: Int,
)

/** What the caller must persist to undo the last grade. */
data class UndoAction(
    /** Card to write back to the database. */
    val restoreCard: CardEntity,
    /** Review log row to delete. */
    val deleteReviewLogId: Long,
)

/**
 * Immutable in-session queue state with Again re-queueing and undo. All transitions are
 * pure; the caller persists card/log changes.
 */
data class StudySession(
    val queue: List<CardEntity>,
    val position: Int = 0,
    val history: List<SessionStep> = emptyList(),
) {
    val currentCard: CardEntity? get() = queue.getOrNull(position)

    val isFinished: Boolean get() = position >= queue.size

    /**
     * Advances past the current card after it was graded. [reviewLogId] is the persisted
     * log's id. If the grade was Again, the pre-grade card is re-inserted
     * ~[QueueBuilder.AGAIN_REQUEUE_OFFSET] positions later.
     */
    fun afterGrade(
        result: GradeResult,
        grade: Grade,
        reviewLogId: Long,
        againToEnd: Boolean = false,
    ): StudySession {
        val step = SessionStep(
            previousCard = result.previousCard,
            reviewLogId = reviewLogId,
            previousQueue = queue,
            previousPosition = position,
        )
        val nextQueue = if (grade == Grade.AGAIN) {
            val offset = if (againToEnd) queue.size else QueueBuilder.AGAIN_REQUEUE_OFFSET
            QueueBuilder.requeueAgain(queue, result.updatedCard, position + 1, offset)
        } else {
            queue
        }
        return copy(queue = nextQueue, position = position + 1, history = history + step)
    }

    /** Moves the current card to the end of the queue without grading it. */
    fun skipCurrent(): StudySession {
        val card = currentCard ?: return this
        if (position >= queue.lastIndex) return this
        val without = queue.filterIndexed { i, _ -> i != position }
        return copy(queue = without + card)
    }

    /** Undoes the last grade, restoring queue position and returning what to persist. */
    fun undo(): Pair<StudySession, UndoAction>? {
        val step = history.lastOrNull() ?: return null
        val restored = copy(
            queue = step.previousQueue,
            position = step.previousPosition,
            history = history.dropLast(1),
        )
        return restored to UndoAction(restoreCard = step.previousCard, deleteReviewLogId = step.reviewLogId)
    }
}
