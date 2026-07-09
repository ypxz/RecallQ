package com.recalldeck.app.ui.browser

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class CardBrowserUiState(
    val cards: List<CardEntity> = emptyList(),
    val query: String = "",
    val stateFilter: CardState? = null,
    val selectedIds: Set<Long> = emptySet(),
    val categories: List<CategoryEntity> = emptyList(),
    val loading: Boolean = true,
)

class CardBrowserViewModel(
    private val repo: DeckRepository,
    subjectId: Long?,
    categoryId: Long?,
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val stateFilter = MutableStateFlow<CardState?>(null)
    private val selectedIds = MutableStateFlow<Set<Long>>(emptySet())

    private val source = when {
        categoryId != null -> repo.observeCards(categoryId)
        subjectId != null -> repo.observeCardsBySubject(subjectId)
        else -> repo.observeAllCards()
    }

    val uiState: StateFlow<CardBrowserUiState> = combine(
        source,
        query,
        stateFilter,
        selectedIds,
    ) { cards, q, filter, selected ->
        val filtered = cards.filter { card ->
            (filter == null || card.state == filter) &&
                (
                    q.isBlank() ||
                        card.question.contains(q, ignoreCase = true) ||
                        card.answer.contains(q, ignoreCase = true)
                    )
        }
        CardBrowserUiState(
            cards = filtered,
            query = q,
            stateFilter = filter,
            selectedIds = selected intersect filtered.map { it.id }.toSet(),
            categories = repo.getAllCategories(),
            loading = false,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), CardBrowserUiState())

    fun setQuery(value: String) {
        query.value = value
    }

    fun setStateFilter(value: CardState?) {
        stateFilter.value = value
    }

    fun toggleSelection(id: Long) {
        selectedIds.value =
            if (id in selectedIds.value) selectedIds.value - id else selectedIds.value + id
    }

    fun clearSelection() {
        selectedIds.value = emptySet()
    }

    fun deleteSelected() {
        viewModelScope.launch {
            repo.deleteCards(selectedIds.value.toList())
            clearSelection()
        }
    }

    fun suspendSelected(suspend: Boolean) {
        viewModelScope.launch {
            repo.setCardsState(
                selectedIds.value.toList(),
                if (suspend) CardState.SUSPENDED else CardState.NEW,
            )
            clearSelection()
        }
    }

    fun moveSelected(categoryId: Long) {
        viewModelScope.launch {
            repo.moveCards(selectedIds.value.toList(), categoryId)
            clearSelection()
        }
    }

    companion object {
        fun factory(subjectId: Long?, categoryId: Long?) = containerViewModelFactory {
            CardBrowserViewModel(it.deckRepository, subjectId, categoryId)
        }
    }
}
