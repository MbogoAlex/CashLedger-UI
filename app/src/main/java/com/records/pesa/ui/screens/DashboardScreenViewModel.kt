package com.records.pesa.ui.screens

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.TransactionItem
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardScreenUiState(
    val currentBalance: Double = 0.0,
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val transactions: List<TransactionItem> = emptyList(),
    val categories: List<TransactionCategory> = emptyList()
)
@RequiresApi(Build.VERSION_CODES.O)
class DashboardScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
): ViewModel() {
    private val _uiState = MutableStateFlow(DashboardScreenUiState())
    val uiState: StateFlow<DashboardScreenUiState> = _uiState.asStateFlow()

    @RequiresApi(Build.VERSION_CODES.O)
    fun setInitialDates() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        _uiState.update {
            it.copy(
                startDate = startDate.toString(),
                endDate = endDate.toString(),
            )
        }
    }
    fun getCurrentBalance() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getCurrentBalance(1)
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            currentBalance = response.body()?.data?.balance!!
                        )
                    }
                } else {
                    Log.e("BALANCE_FETCH_ERROR_RESPONSE", response.toString())
                }

            } catch (e: Exception) {
                Log.e("BALANCE_FETCH_ERROR_EXCEPTION", e.toString())
            }
        }
    }

    fun getTransactions() {
        Log.i("LOADING_TRANSACTIONS", "LOADING TRANSACTIONS")
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    userId = 1,
                    entity = null,
                    categoryId = null,
                    budgetId = null,
                    transactionType = null,
                    latest = true,
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            transactions = response.body()?.data?.transaction?.transactions!!,
                        )
                    }
                } else {
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun  getCategories() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getUserCategories(1, null, null, "latest")
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

    init {
        setInitialDates()
        getTransactions()
        getCurrentBalance()
        getCategories()
    }
}