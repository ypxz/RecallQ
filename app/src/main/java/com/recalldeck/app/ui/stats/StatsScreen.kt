package com.recalldeck.app.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.recalldeck.app.data.db.CardState
import com.recalldeck.app.data.stats.ForecastDay
import com.recalldeck.app.data.stats.HeatmapDay
import com.recalldeck.app.data.stats.StatsSnapshot
import com.recalldeck.app.data.stats.SubjectBreakdown
import com.recalldeck.app.ui.common.EmptyState
import com.recalldeck.app.ui.common.displayLabel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    state: StatsUiState,
    onBack: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Stats") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
    ) { padding ->
        val snapshot = state.snapshot
        if (snapshot == null) {
            if (!state.loading) {
                EmptyState(
                    title = "No stats yet",
                    subtitle = "Review some cards and your progress will show up here.",
                )
            }
            return@Scaffold
        }
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = "${snapshot.currentStreakDays}",
                    label = "day streak",
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    value = snapshot.retentionPercent?.let { "${it.toInt()}%" } ?: "—",
                    label = "retention (30 days)",
                )
            }

            Section("Last 12 weeks") { Heatmap(snapshot.heatmap) }
            Section("Due in the next 30 days") { ForecastChart(snapshot.forecast) }
            Section("Subjects") { SubjectBreakdownList(snapshot.subjectBreakdown) }
        }
    }
}

@Composable
private fun StatCard(modifier: Modifier, value: String, label: String) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(value, style = MaterialTheme.typography.headlineMedium)
            Text(
                label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun Section(title: String, content: @Composable () -> Unit) {
    Column {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        content()
    }
}

/** 12-week review heatmap drawn as a 7-row grid of day cells, oldest week first. */
@Composable
private fun Heatmap(days: List<HeatmapDay>) {
    val cellColor = MaterialTheme.colorScheme.primary
    val emptyColor = MaterialTheme.colorScheme.surfaceVariant
    val max = (days.maxOfOrNull { it.reviewCount } ?: 0).coerceAtLeast(1)
    Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
        if (days.isEmpty()) return@Canvas
        val weeks = (days.size + 6) / 7
        val gap = 3.dp.toPx()
        val cell = minOf(
            (size.width - gap * (weeks - 1)) / weeks,
            (size.height - gap * 6) / 7,
        )
        days.forEachIndexed { index, day ->
            val week = index / 7
            val dayOfWeek = index % 7
            val fraction = day.reviewCount.toFloat() / max
            val color = if (day.reviewCount == 0) {
                emptyColor
            } else {
                cellColor.copy(alpha = 0.25f + 0.75f * fraction)
            }
            drawRoundRect(
                color = color,
                topLeft = Offset(week * (cell + gap), dayOfWeek * (cell + gap)),
                size = Size(cell, cell),
                cornerRadius = CornerRadius(2.dp.toPx()),
            )
        }
    }
}

/** 30-day due forecast drawn as a simple bar chart. Day 0 includes overdue cards. */
@Composable
private fun ForecastChart(forecast: List<ForecastDay>) {
    val barColor = MaterialTheme.colorScheme.primary
    val baselineColor = MaterialTheme.colorScheme.surfaceVariant
    val max = (forecast.maxOfOrNull { it.dueCount } ?: 0).coerceAtLeast(1)
    Column {
        Canvas(modifier = Modifier.fillMaxWidth().height(120.dp)) {
            if (forecast.isEmpty()) return@Canvas
            val gap = 2.dp.toPx()
            val barWidth = (size.width - gap * (forecast.size - 1)) / forecast.size
            drawLine(
                color = baselineColor,
                start = Offset(0f, size.height),
                end = Offset(size.width, size.height),
                strokeWidth = 1.dp.toPx(),
            )
            forecast.forEachIndexed { index, day ->
                if (day.dueCount == 0) return@forEachIndexed
                val barHeight = size.height * (day.dueCount.toFloat() / max)
                drawRoundRect(
                    color = barColor,
                    topLeft = Offset(index * (barWidth + gap), size.height - barHeight),
                    size = Size(barWidth, barHeight),
                    cornerRadius = CornerRadius(1.dp.toPx()),
                )
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                "today",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                "in 30 days",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SubjectBreakdownList(breakdown: List<SubjectBreakdown>) {
    if (breakdown.isEmpty()) {
        Text(
            "No subjects yet.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        breakdown.forEach { subject ->
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(subject.subjectName, style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "${subject.totalCards} cards",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(4.dp))
                StateBar(subject.stateCounts)
                Spacer(Modifier.height(2.dp))
                Text(
                    CardState.entries.joinToString(" · ") { state ->
                        "${subject.stateCounts[state] ?: 0} ${state.displayLabel().lowercase()}"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun StateBar(stateCounts: Map<CardState, Int>) {
    val total = stateCounts.values.sum().coerceAtLeast(1)
    val colors = mapOf(
        CardState.NEW to MaterialTheme.colorScheme.tertiary,
        CardState.LEARNING to MaterialTheme.colorScheme.secondary,
        CardState.REVIEW to MaterialTheme.colorScheme.primary,
        CardState.SUSPENDED to MaterialTheme.colorScheme.surfaceVariant,
    )
    Canvas(modifier = Modifier.fillMaxWidth().height(8.dp)) {
        var x = 0f
        listOf(CardState.NEW, CardState.LEARNING, CardState.REVIEW, CardState.SUSPENDED)
            .forEach { state ->
                val count = stateCounts[state] ?: 0
                if (count == 0) return@forEach
                val width = size.width * (count.toFloat() / total)
                drawRect(
                    color = colors.getValue(state),
                    topLeft = Offset(x, 0f),
                    size = Size(width, size.height),
                )
                x += width
            }
    }
}
