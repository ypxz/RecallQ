package com.recalldeck.app.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context

object NotificationChannels {

    const val DUE_REMINDERS = "due_reminders"

    fun ensureCreated(context: Context) {
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            DUE_REMINDERS,
            "Due card reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Daily reminder when cards are due for review"
        }
        manager.createNotificationChannel(channel)
    }
}
