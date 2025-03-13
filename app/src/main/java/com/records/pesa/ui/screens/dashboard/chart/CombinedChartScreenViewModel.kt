package com.records.pesa.ui.screens.dashboard.chart

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.db.DBRepository
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.Month

data class CombinedChartScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val moneyInPointsData: List<Point> = emptyList(),
    val moneyOutPointsData: List<Point> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val maxAmount: Float = 0.0f,
    val transactions: List<GroupedTransactionData> = emptyList(),
    val monthlyTransactions: List<MonthlyTransaction> = emptyList(),
    val searchText: String = "",
    val transactionType: String = "All types",
    val categoryId: String? = null,
    val budgetId: String? = null,
    val startDate: String = "",
    val endDate: String = "",
    val defaultStartDate: String? = null,
    val defaultEndDate: String? = null,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class CombinedChartScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(CombinedChartScreenUiState())
    val uiState: StateFlow<CombinedChartScreenUiState> = _uiState.asStateFlow()


    fun updateEntity(name: String) {
        _uiState.update {
            it.copy(
                searchText = name
            )
        }
    }

    fun updateStartDate(startDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = startDate.toString()
            )
        }
    }

    fun updateEndDate(endDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = endDate.toString()
            )
        }
    }

    fun updateTransactionType(transactionType: String) {
        _uiState.update {
            it.copy(
                transactionType = if(transactionType.lowercase() == "buy goods and services") "Buy Goods and Services (till)" else if(transactionType.lowercase() == "withdrawal") "Withdraw Cash" else transactionType
            )
        }
    }

    fun initialize(categoryId: String?, budgetId: String?, defaultStartDate: String?, defaultEndDate: String?) {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        _uiState.update {
            it.copy(
                categoryId = categoryId,
                budgetId = budgetId,
                startDate = defaultStartDate ?: startDate.toString(),
                endDate = defaultEndDate ?: endDate.toString(),
                defaultStartDate = defaultStartDate,
                defaultEndDate = defaultEndDate
            )
        }
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
        }

    }
    fun getUserDetails() {
        viewModelScope.launch {
            Log.d("USERS", dbRepository.getUsers().first()[0].toString())
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
        }
    }

    init {
        getUserDetails()
    }
}