package com.recalldeck.app.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.recalldeck.app.data.stats.StatsRepo
import com.recalldeck.app.data.stats.StatsSnapshot
import com.recalldeck.app.ui.common.containerViewModelFactory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class StatsUiState(
    val snapshot: StatsSnapshot? = null,
    val loading: Boolean = true,
)

class StatsViewModel(private val statsRepo: StatsRepo) : ViewModel() {

    private val _uiState = MutableStateFlow(StatsUiState())
    val uiState: StateFlow<StatsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            _uiState.value = StatsUiState(snapshot = statsRepo.snapshot(), loading = false)
        }
    }

    companion object {
        val Factory = containerViewModelFactory { StatsViewModel(it.statsRepo) }
    }
}
