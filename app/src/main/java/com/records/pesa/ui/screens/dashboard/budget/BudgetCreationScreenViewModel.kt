package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalDateTime

enum class BudgetType { CATEGORY, STANDALONE }

data class CategoryPickerItem(
    val id: Int,
    val name: String,
    val lastMonthSpend: Double = 0.0,
    val transactionCount: Int = 0
)

data class BudgetCreationScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val step: Int = 0,
    val budgetType: BudgetType = BudgetType.CATEGORY,
    val isPremium: Boolean = false,
    val showUpgradeDialog: Boolean = false,
    val budgetName: String = "",
    val budgetLimit: String = "",
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val limitDate: LocalDate? = null,
    val categoryId: String? = null,
    val categoryName: String? = null,
    val allCategories: List<CategoryPickerItem> = emptyList(),
    val categorySearch: String = "",
    val avgSpend3Months: Double = 0.0,
    val lastMonthSpend: Double = 0.0,
    val newBudgetId: Int = 0,
    val saveButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class BudgetCreationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetCreationScreenUiState())
    val uiState: StateFlow<BudgetCreationScreenUiState> = _uiState.asStateFlow()

    private val categoryIdArg: String? = savedStateHandle[BudgetCreationScreenDestination.categoryId]

    fun selectBudgetType(type: BudgetType) {
        if (type == BudgetType.STANDALONE && !_uiState.value.isPremium) {
            _uiState.update { it.copy(showUpgradeDialog = true) }
            return
        }
        _uiState.update { it.copy(budgetType = type, step = 1) }
    }

    fun dismissUpgradeDialog() = _uiState.update { it.copy(showUpgradeDialog = false) }

    fun goToStep0() = _uiState.update { it.copy(step = 0) }

    fun updateCategorySearch(query: String) = _uiState.update { it.copy(categorySearch = query) }

    fun selectCategory(item: CategoryPickerItem) {
        _uiState.update {
            it.copy(
                categoryId = item.id.toString(),
                categoryName = item.name,
                categorySearch = ""
            )
        }
        loadCategorySpendStats(item.id)
        checkFields()
    }

    fun clearCategory() {
        _uiState.update { it.copy(categoryId = null, categoryName = null, avgSpend3Months = 0.0, lastMonthSpend = 0.0) }
        checkFields()
    }

    fun updateBudgetName(name: String) { _uiState.update { it.copy(budgetName = name) }; checkFields() }
    fun updateBudgetLimit(amount: String) { _uiState.update { it.copy(budgetLimit = amount) }; checkFields() }
    fun updateStartDate(date: LocalDate) { _uiState.update { it.copy(startDate = date) }; checkFields() }
    fun updateLimitDate(date: LocalDate) { _uiState.update { it.copy(limitDate = date) }; checkFields() }
    fun resetLoadingStatus() = _uiState.update { it.copy(loadingStatus = LoadingStatus.INITIAL) }

    private fun loadCategorySpendStats(categoryId: Int) {
        viewModelScope.launch {
            val today = LocalDate.now()
            val lastMonthStart = today.minusMonths(1).withDayOfMonth(1)
            val lastMonthEnd = today.withDayOfMonth(1).minusDays(1)
            val lastMonthSpend = dbRepository.getOutflowForCategory(categoryId, lastMonthStart, lastMonthEnd).first()
            val threeMonthStart = today.minusMonths(3).withDayOfMonth(1)
            val threeMonthSpend = dbRepository.getOutflowForCategory(categoryId, threeMonthStart, lastMonthEnd).first()
            val avgSpend = threeMonthSpend / 3.0
            _uiState.update { it.copy(lastMonthSpend = lastMonthSpend, avgSpend3Months = avgSpend) }
        }
    }

    private fun checkFields() {
        val s = _uiState.value
        val categoryOk = s.budgetType == BudgetType.STANDALONE || !s.categoryId.isNullOrBlank()
        _uiState.update {
            it.copy(
                saveButtonEnabled = s.budgetName.isNotBlank() &&
                    s.budgetLimit.isNotBlank() &&
                    (s.budgetLimit.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    s.limitDate != null &&
                    s.limitDate.isAfter(s.startDate) &&
                    categoryOk
            )
        }
    }

    fun createBudget() {
        val s = _uiState.value
        val limit = s.budgetLimit.toDoubleOrNull() ?: return
        val endDate = s.limitDate ?: return
        if (s.budgetType == BudgetType.CATEGORY) {
            val catId = s.categoryId?.toIntOrNull() ?: return
            viewModelScope.launch {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
                try {
                    val budget = Budget(
                        name = s.budgetName.trim(),
                        active = true,
                        expenditure = 0.0,
                        budgetLimit = limit,
                        createdAt = LocalDateTime.now(),
                        startDate = s.startDate,
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
        // STANDALONE: future sprint (needs DB schema change)
    }

    init {
        val hasCategoryArg = categoryIdArg?.toIntOrNull() != null
        _uiState.update {
            it.copy(
                categoryId = categoryIdArg,
                step = if (hasCategoryArg) 1 else 0
            )
        }
        viewModelScope.launch {
            _uiState.update { it.copy(userDetails = dbRepository.getUsers().first()[0]) }
            val catId = categoryIdArg?.toIntOrNull()
            if (catId != null) {
                val category = categoryService.getRawCategoryById(catId).first()
                _uiState.update { it.copy(categoryName = category.name) }
                loadCategorySpendStats(catId)
            }
            categoryService.getAllCategories().collect { categories ->
                val today = LocalDate.now()
                val lastMonthStart = today.minusMonths(1).withDayOfMonth(1)
                val lastMonthEnd = today.withDayOfMonth(1).minusDays(1)
                val items = categories.map { cat ->
                    val spend = dbRepository.getOutflowForCategory(cat.category.id, lastMonthStart, lastMonthEnd).first()
                    CategoryPickerItem(
                        id = cat.category.id,
                        name = cat.category.name,
                        lastMonthSpend = spend,
                        transactionCount = cat.transactions.size
                    )
                }
                _uiState.update { it.copy(allCategories = items) }
            }
        }
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate != null && prefs.expiryDate.isAfter(LocalDateTime.now()))
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
    }
}
