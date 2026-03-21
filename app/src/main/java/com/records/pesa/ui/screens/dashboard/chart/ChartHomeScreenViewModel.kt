package com.records.pesa.ui.screens.dashboard.chart

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.time.LocalDate

data class ChartHomeScreenUiState(
    val categoryId: String? = null,
    val budgetId: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
)
class ChartHomeScreenViewModel(
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(ChartHomeScreenUiState())
    val uiState: StateFlow<ChartHomeScreenUiState> = _uiState.asStateFlow()

    fun initialize() {
        _uiState.update {
            it.copy(
                categoryId = savedStateHandle[ChartHomeScreenDestination.categoryId],
                budgetId = savedStateHandle[ChartHomeScreenDestination.budgetId],
                startDate = savedStateHandle[ChartHomeScreenDestination.startDate],
                endDate = savedStateHandle[ChartHomeScreenDestination.endDate],
            )
        }
    }

    init {
        initialize()
    }
}