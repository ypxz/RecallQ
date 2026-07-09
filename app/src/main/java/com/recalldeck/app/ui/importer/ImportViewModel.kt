package com.recalldeck.app.ui.importer

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CardType
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.db.SubjectEntity
import com.recalldeck.app.data.repo.DeckRepository
import com.recalldeck.app.importer.CsvParser
import com.recalldeck.app.importer.ImportResult
import com.recalldeck.app.importer.ParsedCard
import com.recalldeck.app.importer.ParserPreset
import com.recalldeck.app.importer.PdfExtractionResult
import com.recalldeck.app.importer.PdfTextExtractor
import com.recalldeck.app.importer.QaHeuristicParser
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.InputStream

/** User-facing choice of parsing format on the import screen. */
enum class ImportPreset(val label: String) {
    AUTO("Auto-detect"),
    QA_PAIRS("Q:/A: pairs"),
    NUMBERED("Numbered"),
    QUESTION_MARK("Question?"),
    TERM_DEFINITION("Term - Definition"),
    CSV("CSV"),
    ;

    fun toParserPreset(): ParserPreset? = when (this) {
        AUTO, CSV -> null
        QA_PAIRS -> ParserPreset.QA_PAIRS
        NUMBERED -> ParserPreset.NUMBERED
        QUESTION_MARK -> ParserPreset.QUESTION_MARK
        TERM_DEFINITION -> ParserPreset.TERM_DEFINITION
    }
}

data class ImportUiState(
    val fileName: String? = null,
    val preset: ImportPreset = ImportPreset.AUTO,
    val cards: List<ParsedCard> = emptyList(),
    val error: String? = null,
    val subjects: List<SubjectEntity> = emptyList(),
    val categories: List<CategoryEntity> = emptyList(),
    val selectedSubjectId: Long? = null,
    val selectedCategoryId: Long? = null,
    val saving: Boolean = false,
    val savedCount: Int? = null,
) {
    val enabledCount: Int get() = cards.count { it.enabled }
    val canSave: Boolean
        get() = enabledCount > 0 && selectedCategoryId != null && !saving && savedCount == null
}

class ImportViewModel(
    private val deckRepo: DeckRepository,
    private val pdfExtractor: PdfTextExtractor,
    private val now: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImportUiState())
    val uiState: StateFlow<ImportUiState> = _uiState.asStateFlow()

    private var extractedText: String? = null

    init {
        viewModelScope.launch {
            deckRepo.observeSubjects().collect { subjects ->
                _uiState.update { it.copy(subjects = subjects) }
            }
        }
    }

    fun selectSubject(subjectId: Long) {
        _uiState.update {
            it.copy(selectedSubjectId = subjectId, selectedCategoryId = null, categories = emptyList())
        }
        viewModelScope.launch {
            deckRepo.observeCategories(subjectId).collect { categories ->
                _uiState.update { state ->
                    if (state.selectedSubjectId == subjectId) state.copy(categories = categories) else state
                }
            }
        }
    }

    fun selectCategory(categoryId: Long) =
        _uiState.update { it.copy(selectedCategoryId = categoryId) }

    /** Loads and parses a picked file. [openStream] is invoked on the IO dispatcher. */
    fun loadFile(displayName: String, openStream: () -> InputStream?) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(fileName = displayName, error = null, cards = emptyList(), savedCount = null)
            }
            val isPdf = displayName.endsWith(".pdf", ignoreCase = true)
            val isCsv = displayName.endsWith(".csv", ignoreCase = true)
            val result = withContext(Dispatchers.IO) {
                val stream = openStream() ?: return@withContext "Couldn't open the selected file." to null
                stream.use { input ->
                    if (isPdf) {
                        when (val extraction = pdfExtractor.extractText(input)) {
                            is PdfExtractionResult.Success -> null to extraction.text
                            is PdfExtractionResult.Failure -> extraction.message to null
                        }
                    } else {
                        try {
                            null to input.readBytes().toString(Charsets.UTF_8)
                        } catch (e: Exception) {
                            "Couldn't read the selected file." to null
                        }
                    }
                }
            }
            val (error, text) = result
            if (error != null || text == null) {
                _uiState.update { it.copy(error = error ?: "Couldn't read the selected file.") }
                return@launch
            }
            extractedText = text
            if (isCsv) {
                _uiState.update { it.copy(preset = ImportPreset.CSV) }
            }
            reparse()
        }
    }

    fun setPreset(preset: ImportPreset) {
        _uiState.update { it.copy(preset = preset) }
        reparse()
    }

    fun toggleCard(index: Int) = _uiState.update {
        it.copy(
            cards = it.cards.mapIndexed { i, card ->
                if (i == index) card.copy(enabled = !card.enabled) else card
            },
        )
    }

    fun editCard(index: Int, question: String, answer: String) = _uiState.update {
        it.copy(
            cards = it.cards.mapIndexed { i, card ->
                if (i == index) card.copy(question = question, answer = answer) else card
            },
        )
    }

    fun save() {
        val state = _uiState.value
        val categoryId = state.selectedCategoryId ?: return
        val toSave = state.cards.filter { it.enabled }
        if (toSave.isEmpty()) return
        viewModelScope.launch {
            _uiState.update { it.copy(saving = true) }
            val time = now()
            deckRepo.createCards(
                toSave.map { parsed ->
                    CardEntity(
                        categoryId = categoryId,
                        type = CardType.BASIC,
                        question = parsed.question,
                        answer = parsed.answer,
                        dueAt = time,
                        createdAt = time,
                        updatedAt = time,
                    )
                },
            )
            _uiState.update { it.copy(saving = false, savedCount = toSave.size) }
        }
    }

    private fun reparse() {
        val text = extractedText ?: return
        val state = _uiState.value
        val result = when (state.preset) {
            ImportPreset.CSV -> CsvParser.parse(text)
            else -> QaHeuristicParser.parse(text, state.preset.toParserPreset())
        }
        when (result) {
            is ImportResult.Success -> _uiState.update {
                it.copy(cards = result.cards, error = null, savedCount = null)
            }
            is ImportResult.Failure -> _uiState.update {
                it.copy(cards = emptyList(), error = result.message, savedCount = null)
            }
        }
    }

    companion object {
        val Factory = containerViewModelFactory {
            ImportViewModel(it.deckRepository, it.pdfTextExtractor)
        }
    }
}
