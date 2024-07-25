package com.records.pesa.ui.screens.dashboard.chart

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import com.records.pesa.reusables.ChartScreenTab
import com.records.pesa.reusables.ChartScreenTabItem
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun ChartHomeScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ChartScreenTabItem(
            name = "Combined",
            icon = R.drawable.combined_chart,
            tab = ChartScreenTab.COMBINED_CHART
        ),

        ChartScreenTabItem(
            name = "Separate",
            icon = R.drawable.separated_chart,
            tab = ChartScreenTab.SEPARATE_CHART
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(ChartScreenTab.COMBINED_CHART)
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        ChartHomeScreen(
            tabs = tabs,
            currentTab = currentTab,
            onChangeTab = {
                currentTab = it
            },
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }

}

@Composable
fun ChartHomeScreen(
    tabs: List<ChartScreenTabItem>,
    currentTab: ChartScreenTab,
    onChangeTab: (tab: ChartScreenTab) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Previous screen"
                )
            }
        }
        when(currentTab) {
            ChartScreenTab.COMBINED_CHART -> {
                CombinedChartScreenComposable(
                    modifier = Modifier
                        .weight(1f)
                )
            }
            ChartScreenTab.SEPARATE_CHART -> {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                ) {
                    Text(text = "Separate chart")
                }
            }
        }
        BottomNavBar(
            tabItems = tabs,
            currentTab = currentTab,
            onTabSelected = onChangeTab
        )
    }
}

@Composable
private fun BottomNavBar(
    tabItems: List<ChartScreenTabItem>,
    currentTab: ChartScreenTab,
    onTabSelected: (ChartScreenTab) -> Unit,
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
fun ChartHomeScreenPreview(
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        ChartScreenTabItem(
            name = "Combined",
            icon = R.drawable.combined_chart,
            tab = ChartScreenTab.COMBINED_CHART
        ),

        ChartScreenTabItem(
            name = "Separate",
            icon = R.drawable.separated_chart,
            tab = ChartScreenTab.SEPARATE_CHART
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(ChartScreenTab.COMBINED_CHART)
    }
    CashLedgerTheme {
        ChartHomeScreen(
            tabs = tabs,
            currentTab = currentTab,
            onChangeTab = {
                currentTab = it
            },
            navigateToPreviousScreen = {}
        )
    }
}