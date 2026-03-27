package com.records.pesa.nav

import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.DashboardScreenDestination
import com.records.pesa.ui.screens.SplashScreenComposable
import com.records.pesa.ui.screens.SplashScreenDestination
import com.records.pesa.ui.screens.auth.LoginScreenComposable
import com.records.pesa.ui.screens.auth.LoginScreenDestination
import com.records.pesa.ui.screens.auth.RegistrationScreenComposable
import com.records.pesa.ui.screens.auth.RegistrationScreenDestination
import com.records.pesa.ui.screens.auth.UpdatePasswordScreenComposable
import com.records.pesa.ui.screens.auth.UpdatePasswordScreenDestination
import com.records.pesa.ui.screens.backup.BackupRestoreScreenComposable
import com.records.pesa.ui.screens.backup.BackupRestoreScreenDestination
import com.records.pesa.ui.screens.backup.BackupScreenComposable
import com.records.pesa.ui.screens.backup.BackupScreenDestination
import com.records.pesa.ui.screens.dashboard.HomeScreenComposable
import com.records.pesa.ui.screens.dashboard.HomeScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetAllTransactionsScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetAllTransactionsScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetAuditTrailScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetAuditTrailScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetCycleHistoryScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetCycleHistoryScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoryAllTransactionsScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryAllTransactionsScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenDestination
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenComposable
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenDestination
import com.records.pesa.ui.screens.dashboard.sms.SMSFetchScreenDestination
import com.records.pesa.ui.screens.dashboard.sms.SmsFetchScreenComposable
import com.records.pesa.ui.screens.payment.SubscriptionScreenComposable
import com.records.pesa.ui.screens.payment.SubscriptionScreenDestination
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenComposable
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenDestination
import com.records.pesa.ui.screens.transactions.TransactionDetailsScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionDetailsScreenDestination
import com.records.pesa.ui.screens.transactions.TransactionsScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionsScreenDestination

