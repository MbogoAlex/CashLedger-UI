package com.records.pesa.ui.screens

import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.GroupedTransactionData
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.dashboard.chart.BarWithLineChart
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime

object DashboardScreenDestination: AppNavigation {
    override val title: String = "Dashboard screen"
    override val route: String = "dashboard-screen"

}
@RequiresApi(Build.VERSION_CODES.O)
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
            transactions = uiState.transactions,
            transactionCategories = uiState.categories,
            currentBalance = formatMoneyValue(uiState.currentBalance),
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DashboardScreen(
    moneyInPointsData: List<Point>,
    moneyOutPointsData: List<Point>,
    maxAmount: Float = 0.0f,
    groupedTransactions: List<GroupedTransactionData>,
    transactions: List<TransactionItem>,
    transactionCategories: List<TransactionCategory>,
    currentBalance: String,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        HeaderSection(
            currentBalance = currentBalance
        )
        Spacer(modifier = Modifier.height(20.dp))
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
            Text(
                text = "This week",
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = navigateToCategoriesScreen) {
                Text(text = "Customize chart")
            }

        }
        Spacer(modifier = Modifier.height(10.dp))
        if(groupedTransactions.isNotEmpty()) {
            BarWithLineChart(
                transactions = groupedTransactions,
                maxAmount = maxAmount,
                moneyInPointsData = moneyInPointsData,
                moneyOutPointsData = moneyOutPointsData,
                modifier = Modifier
                    .height(450.dp)
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



@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun HeaderSection(
    currentBalance: String,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Text(text = "Hello,")
//                Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "Alex Mbogo",
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                modifier = Modifier
                    .padding(
                        top = 10.dp,
                        start = 10.dp,
                        end = 10.dp
                    )
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                ) {
                    Text(text = formatIsoDateTime(LocalDateTime.now()))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Current balance")
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(
                                painter = painterResource(id = R.drawable.visibility_off),
                                contentDescription = "Hide balance"
                            )
                        }
                    }

//                Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = currentBalance,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row() {
                        Column {
                            Row {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_downward),
                                    contentDescription = null
                                )
                                Text(text = "Income")
                            }
                            Text(text = "KES 1,200")
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        Column {
                            Row {
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_upward),
                                    contentDescription = null
                                )
                                Text(text = "Expenses")
                            }
                            Text(text = "KES 4,330")
                        }
                    }
                }
            }

        }
    }
}


@RequiresApi(Build.VERSION_CODES.O)
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
            moneyInPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyIn)},
            moneyOutPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyOut)
            },
            transactionCategories = transactionCategories,
            navigateToCategoriesScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToCategoryDetailsScreen = {}
        )
    }
}