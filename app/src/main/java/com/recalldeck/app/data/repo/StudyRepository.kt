package com.recalldeck.app.data.repo

import com.recalldeck.app.data.db.CardDao
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.ReviewLogDao
import com.recalldeck.app.data.db.ReviewLogEntity

/** Study scope: all cards, one subject, or one category. */
data class StudyScope(val subjectId: Long? = null, val categoryId: Long? = null)

/**
 * Persistence for study sessions: loading scoped cards, applying grades, and undoing them.
 */
class StudyRepository(
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
) {
    suspend fun getCards(scope: StudyScope): List<CardEntity> = when {
        scope.categoryId != null -> cardDao.getByCategory(scope.categoryId)
        scope.subjectId != null -> cardDao.getBySubject(scope.subjectId)
        else -> cardDao.getAll()
    }

    /**
     * Persists one grade: updates the card (skipped in cram mode, where the card is
     * unchanged) and inserts the review log. Returns the log id for undo.
     */
    suspend fun persistGrade(updatedCard: CardEntity, log: ReviewLogEntity): Long {
        if (log.countedTowardSchedule) cardDao.update(updatedCard)
        return reviewLogDao.insert(log)
    }

    /** Undoes a grade: restores the pre-grade card and deletes its review log. */
    suspend fun undoGrade(restoreCard: CardEntity, reviewLogId: Long) {
        cardDao.update(restoreCard)
        reviewLogDao.deleteById(reviewLogId)
    }

    suspend fun getCard(id: Long): CardEntity? = cardDao.getById(id)

    suspend fun updateCard(card: CardEntity) = cardDao.update(card)

    suspend fun logsFor(cardId: Long): List<ReviewLogEntity> = reviewLogDao.getByCard(cardId)
}
