package com.recalldeck.app.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.recalldeck.app.AppContainer
import com.recalldeck.app.RecallDeckApplication
import com.recalldeck.app.data.db.CardState

/** User-facing name for a card's scheduling state ("do I know this card?"). */
fun CardState.displayLabel(): String = when (this) {
    CardState.NEW -> "Not studied"
    CardState.LEARNING -> "Kind of know"
    CardState.REVIEW -> "Know"
    CardState.SUSPENDED -> "Never ask"
}

/** Parses "#RRGGBB" safely, falling back to a neutral color. */
fun parseColorHex(hex: String): Color = try {
    Color(android.graphics.Color.parseColor(hex))
} catch (e: IllegalArgumentException) {
    Color(0xFF9E9E9E)
}

val SUBJECT_COLORS = listOf(
    "#EF5350", "#AB47BC", "#5C6BC0", "#29B6F6",
    "#26A69A", "#66BB6A", "#FFCA28", "#FF7043",
)

@Composable
fun EmptyState(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.Center)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 8.dp),
        )
    }
}

/** ViewModel factory giving access to the manual DI container. */
inline fun <reified VM : ViewModel> containerViewModelFactory(
    crossinline create: (AppContainer) -> VM,
): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
        val app = extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY]
            as RecallDeckApplication
        @Suppress("UNCHECKED_CAST")
        return create(app.container) as T
    }
}
