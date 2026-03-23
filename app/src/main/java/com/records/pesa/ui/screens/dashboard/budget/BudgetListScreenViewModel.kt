package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class BudgetWithProgress(
    val budget: Budget,
    val actualSpending: Double = 0.0,
    val percentUsed: Int = 0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false,
    val daysLeft: Int = 0
)

data class BudgetListScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryId: String? = null,
    val categoryName: String? = null,
    val searchQuery: String = "",
    val budgets: List<BudgetWithProgress> = emptyList()
)

class BudgetListScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetListScreenUiState())
    val uiState: StateFlow<BudgetListScreenUiState> = _uiState.asStateFlow()

    private val categoryIdArg: String? = savedStateHandle[BudgetListScreenDestination.categoryId]
    private val categoryNameArg: String? = savedStateHandle[BudgetListScreenDestination.categoryName]

    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun clearSearch() = _uiState.update { it.copy(searchQuery = "") }

    val filteredBudgets: List<BudgetWithProgress>
        get() {
            val q = _uiState.value.searchQuery.trim().lowercase()
            return if (q.isEmpty()) _uiState.value.budgets
            else _uiState.value.budgets.filter { it.budget.name.contains(q, ignoreCase = true) }
        }

    private fun observeBudgets() {
        val catId = categoryIdArg?.toIntOrNull()
        viewModelScope.launch {
            val budgetFlow = if (catId != null)
                dbRepository.getBudgetsByCategoryId(catId)
            else
                dbRepository.getAllBudgets()

            budgetFlow.collectLatest { budgets ->
                val today = LocalDate.now()
                val withProgress = budgets.map { budget ->
                    val start = budget.createdAt.toLocalDate()
                    val end = budget.limitDate
                    val spending = dbRepository.getOutflowForCategory(budget.categoryId, start, end).first()
                    val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
                    val daysElapsed = ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, totalDays)
                    val daysLeft = (totalDays - daysElapsed).coerceAtLeast(0)
                    val percentUsed = if (budget.budgetLimit > 0)
                        ((spending / budget.budgetLimit) * 100).roundToInt().coerceAtLeast(0)
                    else 0
                    val remaining = (budget.budgetLimit - spending).coerceAtLeast(0.0)
                    BudgetWithProgress(
                        budget = budget,
                        actualSpending = spending,
                        percentUsed = percentUsed,
                        remaining = remaining,
                        isOverBudget = spending > budget.budgetLimit,
                        daysLeft = daysLeft
                    )
                }.sortedWith(
                    compareByDescending<BudgetWithProgress> { it.isOverBudget }
                        .thenByDescending { it.percentUsed }
                )
                _uiState.update { it.copy(budgets = withProgress) }
            }
        }
    }

    init {
        _uiState.update { it.copy(categoryId = categoryIdArg, categoryName = categoryNameArg) }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
        }
        observeBudgets()
    }
}
