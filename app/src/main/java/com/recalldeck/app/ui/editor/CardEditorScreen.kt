package com.recalldeck.app.ui.editor

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recalldeck.app.data.db.CardType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    state: CardEditorUiState,
    onBack: () -> Unit,
    onTypeChange: (CardType) -> Unit,
    onQuestionChange: (String) -> Unit,
    onAnswerChange: (String) -> Unit,
    onHintChange: (String) -> Unit,
    onMnemonicChange: (String) -> Unit,
    onElaborationChange: (String) -> Unit,
    onSave: () -> Unit,
) {
    LaunchedEffect(state.saved) {
        if (state.saved) onBack()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.isEdit) "Edit card" else "New card") },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            if (!state.isEdit) {
                SingleChoiceSegmentedButtonRow(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                ) {
                    SegmentedButton(
                        selected = state.type == CardType.BASIC,
                        onClick = { onTypeChange(CardType.BASIC) },
                        shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                    ) { Text("Basic") }
                    SegmentedButton(
                        selected = state.type == CardType.CLOZE,
                        onClick = { onTypeChange(CardType.CLOZE) },
                        shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                    ) { Text("Cloze") }
                }
            }
            OutlinedTextField(
                value = state.question,
                onValueChange = onQuestionChange,
                label = {
                    Text(if (state.type == CardType.CLOZE) "Text with {{c1::cloze}} deletions" else "Question")
                },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                minLines = 2,
            )
            if (state.type == CardType.CLOZE) {
                if (state.clozePreviews.isEmpty()) {
                    Text(
                        "Wrap hidden text like {{c1::mitochondria}}. Use c2, c3… for extra cards.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                }
                state.clozePreviews.forEach { preview ->
                    Card(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                "Card ${preview.index}",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(preview.question, style = MaterialTheme.typography.bodyMedium)
                            Text(
                                preview.answer,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            } else {
                OutlinedTextField(
                    value = state.answer,
                    onValueChange = onAnswerChange,
                    label = { Text("Answer") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                    minLines = 2,
                )
            }
            OutlinedTextField(
                value = state.hint,
                onValueChange = onHintChange,
                label = { Text("Hint (optional)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.mnemonic,
                onValueChange = onMnemonicChange,
                label = { Text("Mnemonic (optional)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                singleLine = true,
            )
            OutlinedTextField(
                value = state.elaboration,
                onValueChange = onElaborationChange,
                label = { Text("Detailed explanation (optional)") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                minLines = 2,
            )
            Button(
                onClick = onSave,
                enabled = state.canSave,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Save")
            }
        }
    }
}
