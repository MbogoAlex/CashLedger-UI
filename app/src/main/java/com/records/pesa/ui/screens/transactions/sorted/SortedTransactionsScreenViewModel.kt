package com.records.pesa.ui.screens.transactions.sorted

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.mapper.toIndividualSortedTransactionItem
import com.records.pesa.mapper.toTransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.service.transaction.TransactionService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import kotlin.math.abs

data class SortedTransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val moneyInTransactions: List<IndividualSortedTransactionItem> = emptyList(),
    val moneyOutTransactions: List<IndividualSortedTransactionItem> = emptyList(),
    val startDate: String = "",
    val endDate: String = "",
    val totalIn: Double = 0.0,
    val totalOut: Double = 0.0,
    val entity: String = "",
    val orderByAmount: Boolean = true,
    val moneyIn: Boolean = true,
    val currentTab: SortedTransactionsTab = SortedTransactionsTab.MONEY_IN,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class SortedTransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository,
    private val transactionService: TransactionService
): ViewModel() {
    private val _uiState = MutableStateFlow(SortedTransactionsScreenUiState())
    val uiState: StateFlow<SortedTransactionsScreenUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null

    fun initializeDate() {
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

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
        }
    }

    fun changeStartDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = date.toString()
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
    }

    fun changeEndDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                endDate = date.toString(),
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
    }

    fun changeEntity(entity: String) {
        _uiState.update {
            it.copy(
                entity = entity
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
    }

    fun changeTab(tab: SortedTransactionsTab) {
        _uiState.update {
            it.copy(
                currentTab = tab
            )
        }
//        getTransactions()
    }

    fun changeOrderBy(orderByAmount: Boolean) {
        _uiState.update {
            it.copy(
                orderByAmount = orderByAmount
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
    }

    fun changeMoneyDirection(moneyIn: Boolean) {
        _uiState.update {
            it.copy(
                moneyIn = moneyIn
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactions()
        }
    }


    fun getTransactions(){
        Log.d("getTransactions", "getTransactions")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            val query = transactionService.createUserTransactionQuery(
                userId = uiState.value.userDetails.userId,
                entity = uiState.value.entity,
                categoryId = null,
                budgetId = null,
                transactionType = null,
                moneyDirection = if(uiState.value.moneyIn) "in" else "out",
                startDate = LocalDate.parse(uiState.value.startDate),
                endDate = LocalDate.parse(uiState.value.endDate),
                latest = true
            )
            withContext(Dispatchers.IO) {

                try {
                    transactionService.getUserTransactions(query).collect() {transactions ->
                        transformTransactions(transactions.map { it.toTransactionItem() }, uiState.value.orderByAmount)
                    }

                } catch (e: Exception) {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("getSortedTransactionsErrorException", e.toString())
                }
            }
//            try {
//              val response = when(uiState.value.currentTab) {
//                  SortedTransactionsTab.MONEY_IN -> {
//                      apiRepository.getMoneyInSortedTransactions(
//                        token = uiState.value.userDetails.token,
//                          userId = uiState.value.userDetails.userId,
//                          entity = uiState.value.entity,
//                          categoryId = null,
//                          budgetId = null,
//                          transactionType = null,
//                          moneyIn = true,
//                          orderByAmount = uiState.value.orderByAmount,
//                          ascendingOrder = false,
//                          startDate = uiState.value.startDate,
//                          endDate = uiState.value.endDate
//                      )
//                  }
//                  SortedTransactionsTab.MONEY_OUT -> {
//                      apiRepository.getMoneyOutSortedTransactions(
//                          token = uiState.value.userDetails.token,
//                          userId = uiState.value.userDetails.userId,
//                          entity = uiState.value.entity,
//                          categoryId = null,
//                          budgetId = null,
//                          transactionType = null,
//                          moneyIn = false,
//                          orderByAmount = uiState.value.orderByAmount,
//                          ascendingOrder = false,
//                          startDate = uiState.value.startDate,
//                          endDate = uiState.value.endDate
//                      )
//                  }
//              }
//                if(response.isSuccessful) {
//                    _uiState.update {
//                        it.copy(
//                            moneyInTransactions = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_IN) response.body()?.data?.transaction?.transactions!! else emptyList(),
//                            moneyOutTransactions = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_OUT) response.body()?.data?.transaction?.transactions!! else emptyList(),
//                            totalIn = response.body()?.data?.transaction?.totalMoneyIn!!,
//                            totalOut = response.body()?.data?.transaction?.totalMoneyOut!!,
//                            loadingStatus = LoadingStatus.SUCCESS
//                        )
//                    }
//                } else {
//                    Log.e("getSortedTransactionsErrorResponse", response.toString())
//                    _uiState.update {
//                        it.copy(
//                            loadingStatus = LoadingStatus.FAIL
//                        )
//                    }
//                }
//            } catch (e: Exception) {
//                Log.e("getSortedTransactionsErrorException", e.toString())
//                _uiState.update {
//                    it.copy(
//                        loadingStatus = LoadingStatus.FAIL
//                    )
//                }
//            }
        }
    }

    fun transformTransactions(transactions: List<TransactionItem>, sortedByAmount: Boolean) {
        // Group transactions by entity
        val groupedTransactions = transactions.groupBy { it.entity }

        // Initialize total amount accumulator
        var totalAbsAmount = 0.0

        // Transform the grouped transactions
        val sortedTransactionItems = groupedTransactions.map { (entity, transactionItems) ->
            // Calculate the total amount, total transaction cost, and number of transactions
            val totalAmount = transactionItems.sumOf { it.transactionAmount }
            val totalTransactionCost = transactionItems.sumOf { it.transactionCost }
            val times = transactionItems.size
            val nickName = transactionItems.firstOrNull()?.nickName

            // Accumulate the absolute value of the total amount
            totalAbsAmount += kotlin.math.abs(totalAmount)

            // Create the IndividualSortedTransactionItem object
            IndividualSortedTransactionItem(
                transactionType = transactionItems.first().transactionType,
                times = times,
                amount = totalAmount,
                nickName = nickName,
                name = entity,
                transactionCost = totalTransactionCost
            )
        }

        val sortedTransactions = if(sortedByAmount) sortedTransactionItems.sortedBy {
            abs(it.amount)
        } else sortedTransactionItems.sortedBy {
            it.times
        }

        _uiState.update {
            it.copy(
                moneyInTransactions = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_IN) sortedTransactions.reversed() else emptyList(),
                moneyOutTransactions = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_OUT) sortedTransactions.reversed() else emptyList(),
                totalIn = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_IN) totalAbsAmount else 0.0,
                totalOut = if(uiState.value.currentTab == SortedTransactionsTab.MONEY_OUT) totalAbsAmount else 0.0,
                loadingStatus = LoadingStatus.SUCCESS
            )
        }

    }


    init {
        getUserDetails()
        initializeDate()
        viewModelScope.launch {
            while(uiState.value.userDetails.userId == 0){
                delay(1000L)
            }
            getTransactions()
        }
    }
}