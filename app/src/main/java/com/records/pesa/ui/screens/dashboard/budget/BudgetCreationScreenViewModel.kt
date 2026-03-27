package com.records.pesa.ui.screens.dashboard.budget

import android.app.Application
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.Budget
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.functions.RecurrenceHelper
import com.records.pesa.functions.RecurrenceType
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import com.records.pesa.workers.BudgetRecalculationWorker
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
    val step: Int = 1,
    val budgetType: BudgetType = BudgetType.CATEGORY,
    val isPremium: Boolean = false,
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
    val alertThreshold: Int = 80,
    val newBudgetId: Int = 0,
    val saveButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val categoryMembers: List<String> = emptyList(),
    val selectedMembers: Set<String> = emptySet(),
    // Recurrence
    val isRecurring: Boolean = false,
    val recurrenceType: String = RecurrenceType.MONTHLY.name,
    val recurrenceIntervalDays: Int = 30,
    val nextCyclePreviewEnd: LocalDate? = null,  // preview of when 2nd cycle ends
)

class BudgetCreationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository,
    private val application: Application,
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetCreationScreenUiState())
    val uiState: StateFlow<BudgetCreationScreenUiState> = _uiState.asStateFlow()

    private val categoryIdArg: String? = savedStateHandle[BudgetCreationScreenDestination.categoryId]

    fun selectBudgetType(type: BudgetType) {
        _uiState.update { it.copy(budgetType = type) }
    }

    fun dismissUpgradeDialog() = Unit

    fun goToStep0() = Unit

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
        loadCategoryMembers(item.id)
        checkFields()
    }

    fun clearCategory() {
        _uiState.update { it.copy(categoryId = null, categoryName = null, avgSpend3Months = 0.0, lastMonthSpend = 0.0) }
        checkFields()
    }

    fun updateBudgetName(name: String) { _uiState.update { it.copy(budgetName = name) }; checkFields() }
    fun updateBudgetLimit(amount: String) { _uiState.update { it.copy(budgetLimit = amount) }; checkFields() }
    fun updateStartDate(date: LocalDate) {
        _uiState.update { it.copy(startDate = date) }
        // If recurring, auto-compute end date to match the recurrence type
        autoSetLimitDateIfRecurring()
        checkFields()
    }
    fun updateLimitDate(date: LocalDate) {
        // Only allow manual end date when NOT recurring
        if (!_uiState.value.isRecurring) {
            _uiState.update { it.copy(limitDate = date) }
            refreshRecurrencePreview()
            checkFields()
        }
    }
    fun resetLoadingStatus() = _uiState.update { it.copy(loadingStatus = LoadingStatus.INITIAL) }

    fun toggleRecurring() {
        _uiState.update { it.copy(isRecurring = !it.isRecurring, recurrenceType = if (!it.isRecurring) RecurrenceType.MONTHLY.name else it.recurrenceType) }
        autoSetLimitDateIfRecurring()
    }

    fun setRecurrenceType(type: String) {
        _uiState.update { it.copy(recurrenceType = type) }
        autoSetLimitDateIfRecurring()
    }

    fun setRecurrenceIntervalDays(days: Int) {
        _uiState.update { it.copy(recurrenceIntervalDays = days.coerceAtLeast(1)) }
        autoSetLimitDateIfRecurring()
    }

    /** When recurring is on, lock the end date to match the recurrence type from the start date. */
    private fun autoSetLimitDateIfRecurring() {
        val s = _uiState.value
        if (!s.isRecurring || s.recurrenceType.isNullOrBlank()) {
            refreshRecurrencePreview()
            return
        }
        val autoEnd = RecurrenceHelper.nextCycleEndDate(s.startDate, s.recurrenceType, s.recurrenceIntervalDays.takeIf { it > 0 })
        _uiState.update { it.copy(limitDate = autoEnd) }
        refreshRecurrencePreview()
        checkFields()
    }

    fun setAlertThreshold(value: Int) {
        if (!_uiState.value.isPremium) return
        _uiState.update { it.copy(alertThreshold = value) }
    }

    private fun refreshRecurrencePreview() {
        val s = _uiState.value
        val end = s.limitDate
        if (!s.isRecurring || end == null || s.recurrenceType.isNullOrBlank()) {
            _uiState.update { it.copy(nextCyclePreviewEnd = null) }
            return
        }
        val nextStart = RecurrenceHelper.nextCycleStartDate(end)
        val nextEnd = RecurrenceHelper.nextCycleEndDate(nextStart, s.recurrenceType, s.recurrenceIntervalDays.takeIf { it > 0 })
        _uiState.update { it.copy(nextCyclePreviewEnd = nextEnd) }
    }

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

    fun loadCategoryMembers(categoryId: Int) {
        viewModelScope.launch {
            val categoryWithKeywords = categoryService.getCategoryById(categoryId).first()
            val keywords = categoryWithKeywords.keywords.map { it.keyword }
            val manualMembers = dbRepository.getManualMembersForCategory(categoryId).first()
            val allMembers = (keywords + manualMembers.map { it.name }).distinct()
            _uiState.update { it.copy(categoryMembers = allMembers, selectedMembers = emptySet()) }
        }
    }

    fun toggleMember(name: String) {
        val current = _uiState.value.selectedMembers
        _uiState.update {
            it.copy(selectedMembers = if (name in current) current - name else current + name)
        }
    }

    private fun checkFields() {
        val s = _uiState.value
        _uiState.update {
            it.copy(
                saveButtonEnabled = s.budgetName.isNotBlank() &&
                    s.budgetLimit.isNotBlank() &&
                    (s.budgetLimit.toDoubleOrNull() ?: 0.0) > 0.0 &&
                    s.limitDate != null &&
                    !s.limitDate.isBefore(s.startDate) && // allow same day (daily recurrence)
                    !s.categoryId.isNullOrBlank() &&
                    (!s.isRecurring || !s.recurrenceType.isNullOrBlank())
            )
        }
    }

    fun createBudget() {
        val s = _uiState.value
        val limit = s.budgetLimit.toDoubleOrNull() ?: return
        val endDate = s.limitDate ?: return
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
                    categoryId = catId,
                    alertThreshold = s.alertThreshold,
                    isRecurring = s.isRecurring,
                    recurrenceType = if (s.isRecurring) s.recurrenceType else null,
                    recurrenceIntervalDays = if (s.isRecurring && s.recurrenceType == RecurrenceType.CUSTOM.name) s.recurrenceIntervalDays else null,
                    cycleNumber = 1,
                )
                val newId = dbRepository.insertBudget(budget)
                val members = s.selectedMembers.map { name ->
                    com.records.pesa.db.models.BudgetMember(budgetId = newId.toInt(), memberName = name)
                }
                if (members.isNotEmpty()) {
                    dbRepository.insertBudgetMembers(members)
                }
                // Immediately calculate expenditure so the budget screen shows real figures
                WorkManager.getInstance(application).enqueueUniqueWork(
                    "budget_recalc_on_create",
                    ExistingWorkPolicy.REPLACE,
                    OneTimeWorkRequestBuilder<BudgetRecalculationWorker>().build()
                )
                _uiState.update { it.copy(loadingStatus = LoadingStatus.SUCCESS, newBudgetId = newId.toInt()) }
            } catch (e: Exception) {
                _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
            }
        }
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
                loadCategoryMembers(catId)
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
