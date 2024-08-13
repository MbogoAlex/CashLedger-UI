package com.records.pesa.ui.screens.transactions

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.db.DBRepository
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.models.dbModel.UserDetails
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SingleEntityTransactionsScreenUiState(
    val userDetails: UserDetails = UserDetails(),
    val userId: String = "",
    val entity: String = "",
    val transactionType: String = "",
    val times: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val moneyIn: Boolean = true,
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val categoryId: Int? = null,
    val budgetId: Int? = null,
    val transactions: List<TransactionItem> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL
)
class SingleEntityTransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
): ViewModel() {
    private val _uiState = MutableStateFlow(SingleEntityTransactionsScreenUiState())
    val uiState: StateFlow<SingleEntityTransactionsScreenUiState> = _uiState.asStateFlow()

    private val moneyIn: String? = savedStateHandle[SingleEntityTransactionsScreenDestination.moneyIn]

    fun loadStartupData() {
        _uiState.update {
            it.copy(
                userId = savedStateHandle[SingleEntityTransactionsScreenDestination.userId] ?: "",
                entity = savedStateHandle[SingleEntityTransactionsScreenDestination.entity] ?: "",
                transactionType = savedStateHandle[SingleEntityTransactionsScreenDestination.transactionType] ?: "",
                times = savedStateHandle[SingleEntityTransactionsScreenDestination.times] ?: "",
                startDate = savedStateHandle[SingleEntityTransactionsScreenDestination.startDate] ?: "",
                endDate = savedStateHandle[SingleEntityTransactionsScreenDestination.endDate] ?: "",
                moneyIn = moneyIn == "true",
            )
        }
        getUserDetails()
    }

    fun getTransactions() {
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
                    startDate = uiState.value.startDate,
                    endDate = uiState.value.endDate
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
                    Log.e("GET_TRANSACTIONS_ERROR_RESPONSE", response.toString())
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        loadingStatus = LoadingStatus.FAIL
                    )
                }
                Log.e("GET_TRANSACTIONS_EXCEPTION", e.toString())
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
        loadStartupData()
    }

}