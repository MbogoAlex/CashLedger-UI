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
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetCreationScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetInfoScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenDestination
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
                navigateToHomeScreen = {
                    navController.navigate(HomeScreenDestination.route)
                },
                navigateToBudgetCreationScreen = {
                    navController.navigate("${BudgetCreationScreenDestination.route}/${it}")
                },
                navigateToCategoryBudgetListScreen = {categoryId, categoryName ->
                    navController.navigate("${BudgetListScreenDestination.route}/${categoryId}/${categoryName}")
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
                navigateToTransactionsScreen = {categoryId, budgetId, startDate, endDate ->
                    navController.navigate("${TransactionsScreenDestination.route}/${categoryId}/${budgetId}/${startDate}/${endDate}")
                },
                navigateToPreviousScreen = {
                    navController.popBackStack()
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
                }
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
                }
            )
        }
    }
}