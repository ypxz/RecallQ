package com.recalldeck.app.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.SubjectEntity
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.ui.common.SUBJECT_COLORS
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class HomeUiState(
    val subjects: List<SubjectEntity> = emptyList(),
    val dueCount: Int = 0,
    val streak: Int = 0,
    val loading: Boolean = true,
)

class HomeViewModel(private val repo: DeckRepository) : ViewModel() {

    val uiState: StateFlow<HomeUiState> = combine(
        repo.observeSubjects(),
        repo.observeDueCount(System.currentTimeMillis()),
    ) { subjects, due ->
        HomeUiState(subjects = subjects, dueCount = due, streak = 0, loading = false)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HomeUiState())

    fun createSubject(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val color = SUBJECT_COLORS[uiState.value.subjects.size % SUBJECT_COLORS.size]
            repo.createSubject(name.trim(), color)
        }
    }

    fun updateSubject(subject: SubjectEntity, name: String, colorHex: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.updateSubject(subject, name.trim(), colorHex) }
    }

    fun deleteSubject(subject: SubjectEntity) {
        viewModelScope.launch { repo.deleteSubject(subject) }
    }

    companion object {
        val Factory = containerViewModelFactory { HomeViewModel(it.deckRepository) }
    }
}
