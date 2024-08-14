package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.db.DBRepository
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.network.ApiRepository
import kotlinx.coroutines.Job
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
    val todayTotalIn: Double = 0.0,
    val todayTotalOut: Double = 0.0,
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val moneyInPointsData: List<Point> = emptyList(),
    val moneyOutPointsData: List<Point> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val maxAmount: Float = 0.0f,
    val firstTransactionDate: String = "",
    val transactions: List<TransactionItem> = emptyList(),
    val budgetList: List<BudgetDt> = emptyList(),
    val groupedTransactions: List<GroupedTransactionData> = emptyList(),
    val monthlyTransactions: List<MonthlyTransaction> = emptyList(),
    val appVersion: Double? = null,
    val categories: List<TransactionCategory> = emptyList()
)
class DashboardScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(DashboardScreenUiState())
    val uiState: StateFlow<DashboardScreenUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null

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

    fun getDashboardDetails() {
        viewModelScope.launch {
            try {
               val response = apiRepository.getDashboardDetails(
                   token = uiState.value.userDetails.token,
                   userId = uiState.value.userDetails.userId,
                   date = LocalDate.now().toString()
               )
               if(response.isSuccessful) {
                   _uiState.update {
                       it.copy(
                           todayTotalIn = response.body()?.data?.transaction?.todayExpenditure?.totalIn!!,
                           todayTotalOut = response.body()?.data?.transaction?.todayExpenditure?.totalOut!!,
                           transactions = response.body()?.data?.transaction?.latestTransactions!!,
                           categories = response.body()?.data?.transaction?.categories!!,
                           budgetList = response.body()?.data?.transaction?.budgets!!,
                           currentBalance = response.body()?.data?.transaction?.accountBalance!!,
                           firstTransactionDate = formatLocalDate(LocalDate.parse(response.body()?.data?.transaction?.firstTransactionDate!!))
                       )
                   }
               } else {
                   Log.e("DASHBOARD_DETAILS_RESPONSE_ERROR", response.toString())
               }
            } catch (e: Exception) {
                Log.e("DASHBOARD_DETAILS_EXCEPTION", e.toString())
            }
        }
    }

    fun updateMonth(month: String) {
        _uiState.update {
            it.copy(
                month = month
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            getGroupedByMonthTransactions()
        }
    }

    fun updateYear(year: String) {
        _uiState.update {
            it.copy(
                year = year
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            getGroupedByMonthTransactions()
        }
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
                            monthlyTransactions = response.body()?.data?.transaction?.transactions!!,
                            monthlyInTotal = response.body()?.data?.transaction?.totalMoneyIn!!,
                            monthlyOutTotal = response.body()?.data?.transaction?.totalMoneyOut!!
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

    fun checkAppVersion() {
        viewModelScope.launch {
            try {
                val response = apiRepository.checkAppVersion()
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            appVersion = response.body()?.data?.version
                        )
                    }
                }

            } catch (e: Exception) {

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
            getDashboardDetails()
            getGroupedByMonthTransactions()

        }
    }

    init {
        setInitialDates()
        getUserDetails()
        checkAppVersion()
    }
}