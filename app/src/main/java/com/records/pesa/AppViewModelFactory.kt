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
import com.records.pesa.ui.screens.DashboardScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenViewModel
import com.records.pesa.ui.screens.dashboard.chart.ChartHomeScreenViewModel
import com.records.pesa.ui.screens.dashboard.chart.CombinedChartScreenViewModel
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenViewModel
import com.records.pesa.ui.screens.transactions.TransactionsScreenViewModelScreen

object AppViewModelFactory {
    @RequiresApi(Build.VERSION_CODES.O)
    val Factory = viewModelFactory {
        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            TransactionsScreenViewModelScreen(
                apiRepository = apiRepository,
                savedStateHandle = this.createSavedStateHandle()
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

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            DashboardScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            CategoryDetailsScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            MembersAdditionScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            CategoriesScreenViewModel(
                apiRepository = apiRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            CategoryAdditionScreenViewModel(
                apiRepository = apiRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetListScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetInfoScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetCreationScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            ChartHomeScreenViewModel(
                savedStateHandle = savedStateHandle
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            CombinedChartScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle
            )
        }
    }
}

fun CreationExtras.cashLedgerApplication(): CashLedger =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CashLedger)