@Composable
fun NavigationGraph(
    navController: NavHostController,
    onSwitchTheme: () -> Unit,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = SplashScreenDestination.route,
    ) {
        composable(SplashScreenDestination.route) {
            SplashScreenComposable(
                navigateToSmsFetchScreen = { navController.navigate(SMSFetchScreenDestination.route) },
                navigateToRegistrationScreen = {
                    navController.navigate(RegistrationScreenDestination.route)
                },
                navigateToLoginScreenWithArgs = {phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginScreenDestination.route)
                }
            )
        }
        composable(RegistrationScreenDestination.route) {
            RegistrationScreenComposable(
                navigateToLoginScreenWithArgs = {phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginScreenDestination.route)
                }
            )
        }
        composable(LoginScreenDestination.route) {
            LoginScreenComposable(
                navigateToRegistrationScreen = {
                    navController.navigate(RegistrationScreenDestination.route)
                },
                navigateToSmsFetchScreenWithArgs = {
                    navController.navigate("${SMSFetchScreenDestination.route}/${it}")
                },
                navigateToUpdatePasswordScreen = {
                    navController.navigate(UpdatePasswordScreenDestination.route)
                },
                navigateToSmsFetchScreen = {
                    navController.navigate(SMSFetchScreenDestination.route)
                },
                navigateToBackupRestoreScreen = {
                    navController.navigate(BackupRestoreScreenDestination.route)
                }
            )
        }
        composable(
            LoginScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(LoginScreenDestination.phoneNumber) {
                    type = NavType.StringType
                },
                navArgument(LoginScreenDestination.password) {
                    type = NavType.StringType
                }
            )
        ) {
            LoginScreenComposable(
                navigateToRegistrationScreen = {
                    navController.navigate(RegistrationScreenDestination.route)
                },
                navigateToSmsFetchScreenWithArgs = {
                    navController.navigate("${SMSFetchScreenDestination.route}/${it}")
                },
                navigateToUpdatePasswordScreen = {
                    navController.navigate(UpdatePasswordScreenDestination.route)
                },
                navigateToSmsFetchScreen = {
                    navController.navigate(SMSFetchScreenDestination.route)
                },
                navigateToBackupRestoreScreen = {
                    navController.navigate(BackupRestoreScreenDestination.route)
                }
            )
        }
        composable(UpdatePasswordScreenDestination.route) {
            UpdatePasswordScreenComposable(
                navigateToLoginScreenWithArgs = {phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginScreenDestination.route)
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(SMSFetchScreenDestination.route) {
            SmsFetchScreenComposable(
                navigateToHomeScreen = { navController.navigate(HomeScreenDestination.route) },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.popBackStack(SMSFetchScreenDestination.route, true)
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToBackupRestoreScreen = {
                    navController.navigate(BackupRestoreScreenDestination.route)
                }
            )
        }

        composable(
            SMSFetchScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(SMSFetchScreenDestination.fromLogin) {
                    type = NavType.StringType
                }
            )
        ) {
            SmsFetchScreenComposable(
                navigateToHomeScreen = { navController.navigate(HomeScreenDestination.route) },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.popBackStack(SMSFetchScreenDestination.route, true)
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToBackupRestoreScreen = {
                    navController.navigate(BackupRestoreScreenDestination.route)
                }
            )
        }

        composable(BackupRestoreScreenDestination.route) {
            BackupRestoreScreenComposable(
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToHomeScreenWithArgs = {
                    navController.navigate("${HomeScreenDestination.route}/${it}")
                },
                navigateToSmsFetchScreen = {
                    navController.navigate(SMSFetchScreenDestination.route)
                }
            )
        }

        composable(
            HomeScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(HomeScreenDestination.screen) {
                    type = NavType.StringType
                }
            )
        ) {
            HomeScreenComposable(
                navigateToTransactionsScreen = {
                    navController.navigate(TransactionsScreenDestination.route)
                },
                navigateToCategoryDetailsScreen = {
                    navController.navigate("${CategoryDetailsScreenDestination.route}/${it}")
                },
                navigateToCategoryAdditionScreen = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                },
                navigateToCategoriesScreen = {
                    navController.navigate(CategoriesScreenDestination.route)
                },
                navigateToBudgetInfoScreen = {
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                navigateToBudgetCreationScreen = {
                    navController.navigate(BudgetCreationScreenDestination.route)
                },
                navigateToBudgetCreationScreenWithCategoryId = {
                    navController.navigate("${BudgetCreationScreenDestination.route}/${it}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToLoginScreenWithArgs = {phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                onSwitchTheme = onSwitchTheme,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToTransactionsScreenWithTransactionType = {comment, transactionType, moneyDirection, startDate, endDate ->
                    navController.navigate("${TransactionsScreenDestination.route}/${comment}/${transactionType}/${moneyDirection}/${startDate}/${endDate}")
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginScreenDestination.route)
                },
                navigateToUpdatePasswordScreen = {
                    navController.navigate(UpdatePasswordScreenDestination.route)
                }
            )
        }

        composable(BackupScreenDestination.route) {
            BackupScreenComposable(
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                }
            )
        }

        composable(HomeScreenDestination.route) {
            HomeScreenComposable(
                navigateToTransactionsScreen = {
                    navController.navigate(TransactionsScreenDestination.route)
                },
                navigateToCategoryDetailsScreen = {
                    navController.navigate("${CategoryDetailsScreenDestination.route}/${it}")
                },
                navigateToCategoryAdditionScreen = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                },
                navigateToCategoriesScreen = {
                    navController.navigate(CategoriesScreenDestination.route)
                },
                navigateToBudgetInfoScreen = {
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                navigateToBudgetCreationScreen = {
                    navController.navigate(BudgetCreationScreenDestination.route)
                },
                navigateToBudgetCreationScreenWithCategoryId = {
                    navController.navigate("${BudgetCreationScreenDestination.route}/${it}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToLoginScreenWithArgs = {phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                },
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                onSwitchTheme = onSwitchTheme,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToTransactionsScreenWithTransactionType = {comment, transactionType, moneyDirection, startDate, endDate ->
                    navController.navigate("${TransactionsScreenDestination.route}/${comment}/${transactionType}/${moneyDirection}/${startDate}/${endDate}")
                },
                navigateToLoginScreen = {
                    navController.navigate(LoginScreenDestination.route)
                },
                navigateToUpdatePasswordScreen = {
                    navController.navigate(UpdatePasswordScreenDestination.route)
                }
            )
        }
        composable(TransactionsScreenDestination.route) {
            Log.d("NAV-TYPE", TransactionsScreenDestination.route)
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                showBackArrow = true,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                }
            )
        }

        composable(
            TransactionsScreenDestination.routeWithTransactionType,
            arguments = listOf(
                navArgument(TransactionsScreenDestination.transactionType) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.moneyDirection) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.startDate) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.endDate) {
                    type = NavType.StringType
                },
            )
        ) {
            Log.d("NAV-TYPE", TransactionsScreenDestination.routeWithTransactionType)
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                showBackArrow = true,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                }
            )
        }

        composable(
            SingleEntityTransactionsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(SingleEntityTransactionsScreenDestination.userId) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.transactionType) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.entity) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.startDate) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.endDate) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.times) {
                    type = NavType.StringType
                },
                navArgument(SingleEntityTransactionsScreenDestination.moneyDirection) {
                    type = NavType.StringType
                },
            )
        ) {
            SingleEntityTransactionsScreenComposable(
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                }
            )
        }
        composable(
            CategoryDetailsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CategoryDetailsScreenDestination.categoryId) {
                    type = NavType.StringType
                },
            )
        ) {
            CategoryDetailsScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToMembersAdditionScreen = {
                    navController.navigate("${MembersAdditionScreenDestination.route}/${it}")
                },
                navigateToTransactionsScreen = {
                    navController.navigate("${TransactionsScreenDestination.route}/${it}")
                },
                navigateToAllTransactionsScreen = { catId, startDate, endDate ->
                    navController.navigate("${CategoryAllTransactionsScreenDestination.route}/$catId/$startDate/$endDate")
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToBudgetCreationScreen = {
                    navController.navigate("${BudgetCreationScreenDestination.route}/${it}")
                },
                navigateToBudgetInfoScreen = {
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                navigateToCategoryBudgetListScreen = {categoryId, categoryName ->
                    navController.navigate("${BudgetListScreenDestination.route}/${categoryId}/${categoryName}")
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                navigateToTransactionDetails = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/$it")
                }
            )
        }
        composable(
            CategoryAllTransactionsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CategoryAllTransactionsScreenDestination.categoryId) {
                    type = NavType.StringType
                },
                navArgument(CategoryAllTransactionsScreenDestination.startDate) {
                    type = NavType.StringType
                },
                navArgument(CategoryAllTransactionsScreenDestination.endDate) {
                    type = NavType.StringType
                }
            )
        ) {
            CategoryAllTransactionsScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToTransactionDetails = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/$it")
                }
            )
        }
        composable(
            MembersAdditionScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(MembersAdditionScreenDestination.categoryId) {
                    type = NavType.StringType
                }
            )
        ) {
            MembersAdditionScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            TransactionsScreenDestination.routeWithCategoryId,
            arguments = listOf(
                navArgument(TransactionsScreenDestination.categoryId) {
                    type = NavType.StringType
                }
            )
        ) {
            Log.d("NAV-TYPE", TransactionsScreenDestination.routeWithCategoryId)
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                showBackArrow = true,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                }
            )
        }
        composable(CategoriesScreenDestination.route) {
            CategoriesScreenComposable(
                navigateToCategoryDetailsScreen = {
                    navController.navigate("${CategoryDetailsScreenDestination.route}/${it}")
                },
                navigateToCategoryAdditionScreen = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                showBackArrow = true,
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                }
            )
        }
        composable(CategoryAdditionScreenDestination.route) {
            CategoryAdditionScreenComposable(
                navigateToCategoryDetailsScreen = {
                    navController.popBackStack(CategoryAdditionScreenDestination.route, true)
                    navController.navigate("${CategoryDetailsScreenDestination.route}/${it}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            BudgetListScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetListScreenDestination.categoryId) {
                    type = NavType.StringType
                },
                navArgument(BudgetListScreenDestination.categoryName) {
                    type = NavType.StringType
                },
            )
        ) {
            BudgetListScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToBudgetCreationScreen = {
                    navController.navigate(BudgetCreationScreenDestination.route)
                },
                navigateToBudgetCreationScreenWithCategoryId = {
                    navController.navigate("${BudgetCreationScreenDestination.route}/${it}")
                },
                navigateToBudgetInfoScreen = {
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                showBackArrow = true,
            )
        }
        composable(
            BudgetInfoScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetInfoScreenDestination.budgetId) {
                    type = NavType.StringType
                }
            )
        ) {
            BudgetInfoScreenComposable(
                navigateToBudgetAllTransactions = { budgetId, startDate, endDate ->
                    navController.navigate("${BudgetAllTransactionsScreenDestination.route}/$budgetId/$startDate/$endDate")
                },
                navigateToPreviousScreen = {
                    navController.popBackStack()
                },
                navigateToCategoryDetails = { categoryId ->
                    navController.navigate("${CategoryDetailsScreenDestination.route}/$categoryId")
                },
                navigateToCycleHistory = { budgetId ->
                    navController.navigate("${BudgetCycleHistoryScreenDestination.route}/$budgetId")
                },
                navigateToAuditTrail = { budgetId ->
                    navController.navigate("${BudgetAuditTrailScreenDestination.route}/$budgetId")
                },
                navigateToTransactionDetails = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/$it")
                }
            )
        }
        composable(
            BudgetAuditTrailScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetAuditTrailScreenDestination.budgetId) {
                    type = NavType.StringType
                }
            )
        ) {
            BudgetAuditTrailScreenComposable(
                navigateToPreviousScreen = {
                    navController.popBackStack()
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                }
            )
        }
        composable(
            BudgetCycleHistoryScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetCycleHistoryScreenDestination.budgetId) {
                    type = NavType.StringType
                }
            )
        ) {
            BudgetCycleHistoryScreenComposable(
                navigateToPreviousScreen = {
                    navController.popBackStack()
                },
                navigateToBudgetTransactions = { budgetId, startDate, endDate ->
                    navController.navigate("${BudgetAllTransactionsScreenDestination.route}/$budgetId/$startDate/$endDate")
                }
            )
        }
        composable(
            BudgetAllTransactionsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetAllTransactionsScreenDestination.budgetId) {
                    type = NavType.StringType
                },
                navArgument(BudgetAllTransactionsScreenDestination.startDate) {
                    type = NavType.StringType
                },
                navArgument(BudgetAllTransactionsScreenDestination.endDate) {
                    type = NavType.StringType
                }
            )
        ) {
            BudgetAllTransactionsScreenComposable(
                navigateToPreviousScreen = {
                    navController.popBackStack()
                },
                navigateToTransactionDetails = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/$it")
                }
            )
        }
        composable(
            TransactionsScreenDestination.routeWithBudgetId,
            arguments = listOf(
                navArgument(TransactionsScreenDestination.categoryId) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.budgetId) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.startDate) {
                    type = NavType.StringType
                },
                navArgument(TransactionsScreenDestination.endDate) {
                    type = NavType.StringType
                },
            )
        ) {
            Log.d("NAV_TYPE", TransactionsScreenDestination.routeWithBudgetId)
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyDirection}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToSubscriptionScreen = {
                    navController.navigate(SubscriptionScreenDestination.route)
                },
                showBackArrow = true,
                navigateToTransactionDetailsScreen = {
                    navController.navigate("${TransactionDetailsScreenDestination.route}/${it}")
                },
                navigateToLoginScreenWithArgs = { phoneNumber, password ->
                    navController.navigate("${LoginScreenDestination.route}/${phoneNumber}/${password}")
                }
            )
        }
        composable(
            TransactionDetailsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(TransactionDetailsScreenDestination.transactionId) {
                    type = NavType.StringType
                }
            )
        ) {
            TransactionDetailsScreenComposable(
                navigateToPreviousScreen = {
                    navController.popBackStack()
                },
                navigateToCategoryScreen = {
                    navController.navigate("${CategoryDetailsScreenDestination.route}/$it")
                }
            )
        }
        composable(SubscriptionScreenDestination.route) {
            SubscriptionScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToSmsFetchScreen = { navController.navigate(SMSFetchScreenDestination.route) },
            )
        }
        composable(BudgetCreationScreenDestination.route) {
            Log.i("NAV_WITH_ARGS", "FALSE")
            BudgetCreationScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToBudgetInfoScreen = {
                    navController.popBackStack(BudgetCreationScreenDestination.route, true)
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                navigateToCreateCategory = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                }
            )
        }
        composable(
            BudgetCreationScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(BudgetCreationScreenDestination.categoryId) {
                    type = NavType.StringType
                }
            )
        ) {
            BudgetCreationScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                },
                navigateToBudgetInfoScreen = {
                    navController.popBackStack(BudgetCreationScreenDestination.routeWithArgs, true)
                    navController.navigate("${BudgetInfoScreenDestination.route}/${it}")
                },
                navigateToCreateCategory = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                }
            )
        }
    }
}