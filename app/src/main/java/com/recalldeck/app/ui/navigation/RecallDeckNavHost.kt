package com.recalldeck.app.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.recalldeck.app.ui.browser.CardBrowserScreen
import com.recalldeck.app.ui.browser.CardBrowserViewModel
import com.recalldeck.app.ui.editor.CardEditorScreen
import com.recalldeck.app.ui.editor.CardEditorViewModel
import com.recalldeck.app.ui.home.HomeScreen
import com.recalldeck.app.ui.home.HomeViewModel
import com.recalldeck.app.ui.subject.SubjectDetailScreen
import com.recalldeck.app.ui.subject.SubjectDetailViewModel

private fun Long.orNullIfUnset(): Long? = takeIf { it >= 0 }

@Composable
fun RecallDeckNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Destinations.HOME) {
        composable(Destinations.HOME) {
            val viewModel: HomeViewModel = viewModel(factory = HomeViewModel.Factory)
            val state by viewModel.uiState.collectAsState()
            HomeScreen(
                state = state,
                onCreateSubject = viewModel::createSubject,
                onSubjectClick = { navController.navigate(Destinations.subjectDetail(it)) },
                onStudyAllDue = { navController.navigate(Destinations.STUDY) },
                onStatsClick = { navController.navigate(Destinations.STATS) },
                onImportClick = { navController.navigate(Destinations.IMPORT) },
                onSettingsClick = { navController.navigate(Destinations.SETTINGS) },
            )
        }
        composable(
            Destinations.SUBJECT_DETAIL,
            arguments = listOf(navArgument("subjectId") { type = NavType.LongType }),
        ) { entry ->
            val subjectId = entry.arguments?.getLong("subjectId") ?: return@composable
            val viewModel: SubjectDetailViewModel =
                viewModel(factory = SubjectDetailViewModel.factory(subjectId))
            val state by viewModel.uiState.collectAsState()
            SubjectDetailScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onCreateCategory = viewModel::createCategory,
                onCategoryClick = {
                    navController.navigate(Destinations.cardBrowser(categoryId = it))
                },
                onBrowseAll = {
                    navController.navigate(Destinations.cardBrowser(subjectId = subjectId))
                },
            )
        }
        composable(
            Destinations.CARD_BROWSER,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val subjectId = entry.arguments?.getLong("subjectId")?.orNullIfUnset()
            val categoryId = entry.arguments?.getLong("categoryId")?.orNullIfUnset()
            val viewModel: CardBrowserViewModel =
                viewModel(factory = CardBrowserViewModel.factory(subjectId, categoryId))
            val state by viewModel.uiState.collectAsState()
            CardBrowserScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onQueryChange = viewModel::setQuery,
                onStateFilterChange = viewModel::setStateFilter,
                onToggleSelection = viewModel::toggleSelection,
                onClearSelection = viewModel::clearSelection,
                onDeleteSelected = viewModel::deleteSelected,
                onSuspendSelected = viewModel::suspendSelected,
                onMoveSelected = viewModel::moveSelected,
                onCardClick = { navController.navigate(Destinations.cardEditor(cardId = it)) },
                onAddCard = categoryId?.let {
                    { navController.navigate(Destinations.cardEditor(categoryId = it)) }
                },
            )
        }
        composable(
            Destinations.CARD_EDITOR,
            arguments = listOf(
                navArgument("cardId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val cardId = entry.arguments?.getLong("cardId")?.orNullIfUnset()
            val categoryId = entry.arguments?.getLong("categoryId")?.orNullIfUnset()
            val viewModel: CardEditorViewModel =
                viewModel(factory = CardEditorViewModel.factory(cardId, categoryId))
            val state by viewModel.uiState.collectAsState()
            CardEditorScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onQuestionChange = viewModel::setQuestion,
                onAnswerChange = viewModel::setAnswer,
                onHintChange = viewModel::setHint,
                onMnemonicChange = viewModel::setMnemonic,
                onElaborationChange = viewModel::setElaboration,
                onSave = viewModel::save,
            )
        }
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
