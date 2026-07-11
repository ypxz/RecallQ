package com.recalldeck.app.data.stats

import com.recalldeck.app.data.db.CardDao
import com.recalldeck.app.data.db.CategoryDao
import com.recalldeck.app.data.db.ReviewLogDao
import com.recalldeck.app.data.db.SubjectDao
import java.time.Instant
import java.time.ZoneId

/**
 * Aggregates review/card statistics for the Stats screen. All heavy lifting
 * is delegated to the pure functions in [StatsCalculator].
 */
class StatsRepo(
    private val subjectDao: SubjectDao,
    private val categoryDao: CategoryDao,
    private val cardDao: CardDao,
    private val reviewLogDao: ReviewLogDao,
) {

    suspend fun snapshot(
        now: Long = System.currentTimeMillis(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): StatsSnapshot {
        val today = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val logs = reviewLogDao.getAll()
        val cards = cardDao.getAll()
        val subjects = subjectDao.getAll()
        val categories = categoryDao.getAll()
        return StatsSnapshot(
            currentStreakDays = StatsCalculator.currentStreak(logs, today, zone),
            heatmap = StatsCalculator.heatmap(logs, today, zone),
            forecast = StatsCalculator.dueForecast(cards, today, zone),
            retentionPercent = StatsCalculator.retentionPercent(logs, now),
            subjectBreakdown = StatsCalculator.subjectBreakdown(subjects, categories, cards, logs),
        )
    }
}
