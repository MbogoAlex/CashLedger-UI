package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.BudgetEditPayLoad
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.ExecutionStatus
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.budgets
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetInfoScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val budgetId: String? = null,
    val budget: BudgetDt = budgets[0],
    val budgetName: String = "",
    val budgetLimit: String = "",
    val budgetLimitDate: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val executionStatus: ExecutionStatus = ExecutionStatus.INITIAL
)
class BudgetInfoScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetInfoScreenUiState())
    val uiState: StateFlow<BudgetInfoScreenUiState> = _uiState.asStateFlow()

    private val budgetId: String? = savedStateHandle[BudgetInfoScreenDestination.budgetId]

    fun updateBudgetName(name: String) {
        _uiState.update {
            it.copy(
                budgetName = name
            )
        }
    }

    fun updateBudgetLimit(amount: String) {
        _uiState.update {
            it.copy(
                budgetLimit = amount
            )
        }
    }

    fun updateLimitDate(date: String) {
        _uiState.update {
            it.copy(
                budgetLimitDate = date
            )
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL,
                executionStatus = ExecutionStatus.INITIAL
            )
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
        }
    }

    init {
        _uiState.update {
            it.copy(
                budgetId = budgetId
            )
        }
        getUserDetails()
    }
}