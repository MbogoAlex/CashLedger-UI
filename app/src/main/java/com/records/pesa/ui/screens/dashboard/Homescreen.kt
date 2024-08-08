package com.records.pesa.ui.screens.dashboard

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.profile.AccountInformationScreenComposable
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.launch

object HomeScreenDestination: AppNavigation {
    override val title: String = "Home screen"
    override val route: String = "home-screen"

}
@Composable
fun HomeScreenComposable(
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val activity = (LocalContext.current as? Activity)
    BackHandler(onBack = {activity?.finish()})
    val tabs = listOf(
        HomeScreenTabItem(
            name = "Home",
            icon = R.drawable.home,
            tab = HomeScreenTab.HOME
        ),
        HomeScreenTabItem(
            name = "All transactions",
            icon = R.drawable.transactions,
            tab = HomeScreenTab.ALL_TRANSACTIONS
        ),
        HomeScreenTabItem(
            name = "Categories",
            icon = R.drawable.categories,
            tab = HomeScreenTab.CATEGORIES
        ),
        HomeScreenTabItem(
            name = "Budgets",
            icon = R.drawable.budget_pic,
            tab = HomeScreenTab.BUDGETS
        ),
        HomeScreenTabItem(
            name = "Account information",
            icon = R.drawable.account_info,
            tab = HomeScreenTab.ACCOUNT_INFO
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.HOME)
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        HomeScreen(
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs,
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
            navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToHomeScreen = {
                currentTab = HomeScreenTab.HOME
            },
            navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
        )
    }
}

@Composable
fun HomeScreen(
    currentTab: HomeScreenTab,
    onTabChange: (HomeScreenTab) -> Unit,
    tabs: List<HomeScreenTabItem>,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .padding(
                                horizontal = 20.dp
                            )
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.cashledger_logo),
                            contentDescription = null,
                            modifier = Modifier
                                .height(100.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(15.dp))
                    Divider()
                    Spacer(modifier = Modifier.height(15.dp))
                    Column(
                        modifier = Modifier
                            .verticalScroll(rememberScrollState())
                    ) {
                        for(tab in tabs) {
                            NavigationDrawerItem(
                                label = {
                                    Row {
                                        Icon(
                                            painter = painterResource(id = tab.icon),
                                            contentDescription = tab.name
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(text = tab.name)
                                    }
                                },
                                selected = currentTab == tab.tab,
                                onClick = {
                                    onTabChange(tab.tab)
                                    scope.launch {
                                        drawerState.close()
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(
                        horizontal = 16.dp
                    )
            ) {
                IconButton(onClick = {
                    scope.launch {
                        if(drawerState.isClosed) drawerState.open() else drawerState.close()
                    }
                }) {
                    Icon(
                        tint = Color.Gray,
                        painter = painterResource(id = R.drawable.menu),
                        contentDescription = "Menu"
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                Switch(checked = false, onCheckedChange = {})
            }
            when(currentTab) {
                HomeScreenTab.HOME -> {
                    DashboardScreenComposable(
                        navigateToTransactionsScreen = navigateToTransactionsScreen,
                        navigateToCategoriesScreen = navigateToCategoriesScreen,
                        navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
                        navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
                        modifier = Modifier
                            .weight(1f)
                    )
                }
                HomeScreenTab.ALL_TRANSACTIONS -> {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize()
                    ) {
                        Text(text = "Chart")
                    }
                }
                HomeScreenTab.CATEGORIES -> {
                    BudgetListScreenComposable(
                        navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
                        navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
                        navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
                        navigateToPreviousScreen = navigateToPreviousScreen,
                        modifier = Modifier
                    )
                }
                HomeScreenTab.BUDGETS -> {}
                HomeScreenTab.ACCOUNT_INFO -> {
                    AccountInformationScreenComposable(
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
                    )
                }
            }
        }
    }


}

@Composable
private fun BottomNavBar(
    tabItems: List<HomeScreenTabItem>,
    currentTab: HomeScreenTab,
    onTabSelected: (HomeScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar {
        for(item in tabItems) {
            NavigationBarItem(
                label = {
                    Text(text = item.name)
                },
                selected = item.tab == currentTab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.name
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val tabs = listOf(
        HomeScreenTabItem(
            name = "Home",
            icon = R.drawable.baseline_home_24,
            tab = HomeScreenTab.HOME
        ),
        HomeScreenTabItem(
            name = "All transactions",
            icon = R.drawable.transactions,
            tab = HomeScreenTab.ALL_TRANSACTIONS
        ),
        HomeScreenTabItem(
            name = "Categories",
            icon = R.drawable.categories,
            tab = HomeScreenTab.CATEGORIES
        ),
        HomeScreenTabItem(
            name = "Budgets",
            icon = R.drawable.budget_pic,
            tab = HomeScreenTab.BUDGETS
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.HOME)
    }
    CashLedgerTheme {
        HomeScreen(
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs,
            navigateToTransactionsScreen = {},
            navigateToCategoriesScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToCategoryDetailsScreen = {},
            navigateToBudgetInfoScreen = {},
            navigateToBudgetCreationScreen = {},
            navigateToBudgetCreationScreenWithCategoryId = {},
            navigateToPreviousScreen = {},
            navigateToHomeScreen = {},
            navigateToLoginScreenWithArgs = {phoneNumber, password ->  }
        )
    }
}