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

    fun updateBudget() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        val budget = BudgetEditPayLoad(
            name = uiState.value.budgetName.ifEmpty { uiState.value.budget.name!! },
            budgetLimit = uiState.value.budgetLimit.ifEmpty { uiState.value.budget.budgetLimit.toString() }.toDouble(),
            limitDate = uiState.value.budgetLimitDate.ifEmpty { uiState.value.budget.limitDate },
            limitExceeded = uiState.value.budget.limitReached
        )
        Log.i("UPDATING_BUDGET", budget.toString())
        viewModelScope.launch {
            try {
                val response = apiRepository.updateBudget(
                    token = uiState.value.userDetails.token,
                    budget = budget,
                    budgetId = uiState.value.budget.id
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("budgetUpdateErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("budgetUpdateException", e.toString())
            }
        }
    }

    fun deleteBudget() {
        _uiState.update {
            it.copy(
                executionStatus = ExecutionStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.deleteBudget(
                    token = uiState.value.userDetails.token,
                    budgetId = uiState.value.budget.id
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            executionStatus = ExecutionStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            executionStatus = ExecutionStatus.FAIL
                        )
                    }
                    Log.e("deleteBudgetErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        executionStatus = ExecutionStatus.FAIL
                    )
                }
                Log.e("deleteBudgetErrorException", e.toString())
            }
        }
    }

    fun getBudget() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getBudget(
                    token = uiState.value.userDetails.token,
                    budgetId = uiState.value.budgetId!!.toInt()
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            budget = response.body()?.data?.budget!!,
                        )
                    }
                } else {
                    Log.e("getBudgetErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                Log.e("getBudgetException", e.toString())
            }
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
            getBudget()
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