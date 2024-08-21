package com.records.pesa.ui.screens.transactionTypes

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

data class TransactionTypesScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val allMoneyIn: Double = 0.0,
    val allMoneyOut: Double = 0.0,
    val startDate: String = "",
    val endDate: String = "",
    val fromReversal: Double = 0.0,
    val toReversal: Double = 0.0,
    val deposit: Double = 0.0,
    val fromMshwari: Double = 0.0,
    val toMshwari: Double = 0.0,
    val fromSendMoney: Double = 0.0,
    val toSendMoney: Double = 0.0,
    val till: Double = 0.0,
    val pochi: Double = 0.0,
    val withdrawal: Double = 0.0,
    val airtime: Double = 0.0,
    val fromHustler: Double = 0.0,
    val toHustler: Double = 0.0,
    val fuliza: Double = 0.0,
    val payBill: Double = 0.0,
    val toKcbMpesa: Double = 0.0,
    val fromKcbMpesa: Double = 0.0,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)

class TransactionTypesScreenViewModel(
    private val apiRepository: ApiRepository,
    private val dbRepository: DBRepository
): ViewModel() {
    private val _uiState = MutableStateFlow(TransactionTypesScreenUiState())
    val uiState: StateFlow<TransactionTypesScreenUiState> = _uiState.asStateFlow()

    private var filterJob: Job? = null

    fun getUserDetails() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    userDetails = dbRepository.getUsers().first()[0]
                )
            }
        }
    }

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

    fun updateStartDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                startDate = date.toString()
            )
        }
        filterJob?.cancel()
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactionTypes()
        }
    }

    fun updateEndDate(date: LocalDate) {
        _uiState.update {
            it.copy(
                endDate = date.toString()
            )
        }
        filterJob = viewModelScope.launch {
            delay(500L)
            getTransactionTypes()
        }
    }

    fun getTransactionTypes() {
        viewModelScope.launch {
            try {
                var allMoneyIn = 0.0
                var allMoneyOut = 0.0
                val response = apiRepository.getTransactionTypesDashboard(
                    token = uiState.value.userDetails.token,
                    userId = uiState.value.userDetails.userId,
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate
                )
                if(response.isSuccessful) {
                    val transactions = response.body()?.data?.transaction?.transactions!!
                    // Initialize a map to track found transaction types
                    val foundTransactionTypes = mutableSetOf<String>()

                    for(transaction in transactions){
                        if(transaction.amountSign == "Positive") {
                            allMoneyIn += transaction.amount
                        } else if(transaction.amountSign == "Negative") {
                            allMoneyOut += transaction.amount
                        }

                        when(transaction.transactionType) {
                            "Reversal" -> {
                                foundTransactionTypes.add("Reversal")
                                if(transaction.amountSign == "Positive") {
                                    _uiState.update {
                                        it.copy(
                                            fromReversal = transaction.amount
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            toReversal = transaction.amount
                                        )
                                    }
                                }
                            }
                            "Deposit" -> {
                                foundTransactionTypes.add("Deposit")
                                _uiState.update {
                                    it.copy(
                                        deposit = transaction.amount
                                    )
                                }
                            }
                            "Mshwari" -> {
                                foundTransactionTypes.add("Mshwari")
                                if(transaction.amountSign == "Positive") {
                                    _uiState.update {
                                        it.copy(
                                            fromMshwari = transaction.amount
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            toMshwari = transaction.amount
                                        )
                                    }
                                }
                            }
                            "Send Money" -> {
                                foundTransactionTypes.add("Send Money")
                                if(transaction.amountSign == "Positive") {
                                    _uiState.update {
                                        it.copy(
                                            fromSendMoney = transaction.amount
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            toSendMoney = transaction.amount
                                        )
                                    }
                                }
                            }
                            "Buy Goods and Services (till)" -> {
                                foundTransactionTypes.add("Buy Goods and Services (till)")
                                _uiState.update {
                                    it.copy(
                                        till = transaction.amount
                                    )
                                }
                            }
                            "Pochi la Biashara" -> {
                                foundTransactionTypes.add("Pochi la Biashara")
                                _uiState.update {
                                    it.copy(
                                        pochi = transaction.amount
                                    )
                                }
                            }
                            "Withdraw Cash" -> {
                                foundTransactionTypes.add("Withdraw Cash")
                                _uiState.update {
                                    it.copy(
                                        withdrawal = transaction.amount
                                    )
                                }
                            }
                            "Airtime & Bundles" -> {
                                foundTransactionTypes.add("Airtime & Bundles")
                                _uiState.update {
                                    it.copy(
                                        airtime = transaction.amount
                                    )
                                }
                            }
                            "Hustler Fund" -> {
                                foundTransactionTypes.add("Hustler Fund")
                                if(transaction.amountSign == "Positive") {
                                    _uiState.update {
                                        it.copy(
                                            fromHustler = transaction.amount
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            toHustler = transaction.amount
                                        )
                                    }
                                }
                            }
                            "Fuliza" -> {
                                foundTransactionTypes.add("Fuliza")
                                _uiState.update {
                                    it.copy(
                                        fuliza = transaction.amount
                                    )
                                }
                            }
                            "Pay Bill" -> {
                                foundTransactionTypes.add("Pay Bill")
                                _uiState.update {
                                    it.copy(
                                        payBill = transaction.amount
                                    )
                                }
                            }
                            "KCB Mpesa account" -> {
                                foundTransactionTypes.add("KCB Mpesa account")
                                if(transaction.amountSign == "Positive") {
                                    _uiState.update {
                                        it.copy(
                                            fromKcbMpesa = transaction.amount
                                        )
                                    }
                                } else {
                                    _uiState.update {
                                        it.copy(
                                            toKcbMpesa = transaction.amount
                                        )
                                    }
                                }
                            }
                        }
                    }

// After processing transactions, reset the values for any transaction type not found
                    if (!foundTransactionTypes.contains("Reversal")) {
                        _uiState.update { it.copy(fromReversal = 0.0, toReversal = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Deposit")) {
                        _uiState.update { it.copy(deposit = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Mshwari")) {
                        _uiState.update { it.copy(fromMshwari = 0.0, toMshwari = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Send Money")) {
                        _uiState.update { it.copy(fromSendMoney = 0.0, toSendMoney = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Buy Goods and Services (till)")) {
                        _uiState.update { it.copy(till = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Pochi la Biashara")) {
                        _uiState.update { it.copy(pochi = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Withdraw Cash")) {
                        _uiState.update { it.copy(withdrawal = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Airtime & Bundles")) {
                        _uiState.update { it.copy(airtime = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Hustler Fund")) {
                        _uiState.update { it.copy(fromHustler = 0.0, toHustler = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Fuliza")) {
                        _uiState.update { it.copy(fuliza = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("Pay Bill")) {
                        _uiState.update { it.copy(payBill = 0.0) }
                    }
                    if (!foundTransactionTypes.contains("KCB Mpesa account")) {
                        _uiState.update { it.copy(fromKcbMpesa = 0.0, toKcbMpesa = 0.0) }
                    }

                    _uiState.update {
                        it.copy(
                            allMoneyIn = allMoneyIn,
                            allMoneyOut = allMoneyOut,
                            loadingStatus = LoadingStatus.SUCCESS
                        )
                    }
                } else {
                    Log.e("GetTransactionsResponseError", response.toString())
                    _uiState.update {
                        it.copy(
                            loadingStatus = LoadingStatus.FAIL
                        )
                    }
                }

            } catch (e: Exception) {
                Log.e("GetTransactionsException", e.toString())
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
            }
        }
    }
    init {
        getUserDetails()
        initializeDate()
        viewModelScope.launch {
            while (uiState.value.userDetails.userId == 0) {
                delay(1000)
            }
            getTransactionTypes()
        }
    }
}