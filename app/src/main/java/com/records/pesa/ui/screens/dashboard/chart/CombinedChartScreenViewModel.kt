package com.records.pesa.ui.screens.dashboard.chart

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.db.DBRepository
import com.records.pesa.models.GroupedTransactionData
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.groupedTransactions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class CombinedChartScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val moneyInPointsData: List<Point> = emptyList(),
    val moneyOutPointsData: List<Point> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val maxAmount: Float = 0.0f,
    val transactions: List<GroupedTransactionData> = emptyList(),
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
        getGroupedTransactions()
    }

    fun updateStartDate(startDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = startDate.toString()
            )
        }
        getGroupedTransactions()
    }

    fun updateEndDate(endDate: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = endDate.toString()
            )
        }
        getGroupedTransactions()
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
            getGroupedTransactions()
        }

    }
    fun getGroupedTransactions() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getGroupedTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.searchText,
                    categoryId = uiState.value.categoryId?.toInt(),
                    budgetId = uiState.value.budgetId?.toInt(),
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate
                )
                if(response.isSuccessful) {
                    // Calculate the maximum value for moneyIn and moneyOut dynamically
                    val maxAmount = response.body()?.data?.transaction?.transactions!!.maxOf { maxOf(it.moneyIn, it.moneyOut) }
                    Log.i("MAX_VALUE", maxAmount.toString())

                    // Prepare data points for moneyIn and moneyOut
                    val moneyInPointsData: List<Point> = response.body()?.data?.transaction?.transactions!!.mapIndexed { index, transaction ->
                        Point(index.toFloat(), transaction.moneyIn)
                    }

                    val moneyOutPointsData: List<Point> = response.body()?.data?.transaction?.transactions!!.mapIndexed { index, transaction ->
                        Point(index.toFloat(), transaction.moneyOut)
                    }

                    _uiState.update {
                        it.copy(
                            moneyInPointsData = moneyInPointsData,
                            moneyOutPointsData = moneyOutPointsData,
                            maxAmount = maxAmount,
                            transactions = response.body()?.data?.transaction?.transactions!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                    Log.e("dataFetchFailResponseError", response.toString())
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.SUCCESS
                    )
                }
                Log.e("dataFetchFailException", e.toString())
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