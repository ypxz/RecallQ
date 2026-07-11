package com.recalldeck.app.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.AlertDialog
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.recalldeck.app.data.db.SubjectEntity
import com.recalldeck.app.ui.common.EmptyState
import com.recalldeck.app.ui.common.SUBJECT_COLORS
import com.recalldeck.app.ui.common.parseColorHex

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    state: HomeUiState,
    onCreateSubject: (String) -> Unit,
    onUpdateSubject: (SubjectEntity, String, String) -> Unit,
    onDeleteSubject: (SubjectEntity) -> Unit,
    onSubjectClick: (Long) -> Unit,
    onStudyAllDue: () -> Unit,
    onCustomStudy: () -> Unit,
    onStatsClick: () -> Unit,
    onImportClick: () -> Unit,
    onSettingsClick: () -> Unit,
) {
    var showCreateDialog by remember { mutableStateOf(false) }
    var subjectToEdit by remember { mutableStateOf<SubjectEntity?>(null) }
    var subjectToDelete by remember { mutableStateOf<SubjectEntity?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("RecallDeck") },
                actions = {
                    IconButton(onClick = onImportClick) {
                        Icon(Icons.Default.FileDownload, contentDescription = "Import")
                    }
                    IconButton(onClick = onStatsClick) {
                        Icon(Icons.Default.BarChart, contentDescription = "Stats")
                    }
                    IconButton(onClick = onSettingsClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { showCreateDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Add subject")
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                ),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "${state.dueCount}",
                            style = MaterialTheme.typography.displaySmall,
                        )
                        Text(
                            "cards due",
                            style = MaterialTheme.typography.bodyLarge,
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocalFireDepartment,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.size(18.dp),
                            )
                            Text(
                                "${state.streak}-day streak",
                                style = MaterialTheme.typography.bodyMedium,
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Button(
                            onClick = onStudyAllDue,
                            enabled = state.dueCount > 0,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                        ) {
                            Text("Study all due")
                        }
                        TextButton(onClick = onCustomStudy) { Text("Custom study") }
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (state.subjects.isEmpty() && !state.loading) {
                EmptyState(
                    title = "No subjects yet",
                    subtitle = "Tap + to create your first subject.",
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize().padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(state.subjects, key = { it.id }) { subject ->
                        SubjectCard(
                            subject = subject,
                            onClick = { onSubjectClick(subject.id) },
                            onLongClick = { subjectToEdit = subject },
                        )
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New subject") },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name") },
                    singleLine = true,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onCreateSubject(name)
                        showCreateDialog = false
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            },
        )
    }

    subjectToEdit?.let { subject ->
        var name by remember(subject.id) { mutableStateOf(subject.name) }
        var colorHex by remember(subject.id) { mutableStateOf(subject.colorHex) }
        AlertDialog(
            onDismissRequest = { subjectToEdit = null },
            title = { Text("Edit subject") },
            text = {
                SubjectEditFields(
                    name = name,
                    colorHex = colorHex,
                    onNameChange = { name = it },
                    onColorChange = { colorHex = it },
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onUpdateSubject(subject, name.trim(), colorHex)
                        subjectToEdit = null
                    },
                    enabled = name.isNotBlank(),
                ) { Text("Save") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        subjectToDelete = subject
                        subjectToEdit = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
                TextButton(onClick = { subjectToEdit = null }) { Text("Cancel") }
            },
        )
    }

    subjectToDelete?.let { subject ->
        AlertDialog(
            onDismissRequest = { subjectToDelete = null },
            title = { Text("Delete subject?") },
            text = {
                Text(
                    "\"${subject.name}\" and all of its categories and cards " +
                        "will be permanently deleted.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSubject(subject)
                        subjectToDelete = null
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { subjectToDelete = null }) { Text("Cancel") }
            },
        )
    }
}

@Composable
internal fun SubjectEditFields(
    name: String,
    colorHex: String,
    onNameChange: (String) -> Unit,
    onColorChange: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name") },
            singleLine = true,
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("Color", style = MaterialTheme.typography.labelLarge)
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SUBJECT_COLORS.forEach { hex ->
                val selected = hex.equals(colorHex, ignoreCase = true)
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .then(
                            if (selected) {
                                Modifier.border(
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface),
                                    CircleShape,
                                )
                            } else {
                                Modifier
                            },
                        )
                        .padding(4.dp)
                        .background(parseColorHex(hex), CircleShape)
                        .clickable { onColorChange(hex) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SubjectCard(
    subject: SubjectEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(modifier = Modifier.combinedClickable(onClick = onClick, onLongClick = onLongClick)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Spacer(
                modifier = Modifier
                    .size(16.dp)
                    .background(parseColorHex(subject.colorHex), CircleShape),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(subject.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
        }
    }
}
