package com.recalldeck.app.ui.editor

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.srs.Cloze
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** One cloze index preview row: masked question and its hidden answer. */
data class ClozePreview(val index: Int, val question: String, val answer: String)

data class CardEditorUiState(
    val question: String = "",
    val answer: String = "",
    val hint: String = "",
    val mnemonic: String = "",
    val elaboration: String = "",
    val type: CardType = CardType.BASIC,
    val clozePreviews: List<ClozePreview> = emptyList(),
    val saved: Boolean = false,
    val canSave: Boolean = false,
    val isEdit: Boolean = false,
)

class CardEditorViewModel(
    private val repo: DeckRepository,
    private val cardId: Long?,
    private val categoryId: Long?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CardEditorUiState(isEdit = cardId != null))
    val uiState: StateFlow<CardEditorUiState> = _uiState.asStateFlow()

    private var existing: CardEntity? = null

    init {
        if (cardId != null) {
            viewModelScope.launch {
                repo.getCard(cardId)?.let { card ->
                    existing = card
                    _uiState.update {
                        it.copy(
                            question = card.question,
                            answer = card.answer,
                            hint = card.hint.orEmpty(),
                            mnemonic = card.mnemonic.orEmpty(),
                            elaboration = card.elaboration.orEmpty(),
                            type = card.type,
                            canSave = true,
                        )
                    }
                }
            }
        }
    }

    fun setType(type: CardType) {
        if (_uiState.value.isEdit) return
        updateAndValidate { it.copy(type = type) }
    }

    fun setQuestion(value: String) = updateAndValidate { it.copy(question = value) }
    fun setAnswer(value: String) = updateAndValidate { it.copy(answer = value) }
    fun setHint(value: String) = updateAndValidate { it.copy(hint = value) }
    fun setMnemonic(value: String) = updateAndValidate { it.copy(mnemonic = value) }
    fun setElaboration(value: String) = updateAndValidate { it.copy(elaboration = value) }

    private fun updateAndValidate(transform: (CardEditorUiState) -> CardEditorUiState) {
        _uiState.update { state ->
            val next = transform(state)
            val previews = if (next.type == CardType.CLOZE) {
                Cloze.indices(next.question).map { index ->
                    ClozePreview(
                        index = index,
                        question = Cloze.renderQuestion(next.question, index),
                        answer = Cloze.answerFor(next.question, index),
                    )
                }
            } else {
                emptyList()
            }
            val canSave = when (next.type) {
                CardType.BASIC -> next.question.isNotBlank() && next.answer.isNotBlank()
                CardType.CLOZE -> Cloze.isValid(next.question)
            }
            next.copy(clozePreviews = previews, canSave = canSave)
        }
    }

    fun save() {
        val state = _uiState.value
        if (!state.canSave) return
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val current = existing
            if (current != null) {
                repo.updateCard(
                    current.copy(
                        question = state.question.trim(),
                        answer = state.answer.trim(),
                        hint = state.hint.trim().ifBlank { null },
                        mnemonic = state.mnemonic.trim().ifBlank { null },
                        elaboration = state.elaboration.trim().ifBlank { null },
                    ),
                )
            } else if (categoryId != null) {
                if (state.type == CardType.CLOZE) {
                    val text = state.question.trim()
                    val indices = Cloze.indices(text)
                    val groupId = if (indices.size > 1) repo.nextGroupId() else null
                    repo.createCards(
                        indices.map { index ->
                            CardEntity(
                                categoryId = categoryId,
                                groupId = groupId,
                                type = CardType.CLOZE,
                                clozeIndex = index,
                                question = text,
                                answer = Cloze.answerFor(text, index),
                                hint = state.hint.trim().ifBlank { null },
                                mnemonic = state.mnemonic.trim().ifBlank { null },
                                elaboration = state.elaboration.trim().ifBlank { null },
                                dueAt = now,
                                createdAt = now,
                                updatedAt = now,
                            )
                        },
                    )
                } else {
                    repo.createCard(
                        CardEntity(
                            categoryId = categoryId,
                            type = CardType.BASIC,
                            question = state.question.trim(),
                            answer = state.answer.trim(),
                            hint = state.hint.trim().ifBlank { null },
                            mnemonic = state.mnemonic.trim().ifBlank { null },
                            elaboration = state.elaboration.trim().ifBlank { null },
                            dueAt = now,
                            createdAt = now,
                            updatedAt = now,
                        ),
                    )
                }
            }
            _uiState.update { it.copy(saved = true) }
        }
    }

    companion object {
        fun factory(cardId: Long?, categoryId: Long?) = containerViewModelFactory {
            CardEditorViewModel(it.deckRepository, cardId, categoryId)
        }
    }
}
