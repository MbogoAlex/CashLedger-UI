package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.db.DBRepository
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.mapper.toResponseTransactionCategory
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.absoluteValue

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
    val categories: List<TransactionCategory> = emptyList(),
    val sortedTransactionItems: List<SortedTransactionItem> = emptyList(),
)
class DashboardScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService
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

    fun initializeValues() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val today = transactionService.getTodayExpenditure(LocalDate.now()).first()
                    val currentBalance = transactionService.getCurrentBalance().first()
                    val firstTransaction = transactionService.getFirstTransaction().first()
                    _uiState.update {
                        it.copy(
                            totalMoneyIn = today.totalIn,
                            totalMoneyOut = today.totalOut,
                            currentBalance = currentBalance,
                            firstTransactionDate = formatLocalDate(firstTransaction.date)
                        )
                    }
                } catch (e: Exception) {
                    Log.e("INITIALIZATION_ERROR", e.toString())
                }
            }
        }
    }

    fun getMoneyInAndOutToday() {
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = null,
            categoryId = null,
            budgetId = null,
            transactionType = null,
            moneyDirection = null,
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getUserTransactions(query).collect() {transactions ->
                        transformTransactions2(transactions.map { transactionWithCategories -> transactionWithCategories.toTransactionItem() })
                    }

                } catch (e: Exception) {
                    Log.e("failedToLoadGroupedTransactions", e.toString())
                }
            }
        }
    }

    fun getLatestTransactions() {
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails.userId,
            entity = null,
            categoryId = null,
            budgetId = null,
            transactionType = null,
            moneyDirection = null,
            startDate = LocalDate.now().minusYears(10),
            endDate = LocalDate.now(),
            latest = true
        )
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                  transactionService.getUserTransactions(query).collect() {transactions ->
                      _uiState.update {
                          it.copy(
                              transactions = transactions.map { transaction -> transaction.transaction.toTransactionItem() }
                          )
                      }
                  }
                } catch (e: Exception) {
                    Log.e("FAILED_TO_LOAD_TRANSACTIONS", e.toString())
                }
            }
        }
    }

    fun getCategories() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    categoryService.getAllCategories().collect(){ categories ->
                        _uiState.update {
                            it.copy(
                                categories = categories.map { category -> category.toResponseTransactionCategory() }
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
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
            getGroupedTransactions()
//            getGroupedByMonthTransactions()
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
            getGroupedTransactions()
//            getGroupedByMonthTransactions()
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
//            getDashboardDetails()
            initializeValues()
            getLatestTransactions()
            getCategories()
//            getGroupedByMonthTransactions()
            getGroupedTransactions()
//            apiRepository.getSubscriptionStatus(uiState.value.userDetails.userId)

        }
    }

    fun getGroupedTransactions() {
        viewModelScope.launch {
            val query = transactionService.createUserTransactionQueryByMonthAndYear(
                userId = uiState.value.userDetails.userId,
                entity = null,
                categoryId = null,
                budgetId = null,
                transactionType = null,
                moneyDirection = null,
                month = uiState.value.month,
                year = uiState.value.year.toInt(),
                latest = true
            )
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getUserTransactionsFilteredByMonthAndYear(query).collect(){transactions ->
                        transformTransactions(transactions.map { it.toTransactionItem() })
                    }
                } catch (e: Exception) {
                    Log.e("FAILED_TO_LOAD_GROUPED_TRANSACTIONS", e.toString())
                }
            }
        }
    }

    fun transformTransactions(transactions: List<TransactionItem>) {
        // Initialize totalMoneyIn and totalMoneyOut
        var totalMoneyIn = 0.0
        var totalMoneyOut = 0.0

        // Group transactions by entity
        val groupedByEntity = transactions.groupBy { it.entity }

        // Map each group to a SortedTransactionItem
        val sortedTransactionItems = groupedByEntity.map { (entity, groupedTransactions) ->
            val times = groupedTransactions.size
            val timesOut = groupedTransactions.count { it.transactionAmount < 0 }
            val timesIn = groupedTransactions.count { it.transactionAmount > 0 }
            val totalOut = groupedTransactions.filter { it.transactionAmount < 0 }
                .sumOf { it.transactionAmount.absoluteValue }
            val totalIn = groupedTransactions.filter { it.transactionAmount > 0 }
                .sumOf { it.transactionAmount }

            // Update the totals
            totalMoneyIn += totalIn
            totalMoneyOut += totalOut

            val transactionCost = groupedTransactions.sumOf { it.transactionCost }
            val nickName = groupedTransactions.firstOrNull { it.nickName != null }?.nickName

            SortedTransactionItem(
                transactionType = groupedTransactions.first().transactionType,
                times = times,
                timesOut = timesOut,
                timesIn = timesIn,
                totalOut = totalOut,
                totalIn = totalIn,
                transactionCost = transactionCost,
                entity = entity,
                nickName = nickName
            )
        }

        val sortedInDescendingOrder = sortedTransactionItems.sortedByDescending {
            it.totalIn + it.totalOut.absoluteValue
        }

        _uiState.update {
            it.copy(
                sortedTransactionItems = sortedInDescendingOrder,
                monthlyInTotal = totalMoneyIn,
                monthlyOutTotal = totalMoneyOut,
            )
        }

        // You can use totalMoneyIn and totalMoneyOut as needed in your code
        println("Total Money In: $totalMoneyIn")
        println("Total Money Out: $totalMoneyOut")

    }

    fun transformTransactions2(transactions: List<TransactionItem>) {
        Log.d("todayTransactions", transactions.toString())
        // Initialize totalMoneyIn and totalMoneyOut
        var totalMoneyIn = 0.0
        var totalMoneyOut = 0.0

        for(transaction in transactions) {
            if(transaction.transactionAmount < 0) {
                totalMoneyOut = totalMoneyOut.plus(transaction.transactionAmount.absoluteValue)
            } else if(transaction.transactionAmount > 0) {
                totalMoneyIn = totalMoneyIn.plus(transaction.transactionAmount)
            }
        }

        _uiState.update {
            it.copy(
                todayTotalIn = totalMoneyIn,
                todayTotalOut = totalMoneyOut
            )
        }

    }

    private fun initialzeApp() {
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(2000)
            }
            getLatestTransactions()
            getMoneyInAndOutToday()
        }
    }

    init {
        setInitialDates()
        getUserDetails()
        initialzeApp()
//        checkAppVersion()
    }
}