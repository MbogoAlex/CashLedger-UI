package com.records.pesa.nav

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.DashboardScreenDestination
import com.records.pesa.ui.screens.dashboard.HomeScreenComposable
import com.records.pesa.ui.screens.dashboard.HomeScreenDestination
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
                }
            )
        }
        composable(TransactionsScreenDestination.route) {
            TransactionsScreenComposable()
        }
    }
}