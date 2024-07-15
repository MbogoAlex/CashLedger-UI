package com.records.pesa

import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.viewModelFactory

object AppViewModelFactory {
    val Factory = viewModelFactory {  }
}

fun CreationExtras.cashLedgerApplication(): CashLedger =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CashLedger)