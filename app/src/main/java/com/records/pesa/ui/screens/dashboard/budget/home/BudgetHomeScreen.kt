package com.records.pesa.ui.screens.dashboard.budget.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.unit.dp
import com.records.pesa.R
import com.records.pesa.reusables.BudgetHomeScreenTab
import com.records.pesa.reusables.BudgetHomeScreenTabItem
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun BudgetHomeScreenComposable(
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        BudgetHomeScreenTabItem(
            name = "Info",
            icon = R.drawable.info,
            tab = BudgetHomeScreenTab.INFO
        ),
        BudgetHomeScreenTabItem(
            name = "Money in",
            icon = R.drawable.arrow_downward,
            tab = BudgetHomeScreenTab.MONEY_IN
        ),
        BudgetHomeScreenTabItem(
            name = "Money out",
            icon = R.drawable.arrow_upward,
            tab = BudgetHomeScreenTab.MONEY_OUT
        ),
        BudgetHomeScreenTabItem(
            name = "Chart",
            icon = R.drawable.chart,
            tab = BudgetHomeScreenTab.CHART
        ),
    )
    var currentTab by rememberSaveable {
        mutableStateOf(BudgetHomeScreenTab.INFO)
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BudgetHomeScreen(
            currentTab = currentTab,
            tabs = tabs,
            onTabSelected = {
                currentTab = it
            }
        )
    }
}

@Composable
fun BudgetHomeScreen(
    currentTab: BudgetHomeScreenTab,
    tabs: List<BudgetHomeScreenTabItem>,
    onTabSelected: (BudgetHomeScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        when(currentTab) {
            BudgetHomeScreenTab.INFO -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(text = "Info")
                }
            }
            BudgetHomeScreenTab.MONEY_IN -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(text = "Money in")
                }
            }
            BudgetHomeScreenTab.MONEY_OUT -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(text = "Money out")
                }
            }
            BudgetHomeScreenTab.CHART -> {
                Box(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(text = "Chart")
                }
            }
        }
        BottomNavBar(
            tabItems = tabs,
            currentTab = currentTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun BottomNavBar(
    tabItems: List<BudgetHomeScreenTabItem>,
    currentTab: BudgetHomeScreenTab,
    onTabSelected: (BudgetHomeScreenTab) -> Unit,
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
fun BudgetHomeScreenPreview() {
    val tabs = listOf(
        BudgetHomeScreenTabItem(
            name = "Info",
            icon = R.drawable.info,
            tab = BudgetHomeScreenTab.INFO
        ),
        BudgetHomeScreenTabItem(
            name = "Money in",
            icon = R.drawable.arrow_downward,
            tab = BudgetHomeScreenTab.MONEY_IN
        ),
        BudgetHomeScreenTabItem(
            name = "Money out",
            icon = R.drawable.arrow_upward,
            tab = BudgetHomeScreenTab.MONEY_OUT
        ),
        BudgetHomeScreenTabItem(
            name = "Chart",
            icon = R.drawable.chart,
            tab = BudgetHomeScreenTab.CHART
        ),
    )
    var currentTab by rememberSaveable {
        mutableStateOf(BudgetHomeScreenTab.INFO)
    }
    CashLedgerTheme {
        BudgetHomeScreen(
            currentTab = currentTab,
            tabs = tabs,
            onTabSelected = {
                currentTab = it
            }
        )
    }
}