package com.recalldeck.app.ui.settings

import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import com.recalldeck.app.data.repo.ThemeMode
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    state: SettingsUiState,
    onBack: () -> Unit,
    onRetentionTargetChange: (Double) -> Unit,
    onNewPerDayChange: (Int) -> Unit,
    onThemeModeChange: (ThemeMode) -> Unit,
    onAutoSuspendChange: (Boolean) -> Unit,
    onAgainDelayChange: (Int) -> Unit,
    onNewHardDelayChange: (Int) -> Unit,
    onNewGoodDelayChange: (Int) -> Unit,
    onLearningHardDelayChange: (Int) -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onPickReminderTime: () -> Unit,
    onExportBackup: () -> Unit,
    onImportBackup: () -> Unit,
    onExportCsv: (Long) -> Unit,
    onDismissMessage: () -> Unit,
) {
    var showRestoreConfirm by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings") },
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
            state.message?.let { message ->
                Text(
                    message,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
                TextButton(onClick = onDismissMessage) { Text("Dismiss") }
                Spacer(Modifier.height(8.dp))
            }

            SectionTitle("Scheduling")
            val retention = state.settings.retentionTarget
            Text(
                "Retention target: ${String.format(Locale.US, "%.0f", retention * 100)}%",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = retention.toFloat(),
                onValueChange = { onRetentionTargetChange((it * 100).roundToInt() / 100.0) },
                valueRange = 0.7f..0.97f,
            )
            Text(
                "New cards per day: ${state.settings.newPerDay}",
                style = MaterialTheme.typography.bodyLarge,
            )
            Slider(
                value = state.settings.newPerDay.toFloat(),
                onValueChange = { onNewPerDayChange(it.roundToInt()) },
                valueRange = 0f..100f,
            )
            Spacer(Modifier.height(8.dp))
            Text("Learning steps", style = MaterialTheme.typography.bodyLarge)
            Text(
                "How soon a card comes back after each grade (before it graduates to day intervals)",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(4.dp))
            StepperRow("Again", state.settings.againDelayMinutes, onAgainDelayChange)
            StepperRow("Hard (new card)", state.settings.newHardDelayMinutes, onNewHardDelayChange)
            StepperRow("Good (new card)", state.settings.newGoodDelayMinutes, onNewGoodDelayChange)
            StepperRow("Hard (learning card)", state.settings.learningHardDelayMinutes, onLearningHardDelayChange)
            ToggleRow(
                title = "Auto-suspend mastered cards",
                subtitle = "Suspend cards after 3 consecutive good reviews at 21+ day intervals",
                checked = state.settings.autoSuspendMastered,
                onCheckedChange = onAutoSuspendChange,
            )

            SectionDivider()
            SectionTitle("Reminder")
            ToggleRow(
                title = "Daily reminder",
                subtitle = "Notify when cards are due",
                checked = state.settings.reminderEnabled,
                onCheckedChange = onReminderEnabledChange,
            )
            if (state.settings.reminderEnabled) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "Reminder time",
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = onPickReminderTime) {
                        Text(
                            String.format(
                                Locale.US,
                                "%02d:%02d",
                                state.settings.reminderHour,
                                state.settings.reminderMinute,
                            ),
                        )
                    }
                }
            }

            SectionDivider()
            SectionTitle("Theme")
            Spacer(Modifier.height(8.dp))
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                val modes = listOf(
                    ThemeMode.SYSTEM to "System",
                    ThemeMode.LIGHT to "Light",
                    ThemeMode.DARK to "Dark",
                )
                modes.forEachIndexed { index, (mode, label) ->
                    SegmentedButton(
                        selected = state.settings.themeMode == mode,
                        onClick = { onThemeModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) { Text(label) }
                }
            }

            SectionDivider()
            SectionTitle("Backup")
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = onExportBackup, modifier = Modifier.fillMaxWidth()) {
                Text("Export backup (JSON)")
            }
            Spacer(Modifier.height(8.dp))
            OutlinedButton(
                onClick = { showRestoreConfirm = true },
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Restore from backup")
            }
            Spacer(Modifier.height(8.dp))
            CsvExportButton(state, onExportCsv)
        }
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restore from backup?") },
            text = {
                Text(
                    "Restoring replaces ALL current subjects, cards, and review history " +
                        "with the contents of the backup file. This can't be undone.",
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    showRestoreConfirm = false
                    onImportBackup()
                }) { Text("Restore") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancel") }
            },
        )
    }
}

@Composable
private fun CsvExportButton(state: SettingsUiState, onExportCsv: (Long) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    Column {
        OutlinedButton(
            onClick = { expanded = true },
            enabled = state.subjects.isNotEmpty(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Export subject as CSV")
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            state.subjects.forEach { subject ->
                DropdownMenuItem(
                    text = { Text(subject.name) },
                    onClick = {
                        expanded = false
                        onExportCsv(subject.id)
                    },
                )
            }
        }
    }
}

@Composable
private fun StepperRow(label: String, minutes: Int, onChange: (Int) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        TextButton(onClick = { onChange(minutes - stepFor(minutes - 1)) }, enabled = minutes > 1) { Text("\u2212") }
        Text(
            formatMinutes(minutes),
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        TextButton(onClick = { onChange(minutes + stepFor(minutes)) }, enabled = minutes < 1440) { Text("+") }
    }
}

/** Step size grows with the value: 1 min below 10, 5 min below 60, 30 min beyond. */
private fun stepFor(minutes: Int): Int = when {
    minutes < 10 -> 1
    minutes < 60 -> 5
    else -> 30
}

private fun formatMinutes(minutes: Int): String = when {
    minutes < 60 -> "$minutes min"
    minutes % 60 == 0 -> "${minutes / 60} h"
    else -> "${minutes / 60} h ${minutes % 60} min"
}

@Composable
private fun SectionTitle(title: String) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    Spacer(Modifier.height(4.dp))
}

@Composable
private fun SectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider()
    Spacer(Modifier.height(16.dp))
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
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
