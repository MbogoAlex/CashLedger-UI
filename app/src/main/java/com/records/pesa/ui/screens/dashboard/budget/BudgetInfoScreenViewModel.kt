package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.ExecutionStatus
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class BudgetInfoScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val budgetId: String? = null,
    val budget: Budget? = null,
    // Computed from local transactions
    val actualSpending: Double = 0.0,
    val percentUsed: Int = 0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false,
    val overage: Double = 0.0,
    val daysLeft: Int = 0,
    val daysElapsed: Int = 0,
    val totalDays: Int = 0,
    val dailyBudget: Double = 0.0,
    val dailyAvg: Double = 0.0,
    val projectedTotal: Double = 0.0,
    // Edit fields
    val budgetName: String = "",
    val budgetLimit: String = "",
    val budgetLimitDate: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val executionStatus: ExecutionStatus = ExecutionStatus.INITIAL
)

@OptIn(ExperimentalCoroutinesApi::class)
class BudgetInfoScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetInfoScreenUiState())
    val uiState: StateFlow<BudgetInfoScreenUiState> = _uiState.asStateFlow()

    private val budgetIdArg: String? = savedStateHandle[BudgetInfoScreenDestination.budgetId]

    fun updateBudgetName(name: String) = _uiState.update { it.copy(budgetName = name) }
    fun updateBudgetLimit(amount: String) = _uiState.update { it.copy(budgetLimit = amount) }
    fun updateLimitDate(date: String) = _uiState.update { it.copy(budgetLimitDate = date) }

    fun resetLoadingStatus() = _uiState.update {
        it.copy(loadingStatus = LoadingStatus.INITIAL, executionStatus = ExecutionStatus.INITIAL)
    }

    fun saveBudgetEdits() {
        val budget = _uiState.value.budget ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
            try {
                val updated = budget.copy(
                    name = _uiState.value.budgetName.ifBlank { budget.name },
                    budgetLimit = _uiState.value.budgetLimit.toDoubleOrNull() ?: budget.budgetLimit,
                    limitDate = _uiState.value.budgetLimitDate.takeIf { it.isNotBlank() }
                        ?.let { LocalDate.parse(it) } ?: budget.limitDate
                )
                dbRepository.updateBudget(updated)
                _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
            }
        }
    }

    fun deleteBudget() {
        val budget = _uiState.value.budget ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(executionStatus = ExecutionStatus.LOADING) }
            try {
                dbRepository.deleteBudget(budget)
                _uiState.update { it.copy(executionStatus = ExecutionStatus.SUCCESS) }
            } catch (e: Exception) {
                _uiState.update { it.copy(executionStatus = ExecutionStatus.FAIL) }
            }
        }
    }

    private fun observeBudgetAndSpending() {
        val id = budgetIdArg?.toIntOrNull() ?: return
        viewModelScope.launch {
            dbRepository.getBudgetById(id)
                .filterNotNull()
                .flatMapLatest { budget ->
                    _uiState.update {
                        it.copy(
                            budget = budget,
                            budgetName = budget.name,
                            budgetLimit = budget.budgetLimit.toString(),
                            budgetLimitDate = budget.limitDate.toString()
                        )
                    }
                    val start = budget.createdAt.toLocalDate()
                    val end = budget.limitDate
                    dbRepository.getOutflowForCategory(budget.categoryId, start, end)
                }
                .collectLatest { spending ->
                    val budget = _uiState.value.budget ?: return@collectLatest
                    recomputeStats(budget, spending)
                }
        }
    }

    private fun recomputeStats(budget: Budget, spending: Double) {
        val today = LocalDate.now()
        val start = budget.createdAt.toLocalDate()
        val end = budget.limitDate

        val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
        val daysElapsed = ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, totalDays)
        val daysLeft = (totalDays - daysElapsed).coerceAtLeast(0)

        val percentUsed = if (budget.budgetLimit > 0)
            ((spending / budget.budgetLimit) * 100).roundToInt().coerceAtLeast(0)
        else 0

        val remaining = (budget.budgetLimit - spending).coerceAtLeast(0.0)
        val isOverBudget = spending > budget.budgetLimit
        val overage = if (isOverBudget) spending - budget.budgetLimit else 0.0

        val dailyBudget = if (daysLeft > 0) remaining / daysLeft else 0.0
        val dailyAvg = if (daysElapsed > 0) spending / daysElapsed else 0.0
        val projectedTotal = dailyAvg * totalDays

        _uiState.update {
            it.copy(
                actualSpending = spending,
                percentUsed = percentUsed,
                remaining = remaining,
                isOverBudget = isOverBudget,
                overage = overage,
                daysLeft = daysLeft,
                daysElapsed = daysElapsed,
                totalDays = totalDays,
                dailyBudget = dailyBudget,
                dailyAvg = dailyAvg,
                projectedTotal = projectedTotal
            )
        }
    }

    init {
        _uiState.update { it.copy(budgetId = budgetIdArg) }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
        }
        observeBudgetAndSpending()
    }
}
