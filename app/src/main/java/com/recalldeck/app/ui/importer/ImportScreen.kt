package com.recalldeck.app.ui.importer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recalldeck.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImportScreen(
    state: ImportUiState,
    onBack: () -> Unit,
    onPickFile: () -> Unit,
    onPresetChange: (ImportPreset) -> Unit,
    onToggleCard: (Int) -> Unit,
    onEditCard: (Int, String, String) -> Unit,
    onSubjectSelect: (Long) -> Unit,
    onCategorySelect: (Long) -> Unit,
    onSave: () -> Unit,
) {
    var editingIndex by remember { mutableStateOf<Int?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Import") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(onClick = onPickFile) {
                    Text(if (state.fileName == null) "Pick a file" else "Pick another file")
                }
                Text(
                    state.fileName ?: "PDF, TXT, or CSV",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
            }

            if (state.fileName != null) {
                Spacer(Modifier.height(8.dp))
                Text("Format", style = MaterialTheme.typography.titleSmall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImportPreset.entries.take(3).forEach { preset ->
                        FilterChip(
                            selected = state.preset == preset,
                            onClick = { onPresetChange(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ImportPreset.entries.drop(3).forEach { preset ->
                        FilterChip(
                            selected = state.preset == preset,
                            onClick = { onPresetChange(preset) },
                            label = { Text(preset.label) },
                        )
                    }
                }
            }

            state.error?.let { error ->
                Spacer(Modifier.height(8.dp))
                Text(
                    error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            state.savedCount?.let { count ->
                Spacer(Modifier.height(8.dp))
                Text(
                    "Saved $count cards.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            if (state.cards.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                TargetPickers(state, onSubjectSelect, onCategorySelect)
                Spacer(Modifier.height(8.dp))
                Text(
                    "${state.enabledCount} of ${state.cards.size} cards selected",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                LazyColumn(modifier = Modifier.weight(1f)) {
                    itemsIndexed(state.cards) { index, card ->
                        Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
                            Row(
                                modifier = Modifier.padding(8.dp),
                                verticalAlignment = Alignment.Top,
                            ) {
                                Checkbox(
                                    checked = card.enabled,
                                    onCheckedChange = { onToggleCard(index) },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    if (editingIndex == index) {
                                        var question by remember { mutableStateOf(card.question) }
                                        var answer by remember { mutableStateOf(card.answer) }
                                        OutlinedTextField(
                                            value = question,
                                            onValueChange = { question = it },
                                            label = { Text("Question") },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Spacer(Modifier.height(4.dp))
                                        OutlinedTextField(
                                            value = answer,
                                            onValueChange = { answer = it },
                                            label = { Text("Answer") },
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                        Row {
                                            TextButton(onClick = {
                                                onEditCard(index, question, answer)
                                                editingIndex = null
                                            }) { Text("Done") }
                                            TextButton(onClick = { editingIndex = null }) {
                                                Text("Cancel")
                                            }
                                        }
                                    } else {
                                        Text(card.question, style = MaterialTheme.typography.bodyLarge)
                                        Text(
                                            card.answer,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                }
                                if (editingIndex != index) {
                                    TextButton(onClick = { editingIndex = index }) { Text("Edit") }
                                }
                            }
                        }
                    }
                }
                Button(
                    onClick = onSave,
                    enabled = state.canSave,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                ) {
                    Text(if (state.saving) "Saving…" else "Save ${state.enabledCount} cards")
                }
            } else if (state.fileName == null) {
                EmptyState(
                    title = "Import flashcards from a file",
                    subtitle = "Pick a PDF, TXT, or CSV file and RecallDeck will detect " +
                        "question/answer pairs for you to review before saving.",
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TargetPickers(
    state: ImportUiState,
    onSubjectSelect: (Long) -> Unit,
    onCategorySelect: (Long) -> Unit,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        var subjectExpanded by remember { mutableStateOf(false) }
        val selectedSubject = state.subjects.find { it.id == state.selectedSubjectId }
        ExposedDropdownMenuBox(
            expanded = subjectExpanded,
            onExpandedChange = { subjectExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedSubject?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Subject") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subjectExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = subjectExpanded,
                onDismissRequest = { subjectExpanded = false },
            ) {
                state.subjects.forEach { subject ->
                    DropdownMenuItem(
                        text = { Text(subject.name) },
                        onClick = {
                            onSubjectSelect(subject.id)
                            subjectExpanded = false
                        },
                    )
                }
            }
        }
        Spacer(Modifier.width(8.dp))
        var categoryExpanded by remember { mutableStateOf(false) }
        val selectedCategory = state.categories.find { it.id == state.selectedCategoryId }
        ExposedDropdownMenuBox(
            expanded = categoryExpanded,
            onExpandedChange = { categoryExpanded = it },
            modifier = Modifier.weight(1f),
        ) {
            OutlinedTextField(
                value = selectedCategory?.name ?: "",
                onValueChange = {},
                readOnly = true,
                label = { Text("Category") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                modifier = Modifier.menuAnchor().fillMaxWidth(),
            )
            ExposedDropdownMenu(
                expanded = categoryExpanded,
                onDismissRequest = { categoryExpanded = false },
            ) {
                state.categories.forEach { category ->
                    DropdownMenuItem(
                        text = { Text(category.name) },
                        onClick = {
                            onCategorySelect(category.id)
                            categoryExpanded = false
                        },
                    )
                }
            }
        }
    }
}
