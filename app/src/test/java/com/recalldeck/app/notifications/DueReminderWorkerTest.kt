package com.recalldeck.app.notifications

import android.Manifest
import android.app.NotificationManager
import androidx.test.core.app.ApplicationProvider
import androidx.work.ListenableWorker
import androidx.work.testing.TestListenableWorkerBuilder
import com.recalldeck.app.RecallDeckApplication
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.SubjectEntity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

@RunWith(RobolectricTestRunner::class)
class DueReminderWorkerTest {

    private lateinit var app: RecallDeckApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        shadowOf(app).grantPermissions(Manifest.permission.POST_NOTIFICATIONS)
    }

    private suspend fun seedDueCards(count: Int) {
        val db = app.container.database
        val subjectId = db.subjectDao().insert(
            SubjectEntity(name = "S", colorHex = "#fff", position = 0, createdAt = 1),
        )
        val categoryId = db.categoryDao().insert(
            CategoryEntity(subjectId = subjectId, name = "C", position = 0, createdAt = 1),
        )
        repeat(count) {
            db.cardDao().insert(
                CardEntity(
                    categoryId = categoryId,
                    type = CardType.BASIC,
                    question = "q$it",
                    answer = "a$it",
                    state = CardState.REVIEW,
                    dueAt = 0L,
                    createdAt = 1,
                    updatedAt = 1,
                ),
            )
        }
    }

    private fun notificationManager(): NotificationManager =
        app.getSystemService(NotificationManager::class.java)

    @Test
    fun postsNotificationWithDueCountAndDeepLinkExtra() = runTest {
        seedDueCards(3)
        val worker = TestListenableWorkerBuilder<DueReminderWorker>(app).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        val shadow = shadowOf(notificationManager())
        assertEquals(1, shadow.allNotifications.size)
        val notification = shadow.allNotifications.single()
        assertEquals("3 cards due", shadowOf(notification).contentTitle)
        val contentIntent = shadowOf(notification.contentIntent).savedIntent
        assertTrue(contentIntent.getBooleanExtra(DueReminderWorker.EXTRA_OPEN_DUE_REVIEW, false))
    }

    @Test
    fun usesSingularWordingForOneCard() = runTest {
        seedDueCards(1)
        val worker = TestListenableWorkerBuilder<DueReminderWorker>(app).build()

        worker.doWork()

        val notification = shadowOf(notificationManager()).allNotifications.single()
        assertEquals("1 card due", shadowOf(notification).contentTitle)
    }

    @Test
    fun postsNothingWhenNoCardsAreDue() = runTest {
        val worker = TestListenableWorkerBuilder<DueReminderWorker>(app).build()

        val result = worker.doWork()

        assertEquals(ListenableWorker.Result.success(), result)
        assertTrue(shadowOf(notificationManager()).allNotifications.isEmpty())
    }
}
