package com.records.pesa.ui.screens.transactions

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.records.pesa.models.TransactionItem
import com.records.pesa.network.ApiRepository
import com.records.pesa.reusables.LoadingStatus
import kotlinx.coroutines.launch

data class TransactionsScreenUiState(
    val transactions: List<TransactionItem> = emptyList(),
    val totalMoneyIn: Double = 0.0,
    val totalMoneyOut: Double = 0.0,
    val loadingStatus: LoadingStatus = LoadingStatus.INITIAL
)
class TransactionsScreenViewModelScreen(
    private val apiRepository: ApiRepository
): ViewModel() {
    fun getTransactions() {
        viewModelScope.launch {
            try {
                val response = apiRepository.getTransactions(
                    userId = 1,
                    entity = null,
                    categoryId = null,
                    transactionType = null,
                    latest = false,
                    startDate = "2023-03-06",
                    endDate = "2024-06-25"
                )

            } catch (e: Exception) {

            }
        }
    }

    init {
        getTransactions()
    }
}