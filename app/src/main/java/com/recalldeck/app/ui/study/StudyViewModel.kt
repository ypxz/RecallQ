package com.recalldeck.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.repo.AppSettings
import com.recalldeck.app.data.repo.SettingsRepository
import com.recalldeck.app.data.repo.StudyRepository
import com.recalldeck.app.data.repo.StudyScope
import com.recalldeck.app.srs.Cloze
import com.recalldeck.app.srs.Grade
import com.recalldeck.app.srs.QueueBuilder
import com.recalldeck.app.srs.QueueMode
import com.recalldeck.app.srs.QueueOptions
import com.recalldeck.app.srs.Scheduler
import com.recalldeck.app.srs.StudySession
import com.recalldeck.app.srs.TypeAnswer
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Everything needed to launch a study session, encoded into the study route. */
data class StudyConfig(
    val scope: StudyScope = StudyScope(),
    val options: QueueOptions = QueueOptions(mode = QueueMode.DUE),
    val cram: Boolean = false,
    val typeAnswer: Boolean = false,
)

data class StudySummary(
    val reviewed: Int = 0,
    val again: Int = 0,
    val hard: Int = 0,
    val good: Int = 0,
    val easy: Int = 0,
)

data class StudyUiState(
    val loading: Boolean = true,
    val emptyQueue: Boolean = false,
    val finished: Boolean = false,
    /** Rendered question text (cloze already masked). */
    val question: String = "",
    /** Rendered answer text (cloze fully revealed). */
    val answer: String = "",
    val hint: String? = null,
    val mnemonic: String? = null,
    val elaboration: String? = null,
    val isCloze: Boolean = false,
    val revealed: Boolean = false,
    val hintRevealed: Boolean = false,
    /** Grade button captions, e.g. AGAIN -> "3 min". */
    val intervalCaptions: Map<Grade, String> = emptyMap(),
    val position: Int = 0,
    val total: Int = 0,
    val canUndo: Boolean = false,
    val cram: Boolean = false,
    val typeAnswer: Boolean = false,
    val typedInput: String = "",
    val verdict: TypeAnswer.Verdict? = null,
    val summary: StudySummary = StudySummary(),
)

class StudyViewModel(
    private val studyRepo: StudyRepository,
    settingsRepo: SettingsRepository,
    private val config: StudyConfig,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudyUiState(cram = config.cram, typeAnswer = config.typeAnswer))
    val uiState: StateFlow<StudyUiState> = _uiState.asStateFlow()

    private var settings = AppSettings()
    private var session = StudySession(queue = emptyList())
    private val gradeHistory = mutableListOf<Grade>()
    private val settingsFlow = settingsRepo.settings

    init {
        viewModelScope.launch {
            settings = settingsFlow.first()
            val cards = studyRepo.getCards(config.scope)
            val options = if (config.options.mode == QueueMode.DUE) {
                config.options.copy(newLimit = settings.newPerDay)
            } else {
                config.options
            }
            val queue = QueueBuilder.buildQueue(cards, options, now())
            session = StudySession(queue = queue)
            if (queue.isEmpty()) {
                _uiState.update { it.copy(loading = false, emptyQueue = true) }
            } else {
                showCurrent()
            }
        }
    }

    fun reveal() = _uiState.update { it.copy(revealed = true) }

    fun revealHint() = _uiState.update { it.copy(hintRevealed = true) }

    fun setTypedInput(value: String) = _uiState.update { it.copy(typedInput = value) }

    fun checkTypedAnswer() {
        val card = session.currentCard ?: return
        val expected = typeAnswerExpected(card)
        _uiState.update {
            it.copy(revealed = true, verdict = TypeAnswer.judge(it.typedInput, expected))
        }
    }

    fun grade(grade: Grade) {
        val card = session.currentCard ?: return
        viewModelScope.launch {
            val result = Scheduler.grade(
                card = card,
                grade = grade,
                now = now(),
                settings = settings,
                countedTowardSchedule = !config.cram,
            )
            var persistedCard = result.updatedCard
            val logId = studyRepo.persistGrade(persistedCard, result.reviewLog)
            if (!config.cram && settings.autoSuspendMastered &&
                persistedCard.state == CardState.REVIEW &&
                Scheduler.isMastered(studyRepo.logsFor(card.id))
            ) {
                persistedCard = persistedCard.copy(state = CardState.SUSPENDED)
                studyRepo.updateCard(persistedCard)
            }
            session = session.afterGrade(result, grade, logId)
            gradeHistory.add(grade)
            _uiState.update { it.copy(summary = summaryFrom(gradeHistory)) }
            showCurrent()
        }
    }

    fun undo() {
        val (restored, action) = session.undo() ?: return
        viewModelScope.launch {
            studyRepo.undoGrade(action.restoreCard, action.deleteReviewLogId)
            session = restored
            if (gradeHistory.isNotEmpty()) gradeHistory.removeAt(gradeHistory.lastIndex)
            _uiState.update { it.copy(finished = false, summary = summaryFrom(gradeHistory)) }
            showCurrent()
        }
    }

    /** Suspends the current card ("Never ask again") and moves on without grading. */
    fun suspendCurrent() {
        val card = session.currentCard ?: return
        viewModelScope.launch {
            studyRepo.updateCard(card.copy(state = CardState.SUSPENDED, updatedAt = now()))
            session = session.copy(queue = session.queue.filterIndexed { i, _ -> i != session.position })
            showCurrent()
        }
    }

    private fun summaryFrom(grades: List<Grade>): StudySummary = StudySummary(
        reviewed = grades.size,
        again = grades.count { it == Grade.AGAIN },
        hard = grades.count { it == Grade.HARD },
        good = grades.count { it == Grade.GOOD },
        easy = grades.count { it == Grade.EASY },
    )

    private fun showCurrent() {
        val card = session.currentCard
        if (card == null) {
            _uiState.update { it.copy(loading = false, finished = true, canUndo = session.history.isNotEmpty()) }
            return
        }
        val previews = Scheduler.previewIntervals(card, now(), settings)
        _uiState.update {
            it.copy(
                loading = false,
                finished = false,
                question = displayQuestion(card),
                answer = displayAnswer(card),
                hint = card.hint,
                mnemonic = card.mnemonic,
                elaboration = card.elaboration,
                isCloze = card.type == CardType.CLOZE,
                revealed = false,
                hintRevealed = false,
                intervalCaptions = previews.mapValues { (_, v) -> v.caption },
                position = session.position,
                total = session.queue.size,
                canUndo = session.history.isNotEmpty(),
                typedInput = "",
                verdict = null,
            )
        }
    }

    private fun displayQuestion(card: CardEntity): String =
        if (card.type == CardType.CLOZE) Cloze.renderQuestion(card.question, card.clozeIndex ?: 1)
        else card.question

    private fun displayAnswer(card: CardEntity): String =
        if (card.type == CardType.CLOZE) Cloze.renderAnswer(card.question) else card.answer

    private fun typeAnswerExpected(card: CardEntity): String =
        if (card.type == CardType.CLOZE) Cloze.answerFor(card.question, card.clozeIndex ?: 1)
        else card.answer

    companion object {
        fun factory(config: StudyConfig) = containerViewModelFactory {
            StudyViewModel(it.studyRepository, it.settingsRepository, config)
        }
    }
}
