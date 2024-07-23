package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.BudgetDt
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.budgets
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetInfoScreenUiState(
    val budgetId: String? = null,
    val budget: BudgetDt = budgets[0],
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class BudgetInfoScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetInfoScreenUiState())
    val uiState: StateFlow<BudgetInfoScreenUiState> = _uiState.asStateFlow()

    private val budgetId: String? = savedStateHandle[BudgetInfoScreenDestination.budgetId]
    fun getBudget() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getBudget(uiState.value.budgetId!!.toInt())
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            budget = response.body()?.data?.budget!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getBudgetErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("getBudgetException", e.toString())
            }
        }
    }

    init {
        _uiState.update {
            it.copy(
                budgetId = budgetId
            )
        }
        getBudget()
    }
}