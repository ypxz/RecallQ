package com.recalldeck.app.ui.browser

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DriveFileMove
import androidx.compose.material.icons.filled.PauseCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.recalldeck.app.data.db.CardBucket
import com.recalldeck.app.data.db.CardEntity
import com.recalldeck.app.data.db.CategoryEntity
import com.recalldeck.app.data.repo.AppSettings
import com.recalldeck.app.ui.common.EmptyState

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CardBrowserScreen(
    state: CardBrowserUiState,
    onBack: () -> Unit,
    onQueryChange: (String) -> Unit,
    onBucketFilterChange: (CardBucket?) -> Unit,
    onToggleSelection: (Long) -> Unit,
    onClearSelection: () -> Unit,
    onDeleteSelected: () -> Unit,
    onSuspendSelected: (Boolean) -> Unit,
    onMoveSelected: (Long) -> Unit,
    onCardClick: (Long) -> Unit,
    onAddCard: (() -> Unit)? = null,
) {
    var showMoveDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    val selectionMode = state.selectedIds.isNotEmpty()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (selectionMode) "${state.selectedIds.size} selected" else "Card browser",
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { if (selectionMode) onClearSelection() else onBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    if (selectionMode) {
                        IconButton(onClick = { showMoveDialog = true }) {
                            Icon(Icons.Default.DriveFileMove, contentDescription = "Move")
                        }
                        IconButton(onClick = { onSuspendSelected(true) }) {
                            Icon(Icons.Default.PauseCircle, contentDescription = "Suspend")
                        }
                        IconButton(onClick = { showDeleteDialog = true }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            if (onAddCard != null && !selectionMode) {
                FloatingActionButton(onClick = onAddCard) {
                    Icon(Icons.Default.Add, contentDescription = "Add card")
                }
            }
        },
    ) { padding ->
        Column(modifier = Modifier.padding(padding).fillMaxSize()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = onQueryChange,
                label = { Text("Search") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
            ) {
                FilterChip(
                    selected = state.bucketFilter == null,
                    onClick = { onBucketFilterChange(null) },
                    label = { Text("All") },
                    modifier = Modifier.padding(end = 8.dp),
                )
                CardBucket.entries.forEach { bucket ->
                    FilterChip(
                        selected = state.bucketFilter == bucket,
                        onClick = { onBucketFilterChange(bucket) },
                        label = { Text(bucket.displayLabel) },
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
            Text(
                bucketIntervalCaption(state.settings),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp),
            )
            if (state.cards.isEmpty() && !state.loading) {
                EmptyState(
                    title = "No cards found",
                    subtitle = "Try a different search or filter, or add cards from a category.",
                )
            } else {
                LazyColumn(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
                    items(state.cards, key = { it.id }) { card ->
                        CardRow(
                            card = card,
                            bucket = state.buckets[card.id] ?: CardBucket.NOT_STUDIED,
                            selected = card.id in state.selectedIds,
                            selectionMode = selectionMode,
                            onClick = {
                                if (selectionMode) onToggleSelection(card.id) else onCardClick(card.id)
                            },
                            onLongClick = { onToggleSelection(card.id) },
                        )
                    }
                }
            }
        }
    }

    if (showMoveDialog) {
        AlertDialog(
            onDismissRequest = { showMoveDialog = false },
            title = { Text("Move to category") },
            text = {
                Column {
                    state.categories.forEach { category: CategoryEntity ->
                        TextButton(
                            onClick = {
                                onMoveSelected(category.id)
                                showMoveDialog = false
                            },
                        ) { Text(category.name) }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showMoveDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${state.selectedIds.size} cards?") },
            text = { Text("This cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        onDeleteSelected()
                        showDeleteDialog = false
                    },
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }
}

/** One-line summary of when cards return per bucket, from the learning-step settings. */
fun bucketIntervalCaption(settings: AppSettings): String {
    val hard = if (settings.newHardDelayMinutes == settings.learningHardDelayMinutes) {
        "${settings.newHardDelayMinutes} min"
    } else {
        "${settings.newHardDelayMinutes}–${settings.learningHardDelayMinutes} min"
    }
    return "Returns: Very hard ~${settings.againDelayMinutes} min · Hard ~$hard · " +
        "Medium & Easy after days (adaptive)"
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun CardRow(
    card: CardEntity,
    bucket: CardBucket,
    selected: Boolean,
    selectionMode: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        colors = if (selected) {
            CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
        } else {
            CardDefaults.cardColors()
        },
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    card.question,
                    style = MaterialTheme.typography.bodyLarge,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    card.answer,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Column(modifier = Modifier.width(90.dp)) {
                Text(
                    bucket.displayLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    card.type.name,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
