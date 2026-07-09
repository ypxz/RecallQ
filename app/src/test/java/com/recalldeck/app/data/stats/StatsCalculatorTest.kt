package com.recalldeck.app.data.stats

import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.ReviewLogEntity
import com.recalldeck.app.data.db.SubjectEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

class StatsCalculatorTest {

    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 7, 9)

    private fun millisOn(date: LocalDate, time: LocalTime = LocalTime.NOON): Long =
        date.atTime(time).toInstant(zone).toEpochMilli()

    private fun log(
        reviewedAt: Long,
        rating: Int = 3,
        counted: Boolean = true,
    ) = ReviewLogEntity(
        cardId = 1,
        reviewedAt = reviewedAt,
        rating = rating,
        stateBefore = CardState.REVIEW,
        elapsedDays = 1.0,
        scheduledDays = 1.0,
        durationMs = 100,
        countedTowardSchedule = counted,
    )

    private fun card(
        categoryId: Long = 1,
        state: CardState = CardState.REVIEW,
        dueAt: Long = 0,
    ) = CardEntity(
        categoryId = categoryId,
        type = CardType.BASIC,
        question = "q",
        answer = "a",
        state = state,
        dueAt = dueAt,
        createdAt = 1,
        updatedAt = 1,
    )

    // ---- currentStreak ----

    @Test
    fun streakIsZeroWithNoReviews() {
        assertEquals(0, StatsCalculator.currentStreak(emptyList(), today, zone))
    }

    @Test
    fun streakCountsConsecutiveDaysEndingToday() {
        val logs = listOf(
            log(millisOn(today)),
            log(millisOn(today.minusDays(1))),
            log(millisOn(today.minusDays(2))),
            // Gap on day -3.
            log(millisOn(today.minusDays(4))),
        )
        assertEquals(3, StatsCalculator.currentStreak(logs, today, zone))
    }

    @Test
    fun streakSurvivesWhenTodayNotYetReviewed() {
        val logs = listOf(
            log(millisOn(today.minusDays(1))),
            log(millisOn(today.minusDays(2))),
        )
        assertEquals(2, StatsCalculator.currentStreak(logs, today, zone))
    }

    @Test
    fun streakBrokenByFullMissedDay() {
        val logs = listOf(log(millisOn(today.minusDays(2))))
        assertEquals(0, StatsCalculator.currentStreak(logs, today, zone))
    }

    @Test
    fun streakIgnoresCramReviews() {
        val logs = listOf(
            log(millisOn(today), counted = false),
            log(millisOn(today.minusDays(1))),
        )
        // Cram today doesn't extend; streak is yesterday's single day.
        assertEquals(1, StatsCalculator.currentStreak(logs, today, zone))
    }

    @Test
    fun multipleReviewsOnSameDayCountOnce() {
        val logs = listOf(
            log(millisOn(today, LocalTime.of(8, 0))),
            log(millisOn(today, LocalTime.of(20, 0))),
        )
        assertEquals(1, StatsCalculator.currentStreak(logs, today, zone))
    }

    // ---- heatmap ----

    @Test
    fun heatmapCovers84DaysEndingToday() {
        val heatmap = StatsCalculator.heatmap(emptyList(), today, zone)
        assertEquals(84, heatmap.size)
        assertEquals(today.minusDays(83), heatmap.first().date)
        assertEquals(today, heatmap.last().date)
        assertEquals(0, heatmap.sumOf { it.reviewCount })
    }

    @Test
    fun heatmapBucketsCountsPerDayAndIgnoresOutOfRange() {
        val logs = listOf(
            log(millisOn(today)),
            log(millisOn(today)),
            log(millisOn(today.minusDays(83))),
            log(millisOn(today.minusDays(84))), // out of range
            log(millisOn(today.plusDays(1))), // future, out of range
        )
        val heatmap = StatsCalculator.heatmap(logs, today, zone)
        assertEquals(2, heatmap.last().reviewCount)
        assertEquals(1, heatmap.first().reviewCount)
        assertEquals(3, heatmap.sumOf { it.reviewCount })
    }

    @Test
    fun heatmapIncludesCramReviews() {
        val logs = listOf(log(millisOn(today), counted = false))
        val heatmap = StatsCalculator.heatmap(logs, today, zone)
        assertEquals(1, heatmap.last().reviewCount)
    }

    // ---- dueForecast ----

    @Test
    fun forecastCovers30DaysStartingToday() {
        val forecast = StatsCalculator.dueForecast(emptyList(), today, zone)
        assertEquals(30, forecast.size)
        assertEquals(today, forecast.first().date)
        assertEquals(today.plusDays(29), forecast.last().date)
    }

    @Test
    fun forecastBucketsOverdueIntoTodayAndSkipsBeyondWindow() {
        val cards = listOf(
            card(dueAt = millisOn(today.minusDays(5))), // overdue -> today
            card(dueAt = millisOn(today)),
            card(dueAt = millisOn(today.plusDays(3))),
            card(dueAt = millisOn(today.plusDays(29))),
            card(dueAt = millisOn(today.plusDays(30))), // beyond window
        )
        val forecast = StatsCalculator.dueForecast(cards, today, zone)
        assertEquals(2, forecast[0].dueCount)
        assertEquals(1, forecast[3].dueCount)
        assertEquals(1, forecast[29].dueCount)
        assertEquals(4, forecast.sumOf { it.dueCount })
    }

    @Test
    fun forecastIgnoresNewAndSuspendedCards() {
        val cards = listOf(
            card(state = CardState.NEW, dueAt = millisOn(today)),
            card(state = CardState.SUSPENDED, dueAt = millisOn(today)),
            card(state = CardState.LEARNING, dueAt = millisOn(today)),
        )
        val forecast = StatsCalculator.dueForecast(cards, today, zone)
        assertEquals(1, forecast[0].dueCount)
    }

    // ---- retentionPercent ----

    @Test
    fun retentionIsNullWithNoCountedReviewsInWindow() {
        val now = millisOn(today)
        assertNull(StatsCalculator.retentionPercent(emptyList(), now))
        val oldLog = log(now - 31L * 24 * 60 * 60 * 1000)
        assertNull(StatsCalculator.retentionPercent(listOf(oldLog), now))
    }

    @Test
    fun retentionCountsRatingsAtLeastTwoOverCountedLast30Days() {
        val now = millisOn(today)
        val dayMs = 24L * 60 * 60 * 1000
        val logs = listOf(
            log(now - 1 * dayMs, rating = 1), // forgot
            log(now - 2 * dayMs, rating = 2),
            log(now - 3 * dayMs, rating = 3),
            log(now - 4 * dayMs, rating = 4),
            log(now - 5 * dayMs, rating = 1, counted = false), // cram, ignored
            log(now - 40 * dayMs, rating = 1), // outside window, ignored
        )
        assertEquals(75.0, StatsCalculator.retentionPercent(logs, now)!!, 0.0001)
    }

    // ---- subjectBreakdown ----

    @Test
    fun subjectBreakdownCountsStatesPerSubject() {
        val subjects = listOf(
            SubjectEntity(id = 1, name = "Bio", colorHex = "#fff", position = 0, createdAt = 1),
            SubjectEntity(id = 2, name = "Law", colorHex = "#fff", position = 1, createdAt = 2),
        )
        val categories = listOf(
            CategoryEntity(id = 10, subjectId = 1, name = "c1", position = 0, createdAt = 1),
            CategoryEntity(id = 20, subjectId = 2, name = "c2", position = 0, createdAt = 1),
        )
        val cards = listOf(
            card(categoryId = 10, state = CardState.NEW),
            card(categoryId = 10, state = CardState.NEW),
            card(categoryId = 10, state = CardState.REVIEW),
            card(categoryId = 20, state = CardState.SUSPENDED),
        )
        val breakdown = StatsCalculator.subjectBreakdown(subjects, categories, cards)
        assertEquals(2, breakdown.size)
        val bio = breakdown[0]
        assertEquals("Bio", bio.subjectName)
        assertEquals(2, bio.stateCounts[CardState.NEW])
        assertEquals(1, bio.stateCounts[CardState.REVIEW])
        assertEquals(3, bio.totalCards)
        val law = breakdown[1]
        assertEquals(1, law.stateCounts[CardState.SUSPENDED])
        assertEquals(1, law.totalCards)
    }

    @Test
    fun subjectBreakdownIncludesEmptySubjects() {
        val subjects = listOf(
            SubjectEntity(id = 1, name = "Empty", colorHex = "#fff", position = 0, createdAt = 1),
        )
        val breakdown = StatsCalculator.subjectBreakdown(subjects, emptyList(), emptyList())
        assertEquals(1, breakdown.size)
        assertEquals(0, breakdown[0].totalCards)
    }
}
