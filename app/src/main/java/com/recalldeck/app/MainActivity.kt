package com.recalldeck.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.recalldeck.app.data.repo.AppSettings
import com.recalldeck.app.data.repo.ThemeMode
import com.recalldeck.app.notifications.DueReminderWorker
import com.recalldeck.app.ui.navigation.Destinations
import com.recalldeck.app.ui.navigation.RecallDeckNavHost
import com.recalldeck.app.ui.theme.RecallDeckTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val openDueReview =
            intent?.getBooleanExtra(DueReminderWorker.EXTRA_OPEN_DUE_REVIEW, false) == true
        setContent {
            val settings by (application as RecallDeckApplication)
                .container.settingsRepository.settings
                .collectAsState(initial = AppSettings())
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            RecallDeckTheme(darkTheme = darkTheme) {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val navController = rememberNavController()
                    RecallDeckNavHost(navController = navController)
                    LaunchedEffect(Unit) {
                        if (openDueReview) {
                            navController.navigate(Destinations.study())
                        }
                    }
                }
            }
        }
    }
}
