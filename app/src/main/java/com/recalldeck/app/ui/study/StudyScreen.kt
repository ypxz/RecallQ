package com.recalldeck.app.ui.study

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Undo
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.recalldeck.app.srs.Grade
import com.recalldeck.app.srs.TypeAnswer
import com.recalldeck.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyScreen(
    state: StudyUiState,
    onBack: () -> Unit,
    onReveal: () -> Unit,
    onRevealHint: () -> Unit,
    onGrade: (Grade) -> Unit,
    onUndo: () -> Unit,
    onSuspend: () -> Unit,
    onTypedInputChange: (String) -> Unit,
    onCheckTypedAnswer: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (state.cram) "Cram" else "Study") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onUndo, enabled = state.canUndo) {
                        Icon(Icons.AutoMirrored.Filled.Undo, contentDescription = "Undo")
                    }
                    if (!state.finished && !state.emptyQueue) {
                        IconButton(onClick = { menuOpen = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "More")
                        }
                        DropdownMenu(expanded = menuOpen, onDismissRequest = { menuOpen = false }) {
                            DropdownMenuItem(
                                text = { Text("Never ask again") },
                                onClick = {
                                    menuOpen = false
                                    onSuspend()
                                },
                            )
                        }
                    }
                },
            )
        },
    ) { padding ->
        when {
            state.loading -> Column(
                modifier = Modifier.padding(padding).fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) { CircularProgressIndicator() }

            state.emptyQueue -> EmptyState(
                title = "Nothing to study",
                subtitle = "No cards match this session. Come back when cards are due.",
                modifier = Modifier.padding(padding),
            )

            state.finished -> SessionSummary(state.summary, modifier = Modifier.padding(padding))

            else -> CardStudyContent(
                state = state,
                onReveal = onReveal,
                onRevealHint = onRevealHint,
                onGrade = onGrade,
                onTypedInputChange = onTypedInputChange,
                onCheckTypedAnswer = onCheckTypedAnswer,
                modifier = Modifier.padding(padding),
            )
        }
    }
}

@Composable
private fun CardStudyContent(
    state: StudyUiState,
    onReveal: () -> Unit,
    onRevealHint: () -> Unit,
    onGrade: (Grade) -> Unit,
    onTypedInputChange: (String) -> Unit,
    onCheckTypedAnswer: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize().padding(16.dp)) {
        LinearProgressIndicator(
            progress = { if (state.total == 0) 0f else state.position.toFloat() / state.total },
            modifier = Modifier.fillMaxWidth(),
        )
        Text(
            "${state.position + 1} / ${state.total}",
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp),
        )
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ElevatedCard(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    QuestionAnswerContent(state, onRevealHint)
                }
            }
        }
        if (!state.revealed) {
            if (state.typeAnswer) {
                OutlinedTextField(
                    value = state.typedInput,
                    onValueChange = onTypedInputChange,
                    label = { Text("Type your answer") },
                    modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                    singleLine = true,
                )
                Button(onClick = onCheckTypedAnswer, modifier = Modifier.fillMaxWidth()) {
                    Text("Check")
                }
            } else {
                Button(onClick = onReveal, modifier = Modifier.fillMaxWidth()) {
                    Text("Show answer")
                }
            }
        } else {
            GradeButtons(captions = state.intervalCaptions, onGrade = onGrade)
        }
    }
}

@Composable
private fun QuestionAnswerContent(state: StudyUiState, onRevealHint: () -> Unit) {
    Text(
        state.question,
        style = MaterialTheme.typography.headlineSmall,
        textAlign = TextAlign.Center,
    )
    if (state.hint != null && !state.revealed) {
        Spacer(Modifier.height(16.dp))
        if (state.hintRevealed) {
            Text(
                "Hint: ${state.hint}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        } else {
            AssistChip(onClick = onRevealHint, label = { Text("Show hint") })
        }
    }
    AnimatedVisibility(
        visible = state.revealed,
        enter = fadeIn() + expandVertically(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Spacer(Modifier.height(20.dp))
            HorizontalDivider(modifier = Modifier.fillMaxWidth(0.4f))
            Spacer(Modifier.height(20.dp))
            state.verdict?.let { verdict ->
                Text(
                    when (verdict) {
                        TypeAnswer.Verdict.CORRECT -> "Correct!"
                        TypeAnswer.Verdict.ALMOST -> "Almost — check the answer"
                        TypeAnswer.Verdict.WRONG -> "Not quite"
                    },
                    style = MaterialTheme.typography.titleMedium,
                    color = when (verdict) {
                        TypeAnswer.Verdict.CORRECT -> MaterialTheme.colorScheme.primary
                        TypeAnswer.Verdict.ALMOST -> MaterialTheme.colorScheme.tertiary
                        TypeAnswer.Verdict.WRONG -> MaterialTheme.colorScheme.error
                    },
                )
                Spacer(Modifier.height(8.dp))
            }
            Text(
                state.answer,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center,
            )
            if (state.mnemonic != null) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Mnemonic: ${state.mnemonic}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
            if (state.elaboration != null) {
                Spacer(Modifier.height(8.dp))
                Text(
                    state.elaboration,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun GradeButtons(captions: Map<Grade, String>, onGrade: (Grade) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        GradeButton("Again", captions[Grade.AGAIN], { onGrade(Grade.AGAIN) }, Modifier.weight(1f), error = true)
        GradeButton("Hard", captions[Grade.HARD], { onGrade(Grade.HARD) }, Modifier.weight(1f))
        GradeButton("Good", captions[Grade.GOOD], { onGrade(Grade.GOOD) }, Modifier.weight(1f))
        GradeButton("Easy", captions[Grade.EASY], { onGrade(Grade.EASY) }, Modifier.weight(1f))
    }
}

@Composable
private fun GradeButton(
    label: String,
    caption: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    error: Boolean = false,
) {
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 10.dp),
        colors = if (error) {
            ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.errorContainer,
                contentColor = MaterialTheme.colorScheme.onErrorContainer,
            )
        } else {
            ButtonDefaults.filledTonalButtonColors()
        },
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(label, style = MaterialTheme.typography.labelLarge)
            Text(
                caption ?: "",
                style = MaterialTheme.typography.labelSmall,
                color = LocalContentColor.current.copy(alpha = 0.7f),
            )
        }
    }
}

@Composable
private fun SessionSummary(summary: StudySummary, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("Session complete", style = MaterialTheme.typography.headlineSmall)
        Spacer(Modifier.height(8.dp))
        Text(
            "${summary.reviewed} reviews",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(24.dp))
        SummaryRow("Again", summary.again)
        SummaryRow("Hard", summary.hard)
        SummaryRow("Good", summary.good)
        SummaryRow("Easy", summary.easy)
    }
}

@Composable
private fun SummaryRow(label: String, count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth(0.6f).padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text("$count", style = MaterialTheme.typography.bodyLarge)
    }
}
