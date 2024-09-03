package com.records.pesa

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.records.pesa.db.DBRepository
import com.records.pesa.network.ApiRepository
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.ui.screens.DashboardScreenViewModel
import com.records.pesa.ui.screens.SplashScreenViewModel
import com.records.pesa.ui.screens.auth.LoginScreenViewModel
import com.records.pesa.ui.screens.auth.RegistrationScreenViewModel
import com.records.pesa.ui.screens.auth.UpdatePasswordScreenViewModel
import com.records.pesa.ui.screens.dashboard.HomeScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenViewModel
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenViewModel
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenViewModel
import com.records.pesa.ui.screens.dashboard.chart.ChartHomeScreenViewModel
import com.records.pesa.ui.screens.dashboard.chart.CombinedChartScreenViewModel
import com.records.pesa.ui.screens.dashboard.sms.SmsFetchScreenViewModel
import com.records.pesa.ui.screens.payment.SubscriptionScreenViewModel
import com.records.pesa.ui.screens.profile.AccountInformationScreenViewModel
import com.records.pesa.ui.screens.transactionTypes.TransactionTypesScreenViewModel
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenViewModel
import com.records.pesa.ui.screens.transactions.TransactionDetailsScreenViewModel
import com.records.pesa.ui.screens.transactions.TransactionsScreenViewModel
import com.records.pesa.ui.screens.transactions.sorted.SortedTransactionsScreenViewModel
import com.records.pesa.workers.WorkersRepository

object AppViewModelFactory {
    val Factory = viewModelFactory {
        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            val userService: UserAccountService = cashLedgerApplication().container.userAccountService
            TransactionsScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = this.createSavedStateHandle(),
                dbRepository = cashLedgerApplication().container.dbRepository,
                transactionService = transactionService,
                userAccountService = userService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            SingleEntityTransactionsScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository,
                transactionService = transactionService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            DashboardScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            val categoryService: CategoryService = cashLedgerApplication().container.categoryService
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            CategoryDetailsScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository,
                categoryService = categoryService,
                transactionService = transactionService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            val categoryService: CategoryService = cashLedgerApplication().container.categoryService
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            MembersAdditionScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository,
                categoryService = categoryService,
                transactionService = transactionService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            val categoryService: CategoryService = cashLedgerApplication().container.categoryService
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            val userAccountService: UserAccountService = cashLedgerApplication().container.userAccountService
            CategoriesScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = cashLedgerApplication().container.dbRepository,
                categoryService = categoryService,
                transactionService = transactionService,
                userAccountService = userAccountService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            val categoryService: CategoryService = cashLedgerApplication().container.categoryService
            CategoryAdditionScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = cashLedgerApplication().container.dbRepository,
                categoryService = categoryService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetListScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetInfoScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val savedStateHandle: SavedStateHandle = this.createSavedStateHandle()
            BudgetCreationScreenViewModel(
                apiRepository = apiRepository,
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository
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
                savedStateHandle = savedStateHandle,
                dbRepository = cashLedgerApplication().container.dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val categoryService: CategoryService = cashLedgerApplication().container.categoryService
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            val userService: UserAccountService = cashLedgerApplication().container.userAccountService
            SmsFetchScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                categoryService = categoryService,
                transactionsService = transactionService,
                userAccountService = userService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val userService: UserAccountService = cashLedgerApplication().container.userAccountService
            SplashScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                userAccountService = userService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            RegistrationScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val workersRepository: WorkersRepository = cashLedgerApplication().container.workersRepository
            val userAccountService: UserAccountService = cashLedgerApplication().container.userAccountService
            LoginScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                savedStateHandle = this.createSavedStateHandle(),
                workersRepository = workersRepository,
                userAccountService = userAccountService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            AccountInformationScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            UpdatePasswordScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            SubscriptionScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            HomeScreenViewModel(
                dbRepository = dbRepository,
                apiRepository = apiRepository
            )
        }

        initializer {
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            MainActivityViewModel(
                dbRepository = dbRepository
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            TransactionDetailsScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                savedStateHandle = this.createSavedStateHandle(),
                transactionService = transactionService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            TransactionTypesScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                transactionService = transactionService
            )
        }

        initializer {
            val apiRepository: ApiRepository = cashLedgerApplication().container.apiRepository
            val dbRepository: DBRepository = cashLedgerApplication().container.dbRepository
            val transactionService: TransactionService = cashLedgerApplication().container.transactionService
            SortedTransactionsScreenViewModel(
                apiRepository = apiRepository,
                dbRepository = dbRepository,
                transactionService = transactionService
            )
        }
    }
}

fun CreationExtras.cashLedgerApplication(): CashLedger =
    (this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as CashLedger)