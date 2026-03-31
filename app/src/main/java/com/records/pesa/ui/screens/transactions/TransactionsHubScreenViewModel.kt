package com.records.pesa.ui.screens.transactions

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.UserPreferences
import com.records.pesa.db.models.userPreferences
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import kotlin.math.abs

data class TransactionsHubUiState(
    val userDetails: UserDetails = UserDetails(),
    val preferences: UserPreferences = userPreferences,
    val isPremium: Boolean = false,
    val recentTransactions: List<TransactionItem> = emptyList(),
    val latestTransactions: List<TransactionItem> = emptyList(),
    val topMoneyIn: List<IndividualSortedTransactionItem> = emptyList(),
    val topMoneyOut: List<IndividualSortedTransactionItem> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val totalTransactionCost: Double = 0.0,
    val costByType: List<TransactionTypeSummary> = emptyList(),
    val moneyInCategories: List<TransactionTypeSummary> = emptyList(),
    val moneyOutCategories: List<TransactionTypeSummary> = emptyList(),
    val selectedTimePeriod: TimePeriod = TimePeriod.TODAY,
    val startDate: String = "",
    val endDate: String = "",
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class TransactionsHubScreenViewModel(
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService
) : ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsHubUiState())
    val uiState: StateFlow<TransactionsHubUiState> = _uiState.asStateFlow()

    init {
        applyPeriod(TimePeriod.TODAY)
        loadUser()
        loadPreferences()
    }

    private fun applyPeriod(period: TimePeriod) {
        val (start, end) = period.getDateRange()
        _uiState.update {
            it.copy(
                selectedTimePeriod = period,
                startDate = start.toString(),
                endDate = end.toString()
            )
        }
    }

    fun updateSelectedPeriod(period: TimePeriod) {
        applyPeriod(period)
        fetchAll()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val users = dbRepository.getUsers().first()
            if (users.isNotEmpty()) {
                _uiState.update { it.copy(userDetails = users[0]) }
                fetchAll()
                loadLatestTransactions()
            }
        }
    }

    private fun loadPreferences() {
        viewModelScope.launch {
            dbRepository.getUserPreferences()?.first()?.let { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate?.isAfter(LocalDateTime.now()) == true)
                _uiState.update { it.copy(preferences = prefs, isPremium = isPremium) }
            }
        }
    }

    fun fetchAll() {
        viewModelScope.launch {
            _uiState.update { it.copy(loadingStatus = LoadingStatus.LOADING) }
            val query = transactionService.createUserTransactionQuery(
                userId = uiState.value.userDetails.backUpUserId.toInt(),
                entity = null,
                categoryId = null,
                budgetId = null,
                transactionType = null,
                latest = true,
                moneyDirection = null,
                startDate = LocalDate.parse(uiState.value.startDate),
                endDate = LocalDate.parse(uiState.value.endDate)
            )
            withContext(Dispatchers.IO) {
                try {
                    transactionService.getUserTransactions(query).collect { list ->
                        val items = list.map { it.toTransactionItem() }

                        val totalIn = items.filter { it.transactionAmount > 0 }
                            .sumOf { it.transactionAmount }
                        val totalOut = items.filter { it.transactionAmount < 0 }
                            .sumOf { abs(it.transactionAmount) }

                        val topIn = items
                            .filter { it.transactionAmount > 0 }
                            .groupBy { it.entity }
                            .map { (entity, txs) ->
                                IndividualSortedTransactionItem(
                                    transactionType = txs.first().transactionType,
                                    times = txs.size,
                                    amount = txs.sumOf { it.transactionAmount },
                                    nickName = txs.first().nickName,
                                    name = entity,
                                    transactionCost = txs.sumOf { it.transactionCost }
                                )
                            }
                            .sortedByDescending { it.amount }
                            .take(5)

                        val topOut = items
                            .filter { it.transactionAmount < 0 }
                            .groupBy { it.entity }
                            .map { (entity, txs) ->
                                IndividualSortedTransactionItem(
                                    transactionType = txs.first().transactionType,
                                    times = txs.size,
                                    amount = txs.sumOf { abs(it.transactionAmount) },
                                    nickName = txs.first().nickName,
                                    name = entity,
                                    transactionCost = txs.sumOf { abs(it.transactionCost) }
                                )
                            }
                            .sortedByDescending { it.amount }
                            .take(5)

                        val moneyInCats = items
                            .filter { it.transactionAmount > 0 }
                            .groupBy { it.transactionType }
                            .map { (type, txs) ->
                                TransactionTypeSummary(
                                    transactionType = type,
                                    totalAmount = txs.sumOf { it.transactionAmount },
                                    transactionCount = txs.size,
                                    percentageOfTotal = 0f,
                                    icon = null
                                )
                            }
                            .sortedByDescending { it.totalAmount }

                        val moneyOutCats = items
                            .filter { it.transactionAmount < 0 }
                            .groupBy { it.transactionType }
                            .map { (type, txs) ->
                                TransactionTypeSummary(
                                    transactionType = type,
                                    totalAmount = txs.sumOf { abs(it.transactionAmount) },
                                    transactionCount = txs.size,
                                    percentageOfTotal = 0f,
                                    icon = null
                                )
                            }
                            .sortedByDescending { it.totalAmount }

                        val totalCost = items.sumOf { abs(it.transactionCost) }
                        val costByType = items
                            .filter { abs(it.transactionCost) > 0 }
                            .groupBy { it.transactionType }
                            .map { (type, txs) ->
                                TransactionTypeSummary(
                                    transactionType = type,
                                    totalAmount = txs.sumOf { abs(it.transactionCost) },
                                    transactionCount = txs.size,
                                    percentageOfTotal = if (totalCost > 0) (txs.sumOf { abs(it.transactionCost) } / totalCost * 100).toFloat() else 0f,
                                    icon = null
                                )
                            }
                            .sortedByDescending { it.totalAmount }
                            .take(5)

                        _uiState.update {
                            it.copy(
                                recentTransactions = items.take(5),
                                totalMoneyIn = totalIn,
                                totalMoneyOut = totalOut,
                                totalTransactionCost = totalCost,
                                costByType = costByType,
                                topMoneyIn = topIn,
                                topMoneyOut = topOut,
                                moneyInCategories = moneyInCats,
                                moneyOutCategories = moneyOutCats,
                                loadingStatus = LoadingStatus.SUCCESS
                            )
                        }
                    }
                } catch (e: Exception) {
                    Log.e("TransactionsHubVM", "fetchAll: $e")
                    _uiState.update { it.copy(loadingStatus = LoadingStatus.FAIL) }
                }
            }
        }
    }

    private fun loadLatestTransactions() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val query = transactionService.createUserTransactionQuery(
                    userId = uiState.value.userDetails.backUpUserId.toInt(),
                    entity = null,
                    categoryId = null,
                    budgetId = null,
                    transactionType = null,
                    latest = true,
                    moneyDirection = null,
                    startDate = LocalDate.now().minusYears(10),
                    endDate = LocalDate.now()
                )
                transactionService.getUserTransactions(query).collect { list ->
                    _uiState.update {
                        it.copy(latestTransactions = list.map { it.toTransactionItem() }.take(5))
                    }
                }
            } catch (e: Exception) {
                Log.e("TransactionsHubVM", "loadLatestTransactions: $e")
            }
        }
    }
}

