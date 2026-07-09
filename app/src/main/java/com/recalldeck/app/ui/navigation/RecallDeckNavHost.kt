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
import com.recalldeck.app.data.repo.StudyScope
import com.recalldeck.app.srs.CustomOrder
import com.recalldeck.app.srs.QueueMode
import com.recalldeck.app.srs.QueueOptions
import com.recalldeck.app.ui.study.StudyConfig
import com.recalldeck.app.ui.study.StudyScreen
import com.recalldeck.app.ui.study.StudySetupScreen
import com.recalldeck.app.ui.study.StudySetupViewModel
import com.recalldeck.app.ui.study.StudyViewModel
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
                onStudyAllDue = { navController.navigate(Destinations.study()) },
                onCustomStudy = { navController.navigate(Destinations.studySetup()) },
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
                onStudy = {
                    navController.navigate(Destinations.studySetup(subjectId = subjectId))
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
                onTypeChange = viewModel::setType,
                onQuestionChange = viewModel::setQuestion,
                onAnswerChange = viewModel::setAnswer,
                onHintChange = viewModel::setHint,
                onMnemonicChange = viewModel::setMnemonic,
                onElaborationChange = viewModel::setElaboration,
                onSave = viewModel::save,
            )
        }
        composable(
            Destinations.STUDY_SETUP,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
            ),
        ) { entry ->
            val subjectId = entry.arguments?.getLong("subjectId")?.orNullIfUnset()
            val categoryId = entry.arguments?.getLong("categoryId")?.orNullIfUnset()
            val viewModel: StudySetupViewModel = viewModel(
                factory = StudySetupViewModel.factory(StudyScope(subjectId, categoryId)),
            )
            val state by viewModel.uiState.collectAsState()
            StudySetupScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onModeChange = viewModel::setMode,
                onCountChange = viewModel::setCount,
                onOrderChange = viewModel::setOrder,
                onCramChange = viewModel::setCram,
                onTypeAnswerChange = viewModel::setTypeAnswer,
                onStart = {
                    val s = viewModel.uiState.value
                    navController.navigate(
                        Destinations.study(
                            subjectId = subjectId,
                            categoryId = categoryId,
                            mode = s.mode.name,
                            count = s.count.toIntOrNull() ?: 20,
                            order = s.order.name,
                            cram = s.cram,
                            typeAnswer = s.typeAnswer,
                        ),
                    )
                },
            )
        }
        composable(
            Destinations.STUDY,
            arguments = listOf(
                navArgument("subjectId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("categoryId") { type = NavType.LongType; defaultValue = -1L },
                navArgument("mode") { type = NavType.StringType; defaultValue = "DUE" },
                navArgument("count") { type = NavType.IntType; defaultValue = 20 },
                navArgument("order") { type = NavType.StringType; defaultValue = "RANDOM" },
                navArgument("cram") { type = NavType.BoolType; defaultValue = false },
                navArgument("typeAnswer") { type = NavType.BoolType; defaultValue = false },
            ),
        ) { entry ->
            val args = entry.arguments
            val mode = QueueMode.valueOf(args?.getString("mode") ?: "DUE")
            val count = args?.getInt("count") ?: 20
            val config = StudyConfig(
                scope = StudyScope(
                    subjectId = args?.getLong("subjectId")?.orNullIfUnset(),
                    categoryId = args?.getLong("categoryId")?.orNullIfUnset(),
                ),
                options = QueueOptions(
                    mode = mode,
                    count = if (mode == QueueMode.DUE) null else count,
                    order = CustomOrder.valueOf(args?.getString("order") ?: "RANDOM"),
                ),
                cram = args?.getBoolean("cram") ?: false,
                typeAnswer = args?.getBoolean("typeAnswer") ?: false,
            )
            val viewModel: StudyViewModel = viewModel(factory = StudyViewModel.factory(config))
            val state by viewModel.uiState.collectAsState()
            StudyScreen(
                state = state,
                onBack = { navController.popBackStack() },
                onReveal = viewModel::reveal,
                onRevealHint = viewModel::revealHint,
                onGrade = viewModel::grade,
                onUndo = viewModel::undo,
                onSuspend = viewModel::suspendCurrent,
                onTypedInputChange = viewModel::setTypedInput,
                onCheckTypedAnswer = viewModel::checkTypedAnswer,
            )
        }
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
