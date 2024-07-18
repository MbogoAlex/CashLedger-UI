package com.records.pesa.ui.screens.transactions

import android.util.Log
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class SingleEntityTransactionsScreenUiState(
    val userId: String = "",
    val entity: String = "",
    val transactionType: String = "",
    val times: String = "",
    val startDate: String = "",
    val endDate: String = "",
    val moneyIn: Boolean = true,
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val transactions: List<TransactionItem> = emptyList(),
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class SingleEntityTransactionsScreenViewModel(
    private val apiRepository: ApiRepository,
    private val savedStateHandle: SavedStateHandle
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
        getTransactions()
    }

    fun getTransactions() {
        _uiState.update {
            it.copy(
                loadingStatus = LoadingStatus.LOADING
            )
        }
        viewModelScope.launch {
            try {
                val response = if (uiState.value.moneyIn) apiRepository.getMoneyIn(
                    userId = 1,
                    entity = _uiState.value.entity,
                    categoryId = null,
                    transactionType = _uiState.value.transactionType,
                    moneyIn = uiState.value.moneyIn,
                    latest = true,
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate
                ) else apiRepository.getMoneyOut(
                    userId = 1,
                    entity = _uiState.value.entity,
                    categoryId = null,
                    transactionType = _uiState.value.transactionType,
                    moneyIn = uiState.value.moneyIn,
                    latest = true,
                    startDate = _uiState.value.startDate,
                    endDate = _uiState.value.endDate
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

    init {
        loadStartupData()
    }

}