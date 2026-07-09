package com.recalldeck.app.notifications

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

class ReminderSchedulerTest {

    @Test
    fun delayUntilLaterToday() {
        val now = LocalDateTime.of(2026, 7, 9, 8, 0)
        val delay = ReminderScheduler.initialDelay(LocalTime.of(20, 30), now)
        assertEquals(Duration.ofHours(12).plusMinutes(30), delay)
    }

    @Test
    fun delayRollsToTomorrowWhenTimeAlreadyPassed() {
        val now = LocalDateTime.of(2026, 7, 9, 21, 0)
        val delay = ReminderScheduler.initialDelay(LocalTime.of(20, 0), now)
        assertEquals(Duration.ofHours(23), delay)
    }

    @Test
    fun delayRollsToTomorrowWhenTimeIsExactlyNow() {
        val now = LocalDateTime.of(2026, 7, 9, 9, 0)
        val delay = ReminderScheduler.initialDelay(LocalTime.of(9, 0), now)
        assertEquals(Duration.ofDays(1), delay)
    }
}
