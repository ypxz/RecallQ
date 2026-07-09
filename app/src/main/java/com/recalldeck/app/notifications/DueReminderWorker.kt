package com.recalldeck.app.notifications

import android.Manifest
import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.recalldeck.app.MainActivity
import com.recalldeck.app.RecallDeckApplication

/**
 * Daily worker that queries the number of due cards and posts an
 * "N cards due" notification. Posting is skipped when there are no due
 * cards or when POST_NOTIFICATIONS has not been granted (API 33+).
 */
class DueReminderWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val app = applicationContext as? RecallDeckApplication ?: return Result.failure()
        val dueCount = app.container.database.cardDao()
            .getDueCount(System.currentTimeMillis())
        if (dueCount > 0 && canPostNotifications()) {
            postNotification(dueCount)
        }
        return Result.success()
    }

    private fun canPostNotifications(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED

    private fun postNotification(dueCount: Int) {
        NotificationChannels.ensureCreated(applicationContext)
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(EXTRA_OPEN_DUE_REVIEW, true)
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val cardsWord = if (dueCount == 1) "card" else "cards"
        val notification = Notification.Builder(applicationContext, NotificationChannels.DUE_REMINDERS)
            .setSmallIcon(android.R.drawable.ic_menu_agenda)
            .setContentTitle("$dueCount $cardsWord due")
            .setContentText("Time to review your due cards")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()
        applicationContext.getSystemService(NotificationManager::class.java)
            ?.notify(NOTIFICATION_ID, notification)
    }

    companion object {
        /** Intent extra telling MainActivity to open the due review flow. */
        const val EXTRA_OPEN_DUE_REVIEW = "com.recalldeck.app.OPEN_DUE_REVIEW"
        const val NOTIFICATION_ID = 1001
    }
}
