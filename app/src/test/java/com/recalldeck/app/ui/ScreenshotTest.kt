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
import com.recalldeck.app.ui.editor.ClozePreview
import com.recalldeck.app.ui.home.HomeScreen
import com.recalldeck.app.ui.home.HomeUiState
import com.recalldeck.app.data.repo.AppSettings
import com.recalldeck.app.data.stats.ForecastDay
import com.recalldeck.app.data.stats.HeatmapDay
import com.recalldeck.app.data.stats.StatsSnapshot
import com.recalldeck.app.data.stats.SubjectBreakdown
import com.recalldeck.app.importer.ParsedCard
import com.recalldeck.app.ui.importer.ImportPreset
import com.recalldeck.app.ui.importer.ImportScreen
import com.recalldeck.app.ui.importer.ImportUiState
import com.recalldeck.app.ui.settings.SettingsScreen
import com.recalldeck.app.ui.settings.SettingsUiState
import com.recalldeck.app.ui.stats.StatsScreen
import com.recalldeck.app.ui.stats.StatsUiState
import java.time.LocalDate
import com.recalldeck.app.srs.CustomOrder
import com.recalldeck.app.srs.Grade
import com.recalldeck.app.srs.QueueMode
import com.recalldeck.app.srs.TypeAnswer
import com.recalldeck.app.ui.study.StudyScreen
import com.recalldeck.app.ui.study.StudySetupScreen
import com.recalldeck.app.ui.study.StudySetupUiState
import com.recalldeck.app.ui.study.StudySummary
import com.recalldeck.app.ui.study.StudyUiState
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
                    onCustomStudy = {},
                    onStatsClick = {},
                    onImportClick = {},
                    onSettingsClick = {},
                )
            }
        }
    }

    @Test
    fun homeDark() {
        paparazzi.snapshot {
            RecallDeckTheme(darkTheme = true) {
                HomeScreen(
                    state = HomeUiState(subjects = subjects, dueCount = 12, streak = 4, loading = false),
                    onCreateSubject = {},
                    onSubjectClick = {},
                    onStudyAllDue = {},
                    onCustomStudy = {},
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
                    onCustomStudy = {},
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
                    onStudy = {},
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
                    onTypeChange = {},
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

    @Test
    fun cardEditorCloze() {
        paparazzi.snapshot {
            RecallDeckTheme {
                CardEditorScreen(
                    state = CardEditorUiState(
                        question = "The {{c1::mitochondria}} is the {{c2::powerhouse}} of the cell",
                        type = CardType.CLOZE,
                        clozePreviews = listOf(
                            ClozePreview(1, "The [...] is the powerhouse of the cell", "mitochondria"),
                            ClozePreview(2, "The mitochondria is the [...] of the cell", "powerhouse"),
                        ),
                        canSave = true,
                    ),
                    onBack = {},
                    onTypeChange = {},
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

    @Test
    fun studySetup() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudySetupScreen(
                    state = StudySetupUiState(
                        scopeLabel = "Biology",
                        dueCount = 8,
                        newCount = 5,
                        totalCount = 42,
                        mode = QueueMode.CUSTOM,
                        count = "25",
                        order = CustomOrder.HARDEST,
                        cram = true,
                        loading = false,
                    ),
                    onBack = {},
                    onModeChange = {},
                    onCountChange = {},
                    onOrderChange = {},
                    onCramChange = {},
                    onTypeAnswerChange = {},
                    onStart = {},
                )
            }
        }
    }

    private val studyBase = StudyUiState(
        loading = false,
        question = "What is the powerhouse of the cell?",
        answer = "Mitochondria",
        hint = "Organelle",
        position = 2,
        total = 10,
        intervalCaptions = mapOf(
            Grade.AGAIN to "3 min",
            Grade.HARD to "10 min",
            Grade.GOOD to "3 d",
            Grade.EASY to "7 d",
        ),
        canUndo = true,
    )

    @Test
    fun studyQuestion() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = studyBase,
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studyAnswerRevealed() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = studyBase.copy(
                        revealed = true,
                        mnemonic = "Mighty mito makes energy",
                        elaboration = "Site of aerobic respiration and ATP production.",
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studyAnswerRevealedDark() {
        paparazzi.snapshot {
            RecallDeckTheme(darkTheme = true) {
                StudyScreen(
                    state = studyBase.copy(
                        revealed = true,
                        mnemonic = "Mighty mito makes energy",
                        elaboration = "Site of aerobic respiration and ATP production.",
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studyClozeQuestion() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = studyBase.copy(
                        question = "The [...] is the powerhouse of the cell.",
                        answer = "The mitochondria is the powerhouse of the cell.",
                        isCloze = true,
                        hint = null,
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studyTypeAnswer() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = studyBase.copy(
                        typeAnswer = true,
                        typedInput = "mitochondira",
                        hint = null,
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studyTypeAnswerAlmost() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = studyBase.copy(
                        typeAnswer = true,
                        typedInput = "mitochondira",
                        revealed = true,
                        verdict = TypeAnswer.Verdict.ALMOST,
                        hint = null,
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun studySummary() {
        paparazzi.snapshot {
            RecallDeckTheme {
                StudyScreen(
                    state = StudyUiState(
                        loading = false,
                        finished = true,
                        canUndo = true,
                        summary = StudySummary(reviewed = 12, again = 2, hard = 3, good = 5, easy = 2),
                    ),
                    onBack = {},
                    onReveal = {},
                    onRevealHint = {},
                    onGrade = {},
                    onUndo = {},
                    onSuspend = {},
                    onTypedInputChange = {},
                    onCheckTypedAnswer = {},
                )
            }
        }
    }

    @Test
    fun importEmpty() {
        paparazzi.snapshot {
            RecallDeckTheme {
                ImportScreen(
                    state = ImportUiState(),
                    onBack = {},
                    onPickFile = {},
                    onPresetChange = {},
                    onToggleCard = { _ -> },
                    onEditCard = { _, _, _ -> },
                    onSubjectSelect = {},
                    onCategorySelect = {},
                    onSave = {},
                )
            }
        }
    }

    @Test
    fun importPreview() {
        paparazzi.snapshot {
            RecallDeckTheme {
                ImportScreen(
                    state = ImportUiState(
                        fileName = "biology-notes.pdf",
                        preset = ImportPreset.QA_PAIRS,
                        cards = listOf(
                            ParsedCard("What is the powerhouse of the cell?", "Mitochondria"),
                            ParsedCard("Define osmosis", "Diffusion of water across a membrane"),
                            ParsedCard("What does DNA stand for?", "Deoxyribonucleic acid", enabled = false),
                        ),
                        subjects = subjects,
                        categories = listOf(
                            CategoryEntity(id = 1, subjectId = 1, name = "Cell structure", position = 0, createdAt = 0),
                        ),
                        selectedSubjectId = 1,
                        selectedCategoryId = 1,
                    ),
                    onBack = {},
                    onPickFile = {},
                    onPresetChange = {},
                    onToggleCard = { _ -> },
                    onEditCard = { _, _, _ -> },
                    onSubjectSelect = {},
                    onCategorySelect = {},
                    onSave = {},
                )
            }
        }
    }

    private val statsSnapshot = run {
        val start = LocalDate.of(2026, 4, 15)
        StatsSnapshot(
            currentStreakDays = 7,
            heatmap = List(84) { i -> HeatmapDay(start.plusDays(i.toLong()), (i * 7) % 11) },
            forecast = List(31) { i -> ForecastDay(start.plusDays(84L + i), ((i * 5) % 17)) },
            retentionPercent = 87.5,
            subjectBreakdown = listOf(
                SubjectBreakdown(
                    subjectId = 1,
                    subjectName = "Biology",
                    stateCounts = mapOf(
                        CardState.NEW to 10,
                        CardState.LEARNING to 4,
                        CardState.REVIEW to 28,
                        CardState.SUSPENDED to 2,
                    ),
                ),
                SubjectBreakdown(
                    subjectId = 2,
                    subjectName = "Organic Chemistry",
                    stateCounts = mapOf(
                        CardState.NEW to 20,
                        CardState.REVIEW to 6,
                    ),
                ),
            ),
        )
    }

    @Test
    fun statsDark() {
        paparazzi.snapshot {
            RecallDeckTheme(darkTheme = true) {
                StatsScreen(
                    state = StatsUiState(loading = false, snapshot = statsSnapshot),
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun stats() {
        val start = LocalDate.of(2026, 4, 15)
        paparazzi.snapshot {
            RecallDeckTheme {
                StatsScreen(
                    state = StatsUiState(
                        loading = false,
                        snapshot = StatsSnapshot(
                            currentStreakDays = 7,
                            heatmap = List(84) { i ->
                                HeatmapDay(start.plusDays(i.toLong()), (i * 7) % 11)
                            },
                            forecast = List(31) { i ->
                                ForecastDay(start.plusDays(84L + i), ((i * 5) % 17))
                            },
                            retentionPercent = 87.5,
                            subjectBreakdown = listOf(
                                SubjectBreakdown(
                                    subjectId = 1,
                                    subjectName = "Biology",
                                    stateCounts = mapOf(
                                        CardState.NEW to 10,
                                        CardState.LEARNING to 4,
                                        CardState.REVIEW to 28,
                                        CardState.SUSPENDED to 2,
                                    ),
                                ),
                                SubjectBreakdown(
                                    subjectId = 2,
                                    subjectName = "Organic Chemistry",
                                    stateCounts = mapOf(
                                        CardState.NEW to 20,
                                        CardState.REVIEW to 6,
                                    ),
                                ),
                            ),
                        ),
                    ),
                    onBack = {},
                )
            }
        }
    }

    @Test
    fun settings() {
        paparazzi.snapshot {
            RecallDeckTheme {
                SettingsScreen(
                    state = SettingsUiState(
                        settings = AppSettings(
                            retentionTarget = 0.9,
                            newPerDay = 20,
                            reminderEnabled = true,
                            reminderHour = 18,
                            reminderMinute = 30,
                        ),
                        subjects = subjects,
                    ),
                    onBack = {},
                    onRetentionTargetChange = {},
                    onNewPerDayChange = {},
                    onThemeModeChange = {},
                    onAutoSuspendChange = {},
                    onAgainDelayChange = {},
                    onNewHardDelayChange = {},
                    onNewGoodDelayChange = {},
                    onLearningHardDelayChange = {},
                    onReminderEnabledChange = {},
                    onPickReminderTime = {},
                    onExportBackup = {},
                    onImportBackup = {},
                    onExportCsv = {},
                    onDismissMessage = {},
                )
            }
        }
    }
}
