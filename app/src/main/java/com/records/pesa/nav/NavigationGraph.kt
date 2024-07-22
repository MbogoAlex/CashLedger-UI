package com.records.pesa.nav

import android.os.Build
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
import com.records.pesa.ui.screens.dashboard.HomeScreenComposable
import com.records.pesa.ui.screens.dashboard.HomeScreenDestination
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryAdditionScreenDestination
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoryDetailsScreenDestination
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenComposable
import com.records.pesa.ui.screens.dashboard.category.MembersAdditionScreenDestination
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenComposable
import com.records.pesa.ui.screens.transactions.SingleEntityTransactionsScreenDestination
import com.records.pesa.ui.screens.transactions.TransactionsScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionsScreenDestination

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun NavigationGraph(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = HomeScreenDestination.route,
    ) {
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
                    navController.navigate(CategoryDetailsScreenDestination.route)
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(TransactionsScreenDestination.route) {
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyIn ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyIn}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
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
                navArgument(SingleEntityTransactionsScreenDestination.moneyIn) {
                    type = NavType.StringType
                },
            )
        ) {
            SingleEntityTransactionsScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(
            CategoryDetailsScreenDestination.routeWithArgs,
            arguments = listOf(
                navArgument(CategoryDetailsScreenDestination.categoryId) {
                    type = NavType.StringType
                }
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
                navigateToCategoryBudgetListScreen = {
                    navController.navigate("${BudgetListScreenDestination.route}/${it}")
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
            TransactionsScreenComposable(
                navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyIn ->
                    navController.navigate("${SingleEntityTransactionsScreenDestination.route}/${userId}/${transactionType}/${entity}/${startDate}/${endDate}/${times}/${moneyIn}")
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
        composable(CategoryDetailsScreenDestination.route) {
            CategoriesScreenComposable(
                navigateToCategoryDetailsScreen = {
                    navController.navigate("${CategoryDetailsScreenDestination.route}/${it}")
                },
                navigateToCategoryAdditionScreen = {
                    navController.navigate(CategoryAdditionScreenDestination.route)
                },
                navigateToPreviousScreen = {
                    navController.navigateUp()
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
                }
            )
        ) {
            BudgetListScreenComposable(
                navigateToPreviousScreen = {
                    navController.navigateUp()
                }
            )
        }
    }
}