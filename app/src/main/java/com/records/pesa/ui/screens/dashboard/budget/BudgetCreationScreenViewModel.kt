package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

data class BudgetCreationScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val budgetName: String = "",
    val budgetLimit: String = "",
    val limitDate: LocalDate? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val newBudgetId: Int = 0,
    val saveButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class BudgetCreationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetCreationScreenUiState())
    val uiState: StateFlow<BudgetCreationScreenUiState> = _uiState.asStateFlow()

    private val categoryIdArg: String? = savedStateHandle[BudgetCreationScreenDestination.categoryId]

    fun updateBudgetName(name: String) { _uiState.update { it.copy(budgetName = name) }; checkFields() }
    fun updateBudgetLimit(amount: String) { _uiState.update { it.copy(budgetLimit = amount) }; checkFields() }
    fun updateLimitDate(date: LocalDate) { _uiState.update { it.copy(limitDate = date) }; checkFields() }

    fun resetLoadingStatus() = _uiState.update { it.copy(loadingStatus = LoadingStatus.INITIAL) }

    private fun checkFields() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                saveButtonEnabled = s.budgetName.isNotBlank() &&
                    s.budgetLimit.isNotBlank() &&
                    (s.budgetLimit.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    s.limitDate != null &&
                    s.limitDate.isAfter(LocalDate.now())
            )
        }
    }

    fun createBudget() {
        val catId = _uiState.value.categoryId?.toIntOrNull() ?: return
        val limit = _uiState.value.budgetLimit.toDoubleOrNull() ?: return
        val endDate = _uiState.value.limitDate ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
            try {
                val budget = Budget(
                    name = _uiState.value.budgetName.trim(),
                    active = true,
                    expenditure = 0.0,
                    budgetLimit = limit,
                    createdAt = LocalDateTime.now(),
                    limitDate = endDate,
                    limitReached = false,
                    limitReachedAt = null,
                    exceededBy = 0.0,
                    categoryId = catId
                )
                val newId = dbRepository.insertBudget(budget)
                _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS, newBudgetId = newId.toInt()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
            }
        }
    }

    init {
        _uiState.update { it.copy(categoryId = categoryIdArg) }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
        }
    }
}
