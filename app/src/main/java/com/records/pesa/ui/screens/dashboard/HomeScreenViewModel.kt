package com.records.pesa.ui.screens.dashboard

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.category.CategoryService
import com.records.pesa.ui.screens.dashboard.budget.BudgetStatus
import com.records.pesa.ui.screens.dashboard.budget.BudgetWithProgress
import com.records.pesa.workers.WorkersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.roundToInt

data class HomeScreenUiState(
    val darkMode: Boolean = false,
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
    val freeTrialDays: Int = 0,
    val screen: String? = null,
    val user: UserDetails? = null,
    val budgets: List<BudgetWithProgress> = emptyList(),
    val budgetActiveCount: Int = 0,
    val budgetExceededCount: Int = 0
)

class HomeScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val savedStateHandle: SavedStateHandle,
    private val workersRepository: WorkersRepository,
    private val categoryService: CategoryService
): ViewModel() {
    private val _uiState = MutableStateFlow(value = HomeScreenUiState())
    val uiState: StateFlow<HomeScreenUiState> = _uiState.asStateFlow()

    val screen: String? = savedStateHandle[HomeScreenDestination.screen]

    fun getUserDetails() {
        viewModelScope.launch {
            dbRepository.getUsers().collect() { userDetails ->
                Log.d("DB_USER", userDetails.toString())
                _uiState.update {
                    it.copy(
                        userDetails = if(userDetails.isNotEmpty()) userDetails[0] else UserDetails()
                    )
                }
            }
        }

        viewModelScope.launch {
            val user = dbRepository.getUsers().firstOrNull()?.get(0)
            Log.d("DB_USER", user.toString())
            _uiState.update {
                it.copy(
                    user = user
                )
            }
        }
    }

    fun backUpWorker() {
        viewModelScope.launch {
            Log.d("backUpWorker", "CAlling from dashboard")
            try {
                workersRepository.fetchAndBackupTransactions(
                    token = "dala",
                    userId = uiState.value.userDetails.backUpUserId.toInt(),
                    paymentStatus = uiState.value.userDetails.paymentStatus,
                    priorityHigh = false
                )
                dbRepository.updateUser(
                    uiState.value.userDetails.copy(
                        backupWorkerInitiated = true
                    )
                )
            } catch (e: Exception) {
                Log.e("backUpWorkerException", e.toString())
            }

        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dbRepository.getUserPreferences()?.collect { preferences ->
                        _uiState.update {
                            it.copy(
                                preferences = preferences ?: userPreferences
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }

    fun resetNavigationScreen() {
        _uiState.update {
            it.copy(
                screen = null
            )
        }
    }

    private fun observeBudgets() {
        viewModelScope.launch {
            dbRepository.getAllBudgets().collectLatest { budgets ->
                val today = LocalDate.now()
                val withProgress = budgets.map { budget ->
                    val start = budget.startDate
                    val end = budget.limitDate
                    val spending = if (budget.categoryId != null) {
                        dbRepository.getOutflowForCategory(budget.categoryId, start, end).first()
                    } else {
                        dbRepository.sumManualTransactionsForBudget(budget.id)
                    }
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
                        isOverBudget     -> BudgetStatus.EXCEEDED
                        isExpired        -> BudgetStatus.EXPIRED
                        percentUsed >= 80 -> BudgetStatus.WARNING
                        else             -> BudgetStatus.ON_TRACK
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
                val active = withProgress.filter { it.status != BudgetStatus.EXPIRED }
                val exceeded = withProgress.filter { it.status == BudgetStatus.EXCEEDED }
                _uiState.update {
                    it.copy(
                        budgets = withProgress,
                        budgetActiveCount = active.size,
                        budgetExceededCount = exceeded.size
                    )
                }
            }
        }
    }

    init {
        getUserDetails()
        getUserPreferences()
        observeBudgets()
        _uiState.update {
            it.copy(
                screen = screen
            )
        }
    }
}