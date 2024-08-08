package com.records.pesa.ui.screens.transactions

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionEditPayload
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val transactions: List<TransactionItem> = emptyList(),
    val moneyInTransactions: List<TransactionItem> = emptyList(),
    val moneyOutTransactions: List<TransactionItem> = emptyList(),
    val moneyInSorted: List<SortedTransactionItem> = emptyList(),
    val moneyOutSorted: List<SortedTransactionItem> = emptyList(),
    val nickName: String = "",
    val entity: String = "",
    val transactionType: String = "All types",
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val currentDate: String = "2024-06-15",
    val firstDayOfMonth: String = "2024-06-15",
    val defaultStartDate: String? = null,
    val defaultEndDate: String? = null,
    val startDate: String = "2024-06-15",
    val endDate: String = "2024-06-15",
    val datesSet: Boolean = false,
    val categoryId: Int? = null,
    val budgetId: Int? = null,
    val categoryName: String? = null,
    val budgetName: String? = null,
    val errorCode: Int = 0,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class TransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
): ViewModel() {

    private val _uiState = MutableStateFlow(TransactionsScreenUiState())
    val uiState: StateFlow<TransactionsScreenUiState> = _uiState.asStateFlow()

    private val defaultStartDate: String? = savedStateHandle[TransactionsScreenDestination.startDate]
    private val defaultEndDate: String? = savedStateHandle[TransactionsScreenDestination.endDate]

    private val categoryId: String? = savedStateHandle[TransactionsScreenDestination.categoryId]
    private val budgetId: String? = savedStateHandle[TransactionsScreenDestination.budgetId]

    fun setInitialDates() {
        val currentDate = LocalDate.now()
        val firstDayOfMonth = currentDate.withDayOfMonth(1)
        var startDate = firstDayOfMonth
        var endDate = currentDate
        _uiState.update {
            it.copy(
                currentDate = currentDate.toString(),
                firstDayOfMonth = firstDayOfMonth.toString(),
                startDate = defaultStartDate ?: startDate.toString(),
                endDate = defaultEndDate ?: endDate.toString(),
                categoryId = categoryId?.toInt(),
                budgetId = budgetId?.toInt(),
                categoryName = savedStateHandle[TransactionsScreenDestination.categoryName],
                budgetName = savedStateHandle[TransactionsScreenDestination.budgetName],
                defaultStartDate = defaultStartDate,
                defaultEndDate = defaultEndDate
            )
        }
    }

    fun changeStartDate(startDate: LocalDate, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                startDate = startDate.toString()
            )
        }

        when(tab) {
            TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
            TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
        }
    }

    fun changeEndDate(endDate: LocalDate, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                endDate = endDate.toString()
            )
        }
        when(tab) {
            TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
            TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
        }
    }

    fun changeEntity(entity: String, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                entity = entity
            )
        }
        when(tab) {
            TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
            TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
        }
    }

    fun clearSearch(tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                entity = ""
            )
        }
        when(tab) {
            TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
            TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
        }
    }

    fun changeTransactionType(transactionType: String, tab: TransactionScreenTab) {
        _uiState.update {
            it.copy(
                transactionType = if(transactionType.lowercase() == "buy goods and services") "Buy Goods and Services (till)" else if(transactionType.lowercase() == "withdrawal") "Withdraw Cash" else transactionType
            )
        }
        when(tab) {
            TransactionScreenTab.ALL_TRANSACTIONS -> getTransactions()
            TransactionScreenTab.GROUPED -> getGroupedByEntityTransactions()
        }

    }

    fun updateNickname(name: String) {
        _uiState.update {
            it.copy(
                nickName = name
            )
        }
    }

    fun getTransactions() {
        Log.i("CALLED", "SEARCHING FOR ${uiState.value.entity}")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            transactions = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }



    fun getMoneyInTransactions() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getMoneyIn(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyIn = true,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyInTransactions = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL,
                            errorCode = response.code()
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getMoneyOutTransactions() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getMoneyIn(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyIn = true,
                    latest = true,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyOutTransactions = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getGroupedByEntityTransactions() {
        Log.i("LOADING_TRANSACTIONS", "ID: ${uiState.value.userDetails.userId}")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getGroupedByEntityTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyInSorted = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyIn = response.body()?.data?.transaction?.totalMoneyIn!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun getMoneyOutSortedTransactions() {
        Log.i("LOADING_TRANSACTIONS", "LOADING TRANSACTIONS")
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = apiRepository.getMoneyOutSortedTransactions(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    entity = uiState.value.entity,
                    categoryId = uiState.value.categoryId,
                    budgetId = uiState.value.budgetId,
                    transactionType = if(uiState.value.transactionType.lowercase() != "all types") uiState.value.transactionType else null,
                    moneyIn = false,
                    orderByAmount = true,
                    ascendingOrder = false,
                    startDate = if(uiState.value.defaultStartDate.isNullOrEmpty()) uiState.value.startDate else uiState.value.defaultStartDate!!,
                    endDate = if(uiState.value.defaultEndDate.isNullOrEmpty()) uiState.value.endDate else uiState.value.defaultEndDate!!
                )
                if(response.isSuccessful) {
                    _uiState.update {
                        it.copy(
                            moneyOutSorted = response.body()?.data?.transaction?.transactions!!,
                            totalMoneyOut = response.body()?.data?.transaction?.totalMoneyOut!!,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                    Log.e("GetTransactionsResponseError", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GetTransactionsException", e.toString())
            }
        }
    }

    fun updateTransaction(transactionItem: TransactionItem) {
        var entity = ""
        if(transactionItem.transactionAmount < 0) {
            entity = transactionItem.recipient
        } else if(transactionItem.transactionAmount > 0) {
            entity = transactionItem.sender
        }
        val transactionEditPayload = TransactionEditPayload(
            transactionId = transactionItem.transactionId!!,
            userId = uiState.value.userDetails.userId,
            entity = entity,
            nickName = uiState.value.nickName
        )
        viewModelScope.launch {
            try {
                val response = apiRepository.updateTransaction(
                    token = uiState.value.userDetails.token,
                    transactionEditPayload = transactionEditPayload
                )
                if(response.isSuccessful) {

                } else {}
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
            while(uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getTransactions()
        }
    }

    init {
        setInitialDates()
        getUserDetails()
    }
}