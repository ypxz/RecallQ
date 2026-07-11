package com.recalldeck.app.data.stats

import com.recalldeck.app.data.db.CardBucket
import java.time.LocalDate

/** One day cell of the 12-week review heatmap. */
data class HeatmapDay(
    val date: LocalDate,
    val reviewCount: Int,
)

/** One day of the 30-day due forecast. Overdue cards are bucketed into day 0. */
data class ForecastDay(
    val date: LocalDate,
    val dueCount: Int,
)

data class SubjectBreakdown(
    val subjectId: Long,
    val subjectName: String,
    val bucketCounts: Map<CardBucket, Int>,
) {
    val totalCards: Int get() = bucketCounts.values.sum()
}

data class StatsSnapshot(
    val currentStreakDays: Int,
    val heatmap: List<HeatmapDay>,
    val forecast: List<ForecastDay>,
    val retentionPercent: Double?,
    val subjectBreakdown: List<SubjectBreakdown>,
)
