package com.recalldeck.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Navigation shell. Screens are placeholders until their dedicated PRs land.
 */
@Composable
fun RecallDeckNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) { PlaceholderScreen("Home") }
        composable(Destinations.SUBJECT_DETAIL) { PlaceholderScreen("Subject detail") }
        composable(Destinations.CARD_BROWSER) { PlaceholderScreen("Card browser") }
        composable(Destinations.CARD_EDITOR) { PlaceholderScreen("Card editor") }
        composable(Destinations.STUDY_SETUP) { PlaceholderScreen("Study setup") }
        composable(Destinations.STUDY) { PlaceholderScreen("Study") }
        composable(Destinations.IMPORT) { PlaceholderScreen("Import") }
        composable(Destinations.STATS) { PlaceholderScreen("Stats") }
        composable(Destinations.SETTINGS) { PlaceholderScreen("Settings") }
    }
}

@Composable
private fun PlaceholderScreen(name: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = name)
    }
}
