package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.BudgetCreationPayLoad
import com.records.pesa.models.TransactionCategory
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetCreationScreenUiState(
    val budgetName: String = "",
    val budgetLimit: String = "",
    val limitDate: LocalDate? = null,
    val categoryId: String? = null,
    val budgetId: String = "",
    val selectedCategory: TransactionCategory = transactionCategory,
    val categories: List<TransactionCategory> = emptyList(),
    val saveButtonEnabled: Boolean = false,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class BudgetCreationScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetCreationScreenUiState())
    val uiState: StateFlow<BudgetCreationScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[BudgetCreationScreenDestination.categoryId]

    fun updateCategory(category: TransactionCategory) {
        _uiState.update {
            it.copy(
                selectedCategory = category
            )
        }
    }
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

    fun updateLimitDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                limitDate = date
            )
        }
    }

    fun  getCategories() {
        Log.i("CATEGORY_ID:", uiState.value.categoryId.toString())
        viewModelScope.launch {
            try {
                val response = apiRepository.getUserCategories(1, uiState.value.categoryId?.toInt(), null, "latest")
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            categories = response.body()?.data?.category!!
                        )
                    }
                } else {
                    Log.e("GetCategoriesResponseError", response.toString())
                }
            } catch (e: Exception) {
                Log.e("GetCategoriesException", e.toString())
            }
        }
    }

    fun createBudget() {
        val budget = BudgetCreationPayLoad(
            name = uiState.value.budgetName,
            budgetLimit = uiState.value.budgetLimit.toDouble(),
            limitDate = uiState.value.limitDate.toString(),
            limitExceeded = false
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.createBudget(
                    userId = 1,
                    categoryId = uiState.value.selectedCategory.id,
                    budget = budget
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            budgetId = response.body()?.data?.budget?.id!!.toString(),
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("CreateBudgetResponseError", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("CreateBudgetException", e.toString())
            }
        }
    }

    fun checkIfFieldsAreFilled() {
        _uiState.update {
            it.copy(
                saveButtonEnabled = uiState.value.budgetName.isNotEmpty() &&
                        uiState.value.selectedCategory.name.isNotEmpty() &&
                        uiState.value.budgetLimit.isNotEmpty() &&
                        uiState.value.budgetLimit.toDouble() != 0.0 &&
                        uiState.value.limitDate != null
            )
        }
    }

    fun resetLoadingStatus() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.INITIAL
            )
        }
    }

    init {
        _uiState.update {
            it.copy(
                categoryId = categoryId
            )
        }
        getCategories()
    }
}