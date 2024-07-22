package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.BudgetDt
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetListScreenUiState(
    val categoryId: String? = null,
    val searchQuery: String = "",
    val budgetList: List<BudgetDt> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class BudgetListScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetListScreenUiState())
    val uiState: StateFlow<BudgetListScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[BudgetListScreenDestination.categoryId]

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query
            )
        }
        getBudgets()
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = ""
            )
        }
    }

    fun getBudgets() {
        viewModelScope.launch {
            try {
                val response = if(uiState.value.categoryId != null) apiRepository.getCategoryBudgets(
                    categoryId = uiState.value.categoryId!!.toInt(),
                    name = uiState.value.searchQuery
                ) else apiRepository.getUserBudgets(
                    userId = 1,
                    name = uiState.value.searchQuery
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            budgetList = response.body()?.data?.budget!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getBudgetsErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("getBudgetsException", e.toString())
            }
        }
    }

    init {
        _uiState.update {
            it.copy(
                categoryId = categoryId,
            )
        }
        getBudgets()
    }
}