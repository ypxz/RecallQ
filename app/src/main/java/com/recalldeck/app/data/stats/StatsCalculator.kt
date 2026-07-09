package com.recalldeck.app.data.stats

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.db.SubjectEntity
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

/**
 * Pure aggregation functions over query results. All time-dependent
 * calculations take an explicit `today`/`now` and zone for testability.
 */
object StatsCalculator {

    const val HEATMAP_WEEKS = 12
    const val FORECAST_DAYS = 30
    const val RETENTION_WINDOW_DAYS = 30

    private fun ReviewLogEntity.localDate(zone: ZoneId): LocalDate =
        Instant.ofEpochMilli(reviewedAt).atZone(zone).toLocalDate()

    /**
     * Number of consecutive days with at least one counted review, ending
     * today or yesterday (a streak is not broken until a full day is missed).
     */
    fun currentStreak(logs: List<ReviewLogEntity>, today: LocalDate, zone: ZoneId): Int {
        val reviewDays = logs
            .filter { it.countedTowardSchedule }
            .map { it.localDate(zone) }
            .toHashSet()
        var day = when {
            today in reviewDays -> today
            today.minusDays(1) in reviewDays -> today.minusDays(1)
            else -> return 0
        }
        var streak = 0
        while (day in reviewDays) {
            streak++
            day = day.minusDays(1)
        }
        return streak
    }

    /**
     * Daily review counts (all reviews, cram included) for the last 12 weeks
     * (84 days ending today), oldest day first.
     */
    fun heatmap(logs: List<ReviewLogEntity>, today: LocalDate, zone: ZoneId): List<HeatmapDay> {
        val start = today.minusDays(HEATMAP_WEEKS * 7L - 1)
        val countsByDay = logs
            .map { it.localDate(zone) }
            .filter { !it.isBefore(start) && !it.isAfter(today) }
            .groupingBy { it }
            .eachCount()
        return (0 until HEATMAP_WEEKS * 7L).map { offset ->
            val date = start.plusDays(offset)
            HeatmapDay(date, countsByDay[date] ?: 0)
        }
    }

    /**
     * Due counts for the next 30 days (today first). Cards already overdue
     * are bucketed into today. Only LEARNING/REVIEW cards count.
     */
    fun dueForecast(cards: List<CardEntity>, today: LocalDate, zone: ZoneId): List<ForecastDay> {
        val end = today.plusDays(FORECAST_DAYS - 1L)
        val countsByDay = HashMap<LocalDate, Int>()
        for (card in cards) {
            if (card.state != CardState.LEARNING && card.state != CardState.REVIEW) continue
            var dueDate = Instant.ofEpochMilli(card.dueAt).atZone(zone).toLocalDate()
            if (dueDate.isBefore(today)) dueDate = today
            if (dueDate.isAfter(end)) continue
            countsByDay[dueDate] = (countsByDay[dueDate] ?: 0) + 1
        }
        return (0 until FORECAST_DAYS.toLong()).map { offset ->
            val date = today.plusDays(offset)
            ForecastDay(date, countsByDay[date] ?: 0)
        }
    }

    /**
     * Retention over counted reviews in the last 30 days: percentage with
     * rating >= 2. Null when there are no such reviews.
     */
    fun retentionPercent(logs: List<ReviewLogEntity>, now: Long): Double? {
        val since = now - RETENTION_WINDOW_DAYS * 24L * 60 * 60 * 1000
        val window = logs.filter {
            it.countedTowardSchedule && it.reviewedAt in since..now
        }
        if (window.isEmpty()) return null
        val remembered = window.count { it.rating >= 2 }
        return remembered * 100.0 / window.size
    }

    /** Card counts per state for every subject, in subject position order. */
    fun subjectBreakdown(
        subjects: List<SubjectEntity>,
        categories: List<CategoryEntity>,
        cards: List<CardEntity>,
    ): List<SubjectBreakdown> {
        val subjectByCategory = categories.associate { it.id to it.subjectId }
        val cardsBySubject = cards.groupBy { subjectByCategory[it.categoryId] }
        return subjects.map { subject ->
            val stateCounts = (cardsBySubject[subject.id] ?: emptyList())
                .groupingBy { it.state }
                .eachCount()
            SubjectBreakdown(subject.id, subject.name, stateCounts)
        }
    }
}
