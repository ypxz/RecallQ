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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardEditorScreen(
    state: CardEditorUiState,
    onBack: () -> Unit,
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
            OutlinedTextField(
                value = state.question,
                onValueChange = onQuestionChange,
                label = { Text("Question") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                minLines = 2,
            )
            OutlinedTextField(
                value = state.answer,
                onValueChange = onAnswerChange,
                label = { Text("Answer") },
                modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
                minLines = 2,
            )
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
                label = { Text("Elaboration (optional)") },
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
