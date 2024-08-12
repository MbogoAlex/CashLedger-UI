package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.db.DBRepository
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class DashboardScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val currentBalance: Double = 0.0,
    val month: String = "June",
    val year: String = "2024",
    val selectableMonths: List<String> = listOf("JANUARY", "FEBRUARY", "MARCH", "APRIL", "MAY", "JUNE", "JULY", "AUGUST", "SEPTEMBER", "OCTOBER", "NOVEMBER", "DECEMBER"),
    val selectableYears: List<String> = emptyList(),
    val monthlyInTotal: Double = 0.0,
    val monthlyOutTotal: Double = 0.0,
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val moneyInPointsData: List<Point> = emptyList(),
    val moneyOutPointsData: List<Point> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val maxAmount: Float = 0.0f,
    val transactions: List<TransactionItem> = emptyList(),
    val groupedTransactions: List<GroupedTransactionData> = emptyList(),
    val monthlyTransactions: List<MonthlyTransaction> = emptyList(),
    val categories: List<TransactionCategory> = emptyList()
)
class DashboardScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(DashboardScreenUiState())
    val uiState: StateFlow<DashboardScreenUiState> = _uiState.asStateFlow()

    fun setInitialDates() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        val currentYear = currentDate.year
        val lastYear = 2016
        val years = mutableListOf<String>()
        for (year in currentYear downTo lastYear) {
            years.add(year.toString())
        }
        _uiState.update {
            it.copy(
                startDate = firstDayOfMonth.toString(),
                endDate = currentDate.toString(),
                month = currentDate.month.toString(),
                year = currentDate.year.toString(),
                selectableYears = years
            )
        }
    }

    fun getCurrentBalance() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getCurrentBalance(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId
                )
                if(response.isSuccessful) {
                    Log.d("CUR_BALANCE", response.body()?.data?.balance.toString())
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
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
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
                val response = apiRepository.getUserCategories(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    categoryId = null,
                    name = null,
                    orderBy = "latest"
                )
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

    fun getGroupedTransactions() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        viewModelScope.launch {
            try {
                val response = apiRepository.getGroupedTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = null,
                    categoryId = null,
                    budgetId = null,
                    transactionType = null,
                    startDate = "2023-03-06",
                    endDate = "2024-06-25"
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

                    Log.i("TRANSACTIONS_SIZE", response.body()?.data?.transaction?.transactions!!.size.toString())

                    _uiState.update {
                        it.copy(
                            moneyInPointsData = moneyInPointsData,
                            moneyOutPointsData = moneyOutPointsData,
                            maxAmount = maxAmount,
                            groupedTransactions = response.body()?.data?.transaction?.transactions!!,
                        )
                    }
                } else {
                    Log.e("dataFetchFailResponseError", response.toString())
                }
            } catch (e: Exception) {

                Log.e("dataFetchFailException", e.toString())
            }
        }
    }

    fun updateMonth(month: String) {
        _uiState.update {
            it.copy(
                month = month
            )
        }
        getGroupedByMonthTransactions()
    }

    fun updateYear(year: String) {
        _uiState.update {
            it.copy(
                year = year
            )
        }
        getGroupedByMonthTransactions()
    }

    private fun getGroupedByMonthTransactions() {
        viewModelScope.launch {
            try {
               val response = apiRepository.getGroupedByMonthTransactions(
                   token = uiState.value.userDetails.token,
                   userId = uiState.value.userDetails.userId,
                   entity = null,
                   categoryId = null,
                   budgetId = null,
                   transactionType = null,
                   month = uiState.value.month,
                   year = uiState.value.year
               )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            monthlyTransactions = response.body()?.data?.transaction?.transactions!!
                        )
                    }
                    Log.d("groupedByMonthFetched", response.toString())
                } else {
                    Log.e("fetchGroupedByMonthTransactionsErrorResponse", response.toString())
                }
            } catch (e: Exception) {
                Log.e("fetchGroupedByMonthTransactionsException", e.toString())
            }
        }
    }

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getTransactions()
            getCurrentBalance()
            getCategories()
//            getGroupedTransactions()
            getGroupedByMonthTransactions()

        }
    }

    init {
        setInitialDates()
        getUserDetails()
    }
}