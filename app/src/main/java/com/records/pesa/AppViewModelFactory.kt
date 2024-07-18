package com.records.pesa

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.records.pesa.network.ApiRepository
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenViewModel
import com.records.pesa.ui.screens.transactions.TransactionsScreenViewModelScreen

object AppViewModelFactory {
    @RequiresApi(Build.VERSION_CODES.O)
    val Factory = viewModelFactory {
        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            TransactionsScreenViewModelScreen(
                apiRepository = apiRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            SingleEntityTransactionsScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }
    }
}

fun CreationExtras.cashLedgerApplication(): CashLedger =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CashLedger)