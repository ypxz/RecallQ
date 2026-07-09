package com.recalldeck.app.ui.study

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.data.repo.StudyRepository
import com.recalldeck.app.data.repo.StudyScope
import com.recalldeck.app.srs.CustomOrder
import com.recalldeck.app.srs.QueueMode
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class StudySetupUiState(
    val scopeLabel: String = "All subjects",
    val dueCount: Int = 0,
    val newCount: Int = 0,
    val totalCount: Int = 0,
    val mode: QueueMode = QueueMode.DUE,
    val count: String = "20",
    val order: CustomOrder = CustomOrder.RANDOM,
    val cram: Boolean = false,
    val typeAnswer: Boolean = false,
    val loading: Boolean = true,
)

class StudySetupViewModel(
    private val studyRepo: StudyRepository,
    private val deckRepo: DeckRepository,
    private val scope: StudyScope,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(StudySetupUiState())
    val uiState: StateFlow<StudySetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val label = when {
                scope.categoryId != null ->
                    deckRepo.getCategory(scope.categoryId)?.name ?: "Category"
                scope.subjectId != null ->
                    deckRepo.getSubject(scope.subjectId)?.name ?: "Subject"
                else -> "All subjects"
            }
            val cards = studyRepo.getCards(scope)
            val time = now()
            _uiState.update {
                it.copy(
                    scopeLabel = label,
                    dueCount = cards.count {
                        c ->
                        (c.state == CardState.LEARNING || c.state == CardState.REVIEW) && c.dueAt <= time
                    },
                    newCount = cards.count { c -> c.state == CardState.NEW },
                    totalCount = cards.count { c -> c.state != CardState.SUSPENDED },
                    loading = false,
                )
            }
        }
    }

    fun setMode(mode: QueueMode) = _uiState.update { it.copy(mode = mode) }
    fun setCount(value: String) = _uiState.update { it.copy(count = value.filter(Char::isDigit).take(4)) }
    fun setOrder(order: CustomOrder) = _uiState.update { it.copy(order = order) }
    fun setCram(value: Boolean) = _uiState.update { it.copy(cram = value) }
    fun setTypeAnswer(value: Boolean) = _uiState.update { it.copy(typeAnswer = value) }

    companion object {
        fun factory(scope: StudyScope) = containerViewModelFactory {
            StudySetupViewModel(it.studyRepository, it.deckRepository, scope)
        }
    }
}
