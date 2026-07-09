package com.recalldeck.app.notifications

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * (Re)enqueues the daily due-card reminder at a user-set time of day.
 */
object ReminderScheduler {

    const val WORK_NAME = "due_reminder"

    fun schedule(context: Context, reminderTime: LocalTime, now: LocalDateTime = LocalDateTime.now()) {
        val request = PeriodicWorkRequestBuilder<DueReminderWorker>(Duration.ofDays(1))
            .setInitialDelay(initialDelay(reminderTime, now))
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }

    /** Delay from [now] until the next occurrence of [reminderTime]. */
    fun initialDelay(reminderTime: LocalTime, now: LocalDateTime): Duration {
        var next = now.toLocalDate().atTime(reminderTime)
        if (!next.isAfter(now)) {
            next = next.plusDays(1)
        }
        return Duration.between(now, next)
    }
}
