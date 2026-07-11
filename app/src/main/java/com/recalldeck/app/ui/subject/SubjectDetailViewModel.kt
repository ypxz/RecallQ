package com.recalldeck.app.ui.subject

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CategoryRow(
    val category: CategoryEntity,
    val cardCount: Int,
    val dueCount: Int,
)

data class SubjectDetailUiState(
    val subjectName: String = "",
    val categories: List<CategoryRow> = emptyList(),
    val loading: Boolean = true,
)

class SubjectDetailViewModel(
    private val repo: DeckRepository,
    private val subjectId: Long,
) : ViewModel() {

    val uiState: StateFlow<SubjectDetailUiState> = combine(
        repo.observeCategories(subjectId),
        repo.observeCardsBySubject(subjectId),
    ) { categories, cards ->
        val now = System.currentTimeMillis()
        SubjectDetailUiState(
            subjectName = repo.getSubject(subjectId)?.name ?: "",
            categories = categories.map { category ->
                val inCategory = cards.filter { it.categoryId == category.id }
                CategoryRow(
                    category = category,
                    cardCount = inCategory.size,
                    dueCount = inCategory.count {
                        (it.state == CardState.LEARNING || it.state == CardState.REVIEW) &&
                            it.dueAt <= now
                    },
                )
            },
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), SubjectDetailUiState())

    fun createCategory(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.createCategory(subjectId, name.trim()) }
    }

    fun renameCategory(category: CategoryEntity, name: String) {
        if (name.isBlank()) return
        viewModelScope.launch { repo.renameCategory(category, name.trim()) }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch { repo.deleteCategory(category) }
    }

    companion object {
        fun factory(subjectId: Long) = containerViewModelFactory {
            SubjectDetailViewModel(it.deckRepository, subjectId)
        }
    }
}
