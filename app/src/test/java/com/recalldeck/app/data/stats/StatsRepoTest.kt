package com.recalldeck.app.data.stats

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
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@RunWith(RobolectricTestRunner::class)
class StatsRepoTest {

    private lateinit var db: RecallDeckDatabase
    private lateinit var statsRepo: StatsRepo

    private val zone = ZoneOffset.UTC
    private val today: LocalDate = LocalDate.of(2026, 7, 9)
    private val now: Long = today.atTime(LocalTime.NOON).toInstant(zone).toEpochMilli()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, RecallDeckDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        statsRepo = StatsRepo(
            db.subjectDao(),
            db.categoryDao(),
            db.cardDao(),
            db.reviewLogDao(),
        )
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun emptyDatabaseProducesEmptySnapshot() = runTest {
        val snapshot = statsRepo.snapshot(now = now, zone = zone)
        assertEquals(0, snapshot.currentStreakDays)
        assertEquals(84, snapshot.heatmap.size)
        assertEquals(0, snapshot.heatmap.sumOf { it.reviewCount })
        assertEquals(30, snapshot.forecast.size)
        assertEquals(0, snapshot.forecast.sumOf { it.dueCount })
        assertEquals(null, snapshot.retentionPercent)
        assertEquals(emptyList<SubjectBreakdown>(), snapshot.subjectBreakdown)
    }

    @Test
    fun snapshotAggregatesAcrossTables() = runTest {
        val subjectId = db.subjectDao().insert(
            SubjectEntity(name = "Bio", colorHex = "#fff", position = 0, createdAt = 1),
        )
        val categoryId = db.categoryDao().insert(
            CategoryEntity(subjectId = subjectId, name = "c", position = 0, createdAt = 1),
        )
        val dayMs = 24L * 60 * 60 * 1000
        val cardId = db.cardDao().insert(
            CardEntity(
                categoryId = categoryId,
                type = CardType.BASIC,
                question = "q",
                answer = "a",
                state = CardState.REVIEW,
                dueAt = now + 2 * dayMs,
                createdAt = 1,
                updatedAt = 1,
            ),
        )
        db.cardDao().insert(
            CardEntity(
                categoryId = categoryId,
                type = CardType.BASIC,
                question = "q2",
                answer = "a2",
                state = CardState.NEW,
                dueAt = 0,
                createdAt = 2,
                updatedAt = 2,
            ),
        )
        // Reviews today and yesterday -> streak 2.
        db.reviewLogDao().insert(reviewLog(cardId, now, rating = 3))
        db.reviewLogDao().insert(reviewLog(cardId, now - dayMs, rating = 1))

        val snapshot = statsRepo.snapshot(now = now, zone = zone)

        assertEquals(2, snapshot.currentStreakDays)
        assertEquals(1, snapshot.heatmap.last().reviewCount)
        assertEquals(2, snapshot.heatmap.sumOf { it.reviewCount })
        assertEquals(1, snapshot.forecast[2].dueCount)
        assertEquals(50.0, snapshot.retentionPercent!!, 0.0001)
        val bio = snapshot.subjectBreakdown.single()
        assertEquals(1, bio.stateCounts[CardState.REVIEW])
        assertEquals(1, bio.stateCounts[CardState.NEW])
    }

    private fun reviewLog(cardId: Long, reviewedAt: Long, rating: Int) = ReviewLogEntity(
        cardId = cardId,
        reviewedAt = reviewedAt,
        rating = rating,
        stateBefore = CardState.REVIEW,
        elapsedDays = 1.0,
        scheduledDays = 1.0,
        durationMs = 100,
    )
}
