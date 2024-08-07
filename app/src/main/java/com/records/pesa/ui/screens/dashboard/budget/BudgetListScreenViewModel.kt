package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BudgetListScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val categoryId: String? = null,
    val categoryName: String? = null,
    val searchQuery: String = "",
    val budgetList: List<BudgetDt> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class BudgetListScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(BudgetListScreenUiState())
    val uiState: StateFlow<BudgetListScreenUiState> = _uiState.asStateFlow()

    private val categoryId: String? = savedStateHandle[BudgetListScreenDestination.categoryId]
    private val categoryName: String? = savedStateHandle[BudgetListScreenDestination.categoryName]

    fun updateSearchQuery(query: String) {
        _uiState.update {
            it.copy(
                searchQuery = query
            )
        }
        getBudgets()
    }

    fun clearSearch() {
        _uiState.update {
            it.copy(
                searchQuery = ""
            )
        }
        getBudgets()
    }

    fun getBudgets() {
        viewModelScope.launch {
            try {
                val response = if(uiState.value.categoryId != null) apiRepository.getCategoryBudgets(
                    token = uiState.value.userDetails.token,
                    categoryId = uiState.value.categoryId!!.toInt(),
                    name = uiState.value.searchQuery
                ) else apiRepository.getUserBudgets(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    name = uiState.value.searchQuery
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            budgetList = response.body()?.data?.budget!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getBudgetsErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("getBudgetsException", e.toString())
            }
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
            getBudgets()
        }
    }

    init {
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                categoryName = categoryName
            )
        }
        getUserDetails()

    }
}