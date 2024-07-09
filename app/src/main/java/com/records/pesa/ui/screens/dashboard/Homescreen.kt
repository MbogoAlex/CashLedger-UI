package com.records.pesa.ui.screens.dashboard

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.R
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetScreenComposable
import com.records.pesa.ui.theme.CashLedgerTheme

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreenComposable(
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        HomeScreenTabItem(
            name = "Dashboard",
            icon = R.drawable.dashboard,
            tab = HomeScreenTab.DASHBOARD
        ),
        HomeScreenTabItem(
            name = "Chart",
            icon = R.drawable.chart,
            tab = HomeScreenTab.CHART
        ),
        HomeScreenTabItem(
            name = "Budget",
            icon = R.drawable.budget_2,
            tab = HomeScreenTab.BUDGET
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.DASHBOARD)
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
            tabs = tabs
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HomeScreen(
    currentTab: HomeScreenTab,
    onTabChange: (HomeScreenTab) -> Unit,
    tabs: List<HomeScreenTabItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when(currentTab) {
            HomeScreenTab.DASHBOARD -> {
                DashboardScreenComposable(
                    modifier = Modifier
                        .weight(1f)
                )
            }
            HomeScreenTab.CHART -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Text(text = "Chart")
                }
            }
            HomeScreenTab.BUDGET -> {
                BudgetScreenComposable(
                    modifier = Modifier
                        .weight(1f)
                )
            }
        }
        BottomNavBar(
            tabItems = tabs,
            currentTab = currentTab,
            onTabSelected = onTabChange
        )

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

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val tabs = listOf(
        HomeScreenTabItem(
            name = "Dashboard",
            icon = R.drawable.dashboard,
            tab = HomeScreenTab.DASHBOARD
        ),
        HomeScreenTabItem(
            name = "Chart",
            icon = R.drawable.chart,
            tab = HomeScreenTab.CHART
        ),
        HomeScreenTabItem(
            name = "Budget",
            icon = R.drawable.budget_2,
            tab = HomeScreenTab.BUDGET
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.DASHBOARD)
    }
    CashLedgerTheme {
        HomeScreen(
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs
        )
    }
}