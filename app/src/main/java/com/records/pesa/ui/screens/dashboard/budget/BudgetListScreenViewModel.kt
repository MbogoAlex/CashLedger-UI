package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.category.CategoryService
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

enum class BudgetStatus { ON_TRACK, WARNING, EXCEEDED, EXPIRED }

data class BudgetWithProgress(
    val budget: Budget,
    val actualSpending: Double = 0.0,
    val percentUsed: Int = 0,
    val remaining: Double = 0.0,
    val isOverBudget: Boolean = false,
    val daysLeft: Int = 0,
    val status: BudgetStatus = BudgetStatus.ON_TRACK,
    val categoryName: String = ""
)

data class BudgetListScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryId: String? = null,
    val categoryName: String? = null,
    val searchQuery: String = "",
    val budgets: List<BudgetWithProgress> = emptyList(),
    val sortBy: String = "default",
    val filterStatus: String = "all",
    val isPremium: Boolean = false,
    val activeCount: Int = 0,
    val totalExceeded: Int = 0,
    val totalOverBudgetAmount: Double = 0.0
)

class BudgetListScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetListScreenUiState())
    val uiState: StateFlow<BudgetListScreenUiState> = _uiState.asStateFlow()

    private val categoryIdArg: String? = savedStateHandle[BudgetListScreenDestination.categoryId]
    private val categoryNameArg: String? = savedStateHandle[BudgetListScreenDestination.categoryName]

    fun updateSearchQuery(query: String) = _uiState.update { it.copy(searchQuery = query) }
    fun clearSearch() = _uiState.update { it.copy(searchQuery = "") }
    fun updateSortBy(sort: String) = _uiState.update { it.copy(sortBy = sort) }
    fun updateFilterStatus(filter: String) = _uiState.update { it.copy(filterStatus = filter) }

    fun deleteBudget(budget: Budget) {
        viewModelScope.launch {
            dbRepository.deleteBudget(budget)
            dataStoreRepository.touchLastLocalChange()
        }
    }

    fun undoDelete(budget: Budget) {
        viewModelScope.launch { dbRepository.insertBudget(budget) }
    }

    val filteredBudgets: List<BudgetWithProgress>
        get() {
            val q = _uiState.value.searchQuery.trim().lowercase()
            val filter = _uiState.value.filterStatus
            val sort = _uiState.value.sortBy

            var list = _uiState.value.budgets
            if (q.isNotEmpty()) list = list.filter {
                it.budget.name.contains(q, ignoreCase = true) || it.categoryName.contains(q, ignoreCase = true)
            }
            if (filter != "all") list = list.filter {
                when (filter) {
                    "on_track" -> it.status == BudgetStatus.ON_TRACK
                    "warning"  -> it.status == BudgetStatus.WARNING
                    "exceeded" -> it.status == BudgetStatus.EXCEEDED
                    "expired"  -> it.status == BudgetStatus.EXPIRED
                    else -> true
                }
            }
            return when (sort) {
                "most_used" -> list.sortedByDescending { it.percentUsed }
                "created"   -> list.sortedByDescending { it.budget.createdAt }
                else -> list
            }
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
                    val start = budget.startDate
                    val end = budget.limitDate
                    // budget.expenditure is the single source of truth — computed by BudgetRecalculationWorker
                    // (member-filtered, includes both M-PESA and manual transactions)
                    val spending = budget.expenditure
                    val totalDays = ChronoUnit.DAYS.between(start, end).toInt().coerceAtLeast(1)
                    val daysElapsed = ChronoUnit.DAYS.between(start, today).toInt().coerceIn(0, totalDays)
                    val daysLeft = (totalDays - daysElapsed).coerceAtLeast(0)
                    val percentUsed = if (budget.budgetLimit > 0)
                        ((spending / budget.budgetLimit) * 100).roundToInt().coerceAtLeast(0)
                    else 0
                    val remaining = (budget.budgetLimit - spending).coerceAtLeast(0.0)
                    val isOverBudget = spending > budget.budgetLimit
                    val isExpired = budget.limitDate.isBefore(today) && !isOverBudget
                    val status = when {
                        isOverBudget -> BudgetStatus.EXCEEDED
                        isExpired    -> BudgetStatus.EXPIRED
                        percentUsed >= 80 -> BudgetStatus.WARNING
                        else -> BudgetStatus.ON_TRACK
                    }
                    val catName = try {
                        if (budget.categoryId != null) categoryService.getCategoryById(budget.categoryId).first().category.name else "Standalone"
                    } catch (e: Exception) { "" }
                    BudgetWithProgress(
                        budget = budget,
                        actualSpending = spending,
                        percentUsed = percentUsed,
                        remaining = remaining,
                        isOverBudget = isOverBudget,
                        daysLeft = daysLeft,
                        status = status,
                        categoryName = catName
                    )
                }
                val active = withProgress.filter { it.budget.active && it.status != BudgetStatus.EXPIRED }
                val exceeded = withProgress.filter { it.status == BudgetStatus.EXCEEDED }
                val overAmount = exceeded.sumOf { it.actualSpending - it.budget.budgetLimit }
                _uiState.update {
                    it.copy(
                        budgets = withProgress,
                        activeCount = active.size,
                        totalExceeded = exceeded.size,
                        totalOverBudgetAmount = overAmount
                    )
                }
            }
        }
    }

    init {
        _uiState.update { it.copy(categoryId = categoryIdArg, categoryName = categoryNameArg) }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
        }
        observeBudgets()
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate != null && prefs.expiryDate.isAfter(java.time.LocalDateTime.now()))
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }
}
