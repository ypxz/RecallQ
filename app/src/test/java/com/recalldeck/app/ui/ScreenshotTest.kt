package com.recalldeck.app.ui

import app.cash.paparazzi.DeviceConfig
import app.cash.paparazzi.Paparazzi
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.SubjectEntity
import com.recalldeck.app.ui.browser.CardBrowserScreen
import com.recalldeck.app.ui.browser.CardBrowserUiState
import com.recalldeck.app.ui.editor.CardEditorScreen
import com.recalldeck.app.ui.editor.CardEditorUiState
import com.recalldeck.app.ui.home.HomeScreen
import com.recalldeck.app.ui.home.HomeUiState
import com.recalldeck.app.ui.subject.CategoryRow
import com.recalldeck.app.ui.subject.SubjectDetailScreen
import com.recalldeck.app.ui.subject.SubjectDetailUiState
import com.recalldeck.app.ui.theme.RecallDeckTheme
import org.junit.Rule
import org.junit.Test

class ScreenshotTest {

    @get:Rule
    val paparazzi = Paparazzi(deviceConfig = DeviceConfig.PIXEL_6)

    private val subjects = listOf(
        SubjectEntity(id = 1, name = "Biology", colorHex = "#26A69A", position = 0, createdAt = 0),
        SubjectEntity(id = 2, name = "Organic Chemistry", colorHex = "#AB47BC", position = 1, createdAt = 0),
        SubjectEntity(id = 3, name = "Statistics", colorHex = "#29B6F6", position = 2, createdAt = 0),
    )

    private fun card(id: Long, question: String, answer: String, state: CardState) = CardEntity(
        id = id,
        categoryId = 1,
        type = CardType.BASIC,
        question = question,
        answer = answer,
        state = state,
        dueAt = 0,
        createdAt = 0,
        updatedAt = 0,
    )

    @Test
    fun home() {
        paparazzi.snapshot {
            RecallDeckTheme {
                HomeScreen(
                    state = HomeUiState(subjects = subjects, dueCount = 12, streak = 4, loading = false),
                    onCreateSubject = {},
                    onSubjectClick = {},
                    onStudyAllDue = {},
                    onStatsClick = {},
                    onImportClick = {},
                    onSettingsClick = {},
                )
            }
        }
    }

    @Test
    fun homeEmpty() {
        paparazzi.snapshot {
            RecallDeckTheme {
                HomeScreen(
                    state = HomeUiState(loading = false),
                    onCreateSubject = {},
                    onSubjectClick = {},
                    onStudyAllDue = {},
                    onStatsClick = {},
                    onImportClick = {},
                    onSettingsClick = {},
                )
            }
        }
    }

    @Test
    fun subjectDetail() {
        paparazzi.snapshot {
            RecallDeckTheme {
                SubjectDetailScreen(
                    state = SubjectDetailUiState(
                        subjectName = "Biology",
                        categories = listOf(
                            CategoryRow(
                                CategoryEntity(id = 1, subjectId = 1, name = "Cell structure", position = 0, createdAt = 0),
                                cardCount = 24,
                                dueCount = 6,
                            ),
                            CategoryRow(
                                CategoryEntity(id = 2, subjectId = 1, name = "Genetics", position = 1, createdAt = 0),
                                cardCount = 12,
                                dueCount = 0,
                            ),
                        ),
                        loading = false,
                    ),
                    onBack = {},
                    onCreateCategory = {},
                    onCategoryClick = {},
                    onBrowseAll = {},
                )
            }
        }
    }

    @Test
    fun cardBrowser() {
        paparazzi.snapshot {
            RecallDeckTheme {
                CardBrowserScreen(
                    state = CardBrowserUiState(
                        cards = listOf(
                            card(1, "What is the powerhouse of the cell?", "Mitochondria", CardState.REVIEW),
                            card(2, "Define osmosis", "Diffusion of water across a membrane", CardState.NEW),
                            card(3, "What does DNA stand for?", "Deoxyribonucleic acid", CardState.SUSPENDED),
                        ),
                        selectedIds = setOf(2L),
                        loading = false,
                    ),
                    onBack = {},
                    onQueryChange = {},
                    onStateFilterChange = {},
                    onToggleSelection = {},
                    onClearSelection = {},
                    onDeleteSelected = {},
                    onSuspendSelected = {},
                    onMoveSelected = {},
                    onCardClick = {},
                    onAddCard = {},
                )
            }
        }
    }

    @Test
    fun cardEditor() {
        paparazzi.snapshot {
            RecallDeckTheme {
                CardEditorScreen(
                    state = CardEditorUiState(
                        question = "What is the function of ribosomes?",
                        answer = "Protein synthesis",
                        hint = "Think translation",
                        canSave = true,
                    ),
                    onBack = {},
                    onQuestionChange = {},
                    onAnswerChange = {},
                    onHintChange = {},
                    onMnemonicChange = {},
                    onElaborationChange = {},
                    onSave = {},
                )
            }
        }
    }
}
