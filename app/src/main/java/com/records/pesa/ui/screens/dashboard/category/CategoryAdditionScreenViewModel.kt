package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionTypeData
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDateTime

data class CategoryAdditionScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryName: String = "",
    val categoryId: Int = 0,
    val totalTransactions: Int = 0,
    val uncategorizedCount: Int = 0,
    val totalCategories: Int = 0,
    val typeBreakdown: List<TransactionTypeData> = emptyList(),
    val topUncategorizedEntities: List<AggregatedTransaction> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class CategoryAdditionScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService
): ViewModel() {
    private val _uiState = MutableStateFlow(CategoryAdditionScreenUiState())
    val uiState: StateFlow<CategoryAdditionScreenUiState> = _uiState.asStateFlow()

    fun updateCategoryName(name: String) {
        _uiState.update { it.copy(categoryName = name) }
    }

    fun createCategory() {
        _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
        viewModelScope.launch {
            val category = TransactionCategory(
                name = uiState.value.categoryName,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                updatedTimes = 0.0,
                contains = emptyList()
            )
            withContext(Dispatchers.IO) {
                try {
                    val categoryId = categoryService.insertTransactionCategory(category)
                    _uiState.update {
                        it.copy(
                            categoryId = categoryId.toInt(),
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } catch (e: Exception) {
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
                    Log.e("createCategoryException", e.toString())
                }
            }
        }
    }

    fun resetLoadingStatus() {
        _uiState.update { it.copy(loadingStatus = LoadingStatus.INITIAL) }
    }

    private fun loadInsights() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val total = dbRepository.getTotalTransactionCount()
                    val uncategorized = dbRepository.getUncategorizedTransactionCount()
                    val entities = dbRepository.getTopUncategorizedEntities()
                    val breakdown = dbRepository.getTransactionTypeBreakdown(
                        startDate = java.time.LocalDate.of(2000, 1, 1),
                        endDate = java.time.LocalDate.now()
                    )
                    val categories = categoryService.getAllCategories().first()
                    _uiState.update {
                        it.copy(
                            totalTransactions = total,
                            uncategorizedCount = uncategorized,
                            topUncategorizedEntities = entities,
                            typeBreakdown = breakdown,
                            totalCategories = categories.size
                        )
                    }
                } catch (e: Exception) {
                    Log.e("CategoryAdditionInsights", e.toString())
                }
            }
        }
    }

    private fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(userDetails = dbRepository.getUsers().first()[0])
            }
        }
    }

    init {
        getUserDetails()
        loadInsights()
    }
}