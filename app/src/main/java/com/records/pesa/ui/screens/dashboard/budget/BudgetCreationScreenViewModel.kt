package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.BudgetEditPayLoad
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class BudgetCreationScreenUiState(
    val userDetails: UserDetails = UserDetails(),
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
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
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
        }
    }

    init {
        _uiState.update {
            it.copy(
                categoryId = categoryId
            )
        }
        getUserDetails()
    }
}