package com.recalldeck.app.ui.study

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recalldeck.app.srs.CustomOrder
import com.recalldeck.app.srs.QueueMode

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudySetupScreen(
    state: StudySetupUiState,
    onBack: () -> Unit,
    onModeChange: (QueueMode) -> Unit,
    onCountChange: (String) -> Unit,
    onOrderChange: (CustomOrder) -> Unit,
    onCramChange: (Boolean) -> Unit,
    onTypeAnswerChange: (Boolean) -> Unit,
    onStart: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Study setup") },
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
            Text(state.scopeLabel, style = MaterialTheme.typography.titleLarge)
            Text(
                "${state.dueCount} due · ${state.newCount} new · ${state.totalCount} cards",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(16.dp))

            Text("Mode", style = MaterialTheme.typography.titleMedium)
            ModeOption(
                title = "Due review",
                subtitle = "Cards that are due now, plus new cards up to the daily limit",
                selected = state.mode == QueueMode.DUE,
                onClick = { onModeChange(QueueMode.DUE) },
            )
            ModeOption(
                title = "Random mix",
                subtitle = "A shuffled mix of all cards, regardless of due date",
                selected = state.mode == QueueMode.RANDOM,
                onClick = { onModeChange(QueueMode.RANDOM) },
            )
            ModeOption(
                title = "Custom session",
                subtitle = "Pick the count, order, and cram option",
                selected = state.mode == QueueMode.CUSTOM,
                onClick = { onModeChange(QueueMode.CUSTOM) },
            )

            if (state.mode == QueueMode.RANDOM || state.mode == QueueMode.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = state.count,
                    onValueChange = onCountChange,
                    label = { Text("Number of cards") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            if (state.mode == QueueMode.CUSTOM) {
                Spacer(Modifier.height(12.dp))
                Text("Order", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(8.dp))
                SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                    val orders = listOf(
                        CustomOrder.RANDOM to "Random",
                        CustomOrder.OLDEST to "Oldest",
                        CustomOrder.HARDEST to "Hardest",
                    )
                    orders.forEachIndexed { index, (order, label) ->
                        SegmentedButton(
                            selected = state.order == order,
                            onClick = { onOrderChange(order) },
                            shape = SegmentedButtonDefaults.itemShape(index = index, count = orders.size),
                        ) { Text(label) }
                    }
                }
                Spacer(Modifier.height(12.dp))
                ToggleRow(
                    title = "Cram mode",
                    subtitle = "Reviews don't affect scheduling",
                    checked = state.cram,
                    onCheckedChange = onCramChange,
                )
            }

            Spacer(Modifier.height(4.dp))
            ToggleRow(
                title = "Type the answer",
                subtitle = "Type before revealing; your self-grade is final",
                checked = state.typeAnswer,
                onCheckedChange = onTypeAnswerChange,
            )

            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onStart,
                enabled = !state.loading && state.totalCount > 0,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Start studying")
            }
        }
    }
}

@Composable
private fun ModeOption(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .selectable(selected = selected, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Column(modifier = Modifier.padding(start = 4.dp)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ToggleRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
