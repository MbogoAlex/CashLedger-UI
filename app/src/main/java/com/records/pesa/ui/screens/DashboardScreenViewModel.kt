package com.records.pesa.ui.screens

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.yml.charts.common.model.Point
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.mapper.toResponseTransactionCategory
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.BudgetDt
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.auth.AuthenticationManager
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.workers.TransactionInsertedEvent
import com.records.pesa.workers.WorkersRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.absoluteValue

data class DashboardScreenUiState(
    val userDetails: UserDetails? = null,
    val preferences: UserPreferences = userPreferences,
    val userPassword: String = "",
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
    val showSubscriptionExpiredDialog: Boolean = false,
    val showSubscriptionActivatedDialog: Boolean = false,
    // Time Period Selector fields
    val selectedTimePeriod: TimePeriod = TimePeriod.TODAY,
    val availableYears: List<Int> = emptyList(),
    val periodTotalIn: Double = 0.0,
    val periodTotalOut: Double = 0.0,
    val transactionTypeBreakdown: List<TransactionTypeSummary> = emptyList(),
    val moneyInCategories: List<TransactionTypeSummary> = emptyList(),
    val moneyOutCategories: List<TransactionTypeSummary> = emptyList(),
    val periodTransactions: List<TransactionItem> = emptyList(),
    val navigateToTransactionId: Int? = null,
    val hasExistingTransactions: Boolean? = null,  // null = not yet checked
)
class DashboardScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val dataStoreRepository: DataStoreRepository,
    private val transactionService: TransactionService,
    private val categoryService: CategoryService,
    private val workersRepository: WorkersRepository,
    private val authenticationManager: AuthenticationManager
): ViewModel() {
    private val _uiState = MutableStateFlow(DashboardScreenUiState())
    val uiState: StateFlow<DashboardScreenUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null
    private var balanceJob: Job? = null
    private var todayExpenditureJob: Job? = null
    private var initialPaidStatus: Boolean? = null

    fun updatePassword(password: String) {
        _uiState.update {
            it.copy(
                userPassword = password
            )
        }
    }

    private fun setInitialDates() {
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

    fun backUpWorker() {
        viewModelScope.launch {
            Log.d("backUpWorker", "CAlling from dashboard")
            try {
                workersRepository.fetchAndBackupTransactions(
                    token = "dala",
                    userId = uiState.value.userDetails!!.dynamoUserId?.toInt() ?: uiState.value.userDetails!!.phoneNumber.toInt(),
                    paymentStatus = uiState.value.userDetails!!.paymentStatus,
                    priorityHigh = false
                )
                dbRepository.updateUser(
                    uiState.value.userDetails!!.copy(
                        backupWorkerInitiated = true
                    )
                )
            } catch (e: Exception) {
                Log.e("backUpWorkerException", e.toString())

            }

        }
    }




    fun initializeValues() {
        // Live balance — re-emits from Room whenever any transaction is inserted/updated/deleted
        balanceJob?.cancel()
        balanceJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionService.getCurrentBalance().collect { balance ->
                    _uiState.update { it.copy(currentBalance = balance) }
                }
            } catch (e: Exception) {
                Log.e("INITIALIZATION_ERROR", e.toString())
            }
        }

        // Live today totals — re-emits from Room on every transaction change for today
        todayExpenditureJob?.cancel()
        todayExpenditureJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                transactionService.getTodayExpenditure(LocalDate.now()).collect { today ->
                    _uiState.update {
                        it.copy(
                            totalMoneyIn = today.totalIn,
                            totalMoneyOut = today.totalOut,
                            todayTotalIn = today.totalIn,
                            todayTotalOut = today.totalOut,
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("INITIALIZATION_ERROR", e.toString())
            }
        }

        // First transaction date — one-shot is fine, rarely changes
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val firstTransaction = transactionService.getFirstTransaction().first()
                _uiState.update { it.copy(firstTransactionDate = formatLocalDate(firstTransaction.date)) }
            } catch (e: Exception) {
                Log.e("INITIALIZATION_ERROR", e.toString())
            }
        }
    }

    fun getMoneyInAndOutToday() {
        val query = transactionService.createUserTransactionQuery(
            userId = uiState.value.userDetails!!.backUpUserId.toInt(),
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
            userId = uiState.value.userDetails!!.backUpUserId.toInt(),
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

    fun getUserDetails() {
        viewModelScope.launch(Dispatchers.IO) {
            dbRepository.getUser()?.collect { user ->
                _uiState.update {
                    it.copy(
                        userDetails = user
                    )
                }
            }

        }
    }

    private fun loadStartupData() {
        viewModelScope.launch {
            while (uiState.value.userDetails == null) {
                delay(1000)
            }
            backUpWorker()
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
                userId = uiState.value.userDetails!!.backUpUserId.toInt(),
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

    fun changeBalanceVisibility() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val updatedPrefs = uiState.value.preferences.copy(
                    showBalance = !uiState.value.preferences.showBalance
                )
                dbRepository.updateUserPreferences(updatedPrefs)
                dataStoreRepository.saveUserPreferences(updatedPrefs)
            }
        }
    }

    private fun checkSubscriptionStatus() {

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    // Wait until initial preferences have been loaded
                    while (initialPaidStatus == null) {
                        delay(500)
                    }

                    val response = authenticationManager.executeWithAuth { token ->
                        apiRepository.getUserSubscription(
                            token = token,
                            containerId = 1
                        )
                    }

                    if(response?.isSuccessful == true) {
                        val subscription = response.body()?.data
                        Log.d("ManualSubscriptionCheck", subscription.toString())

                        if(subscription != null) {
                            val prefs = uiState.value.preferences.copy(
                                paid = true,
                                paidAt = if (subscription.paidAt != null) LocalDateTime.parse(subscription.paidAt) else null,
                                expiryDate = if (subscription.expiredAt != null) LocalDateTime.parse(subscription.expiredAt) else null,
                                permanent = subscription.subscriptionPackageId == 4
                            )
                            dataStoreRepository.saveUserPreferences(prefs)
                            dbRepository.updateUserPreferences(prefs)

                            if (initialPaidStatus == false) {
                                _uiState.update { it.copy(showSubscriptionActivatedDialog = true) }
                            }
                        } else {
                            val prefs = uiState.value.preferences.copy(
                                paid = false,
                                permanent = false
                            )
                            dataStoreRepository.saveUserPreferences(prefs)
                            dbRepository.updateUserPreferences(prefs)

                            if (initialPaidStatus == true) {
                                _uiState.update { it.copy(showSubscriptionExpiredDialog = true) }
                            }
                        }
                    } else {
                        Log.e("ManualSubscriptionCheck", "API call failed: ${response?.code()}")
                    }
                } catch (e: Exception) {
                    Log.e("ManualSubscriptionCheck", "Exception: $e")
                }
            }
        }
    }

    private fun getUserPreferences() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    dataStoreRepository.getUserPreferences().collect { preferences ->
                        if (initialPaidStatus == null) {
                            initialPaidStatus = preferences.paid
                        }
                        _uiState.update {
                            it.copy(
                                preferences = preferences
                            )
                        }
                    }
                } catch (e: Exception) {

                }
            }
        }
    }


    fun dismissSubscriptionExpiredDialog() {
        _uiState.update { it.copy(showSubscriptionExpiredDialog = false) }
    }

    fun dismissSubscriptionActivatedDialog() {
        _uiState.update { it.copy(showSubscriptionActivatedDialog = false) }
    }

    fun clearTransactionNavigation() {
        _uiState.update { it.copy(navigateToTransactionId = null) }
    }

    private fun checkHasExistingTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val count = dbRepository.getTotalTransactionCount()
                _uiState.update { it.copy(hasExistingTransactions = count > 0) }
            } catch (e: Exception) {
                _uiState.update { it.copy(hasExistingTransactions = false) }
            }
        }
    }

    private fun initialzeApp() {
        viewModelScope.launch {
            while (uiState.value.userDetails == null) {
                delay(2000)
            }
            getLatestTransactions()
            getMoneyInAndOutToday()
        }
    }

    // ===== Time Period Selector Functions =====
    
    /**
     * Load available years that have transactions
     */
    private fun loadAvailableYears() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                dbRepository.getDistinctYearsWithTransactions().collect { years ->
                    _uiState.update { it.copy(availableYears = years) }
                }
            } catch (e: Exception) {
                Log.e("LOAD_YEARS_ERROR", e.toString())
            }
        }
    }
    
    /**
     * Update selected time period and recalculate totals
     */
    fun updateSelectedPeriod(period: TimePeriod) {
        val (start, end) = period.getDateRange()
        _uiState.update {
            it.copy(
                selectedTimePeriod = period,
                startDate = start.toString(),
                endDate = end.toString()
            )
        }
        calculatePeriodTotals()
        calculateTransactionTypeBreakdown()
        calculatePeriodTransactions()
        calculateChartData()
    }

    fun changeStartDate(date: LocalDate) {
        _uiState.update {
            it.copy(selectedTimePeriod = TimePeriod.CUSTOM, startDate = date.toString())
        }
        calculatePeriodTotals()
        calculateTransactionTypeBreakdown()
        calculatePeriodTransactions()
        calculateChartData()
    }

    fun changeEndDate(date: LocalDate) {
        _uiState.update {
            it.copy(selectedTimePeriod = TimePeriod.CUSTOM, endDate = date.toString())
        }
        calculatePeriodTotals()
        calculateTransactionTypeBreakdown()
        calculatePeriodTransactions()
        calculateChartData()
    }
    
    /**
     * Calculate total IN and OUT for the selected period
     */
    private fun calculatePeriodTotals() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startDate = LocalDate.parse(uiState.value.startDate)
                val endDate = LocalDate.parse(uiState.value.endDate)
                Log.d("PERIOD_TOTALS", "Period: ${uiState.value.selectedTimePeriod.getDisplayName()}, Start: $startDate, End: $endDate")
                
                val totalIn = dbRepository.getTotalInForPeriod(startDate, endDate)
                val totalOut = dbRepository.getTotalOutForPeriod(startDate, endDate)
                
                Log.d("PERIOD_TOTALS", "Money In: $totalIn, Money Out: $totalOut")
                
                _uiState.update {
                    it.copy(
                        periodTotalIn = totalIn,
                        periodTotalOut = totalOut
                    )
                }
            } catch (e: Exception) {
                Log.e("CALCULATE_PERIOD_TOTALS_ERROR", e.toString())
            }
        }
    }
    
    /**
     * Calculate transaction type breakdown for the selected period
     */
    private fun calculateTransactionTypeBreakdown() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startDate = LocalDate.parse(uiState.value.startDate)
                val endDate = LocalDate.parse(uiState.value.endDate)
                Log.d("BREAKDOWN_DATES", "Period: ${uiState.value.selectedTimePeriod.getDisplayName()}, Start: $startDate, End: $endDate")
                
                // Get actual transactions to group by type AND sign (like TransactionTypesScreen does)
                dbRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                    Log.d("BREAKDOWN_DATA", "Found ${transactions.size} transactions")
                    
                    // Group by type AND whether amount is positive or negative
                    val grouped = transactions
                        .groupBy { Pair(it.transactionType, it.transactionAmount >= 0) }
                        .map { (key, groupedTransactions) ->
                            val (transactionType, isPositive) = key
                            val totalAmount = groupedTransactions.sumOf { it.transactionAmount.absoluteValue }
                            TransactionTypeSummary(
                                transactionType = transactionType,
                                totalAmount = totalAmount,
                                transactionCount = groupedTransactions.size,
                                percentageOfTotal = 0f, // Not needed for dashboard
                                icon = null
                            ) to isPositive
                        }
                    
                    // Separate into money IN (positive) and money OUT (negative)
                    val moneyInTypes = grouped
                        .filter { it.second } // isPositive = true
                        .map { it.first }
                        .sortedByDescending { it.totalAmount }
                    
                    val moneyOutTypes = grouped
                        .filter { !it.second } // isPositive = false
                        .map { it.first }
                        .sortedByDescending { it.totalAmount }
                    
                    Log.d("BREAKDOWN_DATA", "Money IN types: ${moneyInTypes.size}, Money OUT types: ${moneyOutTypes.size}")
                    
                    _uiState.update {
                        it.copy(
                            moneyInCategories = moneyInTypes,
                            moneyOutCategories = moneyOutTypes
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CALCULATE_TYPE_BREAKDOWN_ERROR", e.toString())
            }
        }
    }
    
    /**
     * Calculate chart data points for the selected period
     */
    private fun calculateChartData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startDate = LocalDate.parse(uiState.value.startDate)
                val endDate = LocalDate.parse(uiState.value.endDate)
                Log.d("CHART_DATA", "Calculating chart data from $startDate to $endDate")
                
                dbRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                    Log.d("CHART_DATA", "Found ${transactions.size} transactions")
                    
                    // Group transactions by date
                    val groupedByDate = transactions.groupBy { it.date }
                    
                    // Sort dates and create points
                    val sortedDates = groupedByDate.keys.sorted()
                    Log.d("CHART_DATA", "Grouped into ${sortedDates.size} dates")
                    
                    val moneyInPoints = sortedDates.mapIndexed { index, date ->
                        val dayTransactions = groupedByDate[date] ?: emptyList()
                        val totalIn = dayTransactions
                            .filter { it.transactionAmount > 0 }
                            .sumOf { it.transactionAmount }
                        Point(index.toFloat(), totalIn.toFloat())
                    }
                    
                    val moneyOutPoints = sortedDates.mapIndexed { index, date ->
                        val dayTransactions = groupedByDate[date] ?: emptyList()
                        val totalOut = dayTransactions
                            .filter { it.transactionAmount < 0 }
                            .sumOf { it.transactionAmount.absoluteValue }
                        Point(index.toFloat(), totalOut.toFloat())
                    }
                    
                    Log.d("CHART_DATA", "Created ${moneyInPoints.size} money in points and ${moneyOutPoints.size} money out points")
                    
                    _uiState.update {
                        it.copy(
                            moneyInPointsData = moneyInPoints,
                            moneyOutPointsData = moneyOutPoints
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("CALCULATE_CHART_DATA_ERROR", e.toString())
            }
        }
    }

    init {
        setInitialDates()
        getUserDetails()
        loadStartupData()
        initialzeApp()
        getUserPreferences()
        checkSubscriptionStatus()
        loadAvailableYears()
        calculatePeriodTotals()
        calculateTransactionTypeBreakdown()
        calculatePeriodTransactions()
        calculateChartData()
        checkHasExistingTransactions()
//        checkAppVersion()

        // When the SMS pipeline inserts a new transaction, navigate to its details screen
        viewModelScope.launch {
            TransactionInsertedEvent.flow.collect { transactionId ->
                _uiState.update { it.copy(navigateToTransactionId = transactionId) }
            }
        }

        // Observe local DB changes and debounce-trigger backup
        viewModelScope.launch(Dispatchers.IO) {
            dataStoreRepository.getLastLocalChange()
                .collect { _ ->
                    val session = dataStoreRepository.getUserSession().firstOrNull()
                    val token = session?.accessToken ?: return@collect
                    val userId = session.userId?.toInt()?.takeIf { it > 0 } ?: return@collect
                    workersRepository.triggerBackupAfterChange(token = token, userId = userId)
                }
        }
    }
    
    /**
     * Get transactions for the selected period (for Top Senders/Receivers)
     */
    private fun calculatePeriodTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val startDate = LocalDate.parse(uiState.value.startDate)
                val endDate = LocalDate.parse(uiState.value.endDate)
                
                dbRepository.getTransactionsBetweenDates(startDate, endDate).collect { transactions ->
                    Log.d("PERIOD_TRANSACTIONS", "Found ${transactions.size} transactions for period")
                    
                    // Convert Transaction to TransactionItem
                    val transactionItems = transactions.map { tx ->
                        TransactionItem(
                            transactionId = tx.id,
                            transactionCode = tx.transactionCode,
                            transactionType = tx.transactionType,
                            transactionAmount = tx.transactionAmount,
                            transactionCost = tx.transactionCost,
                            date = tx.date.toString(),
                            time = tx.time.toString(),
                            sender = tx.sender,
                            recipient = tx.recipient,
                            nickName = tx.nickName,
                            entity = tx.entity,
                            balance = tx.balance,
                            comment = tx.comment,
                            categories = null
                        )
                    }
                    
                    _uiState.update {
                        it.copy(periodTransactions = transactionItems)
                    }
                }
            } catch (e: Exception) {
                Log.e("CALCULATE_PERIOD_TRANSACTIONS_ERROR", e.toString())
            }
        }
    }
}