package com.records.pesa.ui.screens

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.model.Point
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionCategoryCell
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.dashboard.chart.BarWithLineChart
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate

object DashboardScreenDestination: AppNavigation {
    override val title: String = "Dashboard screen"
    override val route: String = "dashboard-screen"

}
@Composable
fun DashboardScreenComposable(
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: DashboardScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        DashboardScreen(
            moneyInPointsData = uiState.moneyInPointsData,
            moneyOutPointsData = uiState.moneyOutPointsData,
            maxAmount = uiState.maxAmount,
            groupedTransactions = uiState.groupedTransactions,
            monthlyTransactions = uiState.monthlyTransactions,
            transactions = uiState.transactions,
            selectableYears = uiState.selectableYears,
            selectableMonths = uiState.selectableMonths,
            selectedYear = uiState.year,
            selectedMonth = uiState.month,
            onSelectYear = {
                viewModel.updateYear(it)
            },
            onSelectMonth = {
                viewModel.updateMonth(it)
            },
            transactionCategories = uiState.categories,
            currentBalance = formatMoneyValue(uiState.currentBalance),
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen
        )
    }
}

@Composable
fun DashboardScreen(
    moneyInPointsData: List<Point>,
    moneyOutPointsData: List<Point>,
    maxAmount: Float = 0.0f,
    groupedTransactions: List<GroupedTransactionData>,
    monthlyTransactions: List<MonthlyTransaction>,
    transactions: List<TransactionItem>,
    transactionCategories: List<TransactionCategory>,
    currentBalance: String,
    selectedYear: String,
    selectedMonth: String,
    selectableMonths: List<String>,
    selectableYears: List<String>,
    onSelectMonth: (month: String) -> Unit,
    onSelectYear: (month: String) -> Unit,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {

    var selectYearExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var selectMonthExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    Column(
        modifier = Modifier
            .padding(
                top = 8.dp,
                start = 16.dp,
                end = 16.dp,
                bottom = 16.dp
            )
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(
            currentBalance = currentBalance
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Transactions History",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = navigateToTransactionsScreen) {
                Text(text = "See all")
            }
        }
        Spacer(modifier = Modifier.height(10.dp))
        transactions.take(2).forEach {
            TransactionItemCell(
                transaction = it
            )
        }
        Spacer(modifier = Modifier.height(20.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if(transactionCategories.isEmpty()) {
                TextButton(onClick = navigateToCategoryAdditionScreen) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Add")
                        Spacer(modifier = Modifier.width(3.dp))
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Add category")
                    }

                }
            } else {
                TextButton(onClick = navigateToCategoriesScreen) {
                    Text(text = "See all")
                }
            }

        }
        Spacer(modifier = Modifier.height(10.dp))
        if(transactionCategories.isNotEmpty()) {
            transactionCategories.take(2).forEach {
                TransactionCategoryCell(
                    transactionCategory = it,
                    navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen
                )
            }
        } else {
            Text(
                text = "Create categories and add transactions to them to analyze the money flow in each category",
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                TextButton(onClick = { selectMonthExpanded = !selectMonthExpanded }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedMonth)
                        if(selectMonthExpanded) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                        } else {
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
                DropdownMenu(
                    expanded = selectMonthExpanded,
                    onDismissRequest = { selectMonthExpanded = !selectMonthExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .height(200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        selectableMonths.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(text = it)
                                },
                                onClick = {
                                    onSelectMonth(it)
                                    selectMonthExpanded = !selectMonthExpanded
                                }
                            )
                        }
                    }
                }

            }
            Spacer(modifier = Modifier.width(5.dp))
            Column {
                TextButton(onClick = { selectYearExpanded = !selectYearExpanded }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = selectedYear)
                        if(selectYearExpanded) {
                            Icon(imageVector = Icons.Default.KeyboardArrowUp, contentDescription = null)
                        } else {
                            Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                        }
                    }
                }
                DropdownMenu(
                    expanded = selectYearExpanded,
                    onDismissRequest = { selectYearExpanded = !selectYearExpanded }
                ) {
                    Column(
                        modifier = Modifier
                            .height(200.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        selectableYears.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(text = it)
                                },
                                onClick = {
                                    onSelectYear(it)
                                    selectYearExpanded = !selectYearExpanded
                                }
                            )
                        }
                    }
                }
            }


        }
        Spacer(modifier = Modifier.height(10.dp))
        if(monthlyTransactions.isNotEmpty()) {
            BarWithLineChart(
                transactions = monthlyTransactions,
                maxAmount = maxAmount,
                moneyInPointsData = moneyInPointsData,
                moneyOutPointsData = moneyOutPointsData,
                modifier = Modifier
                    .height(350.dp)
            )
        } else {
            Text(
                text = "No transactions for this week",
                textAlign = TextAlign.Center,
                fontStyle = FontStyle.Italic,
                fontWeight = FontWeight.Light,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
            )
        }

    }
}



@Composable
fun HeaderSection(
    currentBalance: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ElevatedCard(
            modifier = Modifier
        ) {
            Column {
                Text(
                    text = "M-PESA transactions since ${formatLocalDate(LocalDate.now())}",
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(10.dp)
                )
            }

        }
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Account balance"
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = currentBalance,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = { /*TODO*/ }
            ) {
                Icon(painter = painterResource(id = R.drawable.visibility_on), contentDescription = null)
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "${formatLocalDate(LocalDate.now())} (today)")
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = 16.dp
                )
        ) {
            Card {
                Box {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
                        Text(
                            text = "Ksh1,200",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surfaceTint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Card {
                Box {
                    Row(
                        modifier = Modifier
                            .padding(16.dp)
                    ) {
                        Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
                        Text(
                            text = "Ksh3,400",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
    }

}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        DashboardScreen(
            navigateToTransactionsScreen = {},
            transactions = transactions,
            currentBalance = "Ksh 5,350",
            maxAmount = groupedTransactions.maxOf { maxOf(it.moneyIn, it.moneyOut) },
            groupedTransactions = groupedTransactions,
            monthlyTransactions = emptyList(),
            moneyInPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyIn)},
            moneyOutPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyOut)
            },
            selectableMonths = emptyList(),
            selectedYear = "2024",
            selectableYears = emptyList(),
            selectedMonth = "AUGUST",
            onSelectMonth = {},
            onSelectYear = {},
            transactionCategories = transactionCategories,
            navigateToCategoriesScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToCategoryDetailsScreen = {}
        )
    }
}