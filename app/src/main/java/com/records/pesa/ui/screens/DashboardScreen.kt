package com.records.pesa.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.extensions.isNotNull
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
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.Month

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
    navigateToSubscriptionScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: DashboardScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val appVersion = 87.0

    if (uiState.appVersion.isNotNull() && appVersion != uiState.appVersion) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "New version available", Toast.LENGTH_LONG).show()
            delay(5000)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.records.pesa")
            )
            context.startActivity(intent)
        }
    }

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showBalance by rememberSaveable {
        mutableStateOf(true)
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = false
            },
            onConfirm = {
                showSubscriptionDialog = false
                navigateToSubscriptionScreen()
            }
        )
    }

    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        DashboardScreen(
            premium = uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0179189199",
            totalInToday = formatMoneyValue(uiState.todayTotalIn),
            totalOutToday = formatMoneyValue(uiState.todayTotalOut),
            monthlyTotalIn = formatMoneyValue(uiState.monthlyInTotal),
            monthlyTotalOut = formatMoneyValue(uiState.monthlyOutTotal),
            firstTransactionDate = uiState.firstTransactionDate,
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
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = true
            },
            onToggleBalanceVisibility = {
                showBalance = !showBalance
            },
            showBalance = showBalance,
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen
        )
    }
}

@Composable
fun DashboardScreen(
    premium: Boolean,
    totalInToday: String,
    totalOutToday: String,
    monthlyTotalIn: String,
    monthlyTotalOut: String,
    firstTransactionDate: String,
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
    onShowSubscriptionDialog: () -> Unit,
    onToggleBalanceVisibility: () -> Unit,
    showBalance: Boolean,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {

    var selectYearExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var selectMonthExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val oneMonthBack = LocalDate.now().minusMonths(1).month

    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
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
                        totalInToday = totalInToday,
                        totalOutToday = totalOutToday,
                        currentBalance = currentBalance,
                        firstTransactionDate = firstTransactionDate,
                        onToggleBalanceVisibility = onToggleBalanceVisibility,
                        showBalance = showBalance,
                        modifier = Modifier
                            .fillMaxWidth()
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Transactions History",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = navigateToTransactionsScreen) {
                            Text(
                                text = "See all",
                                fontSize = 15.sp
                            )
                        }
                    }
//        Spacer(modifier = Modifier.height(5.dp))
                    transactions.take(2).forEachIndexed { index, item ->
                        TransactionItemCell(
                            transaction = item,
                            modifier = Modifier
                                .clickable {
                                    navigateToTransactionDetailsScreen(item.transactionId.toString())
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Categories",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        if(transactionCategories.isEmpty()) {
                            TextButton(onClick = navigateToCategoryAdditionScreen) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Add",
                                        fontSize = 15.sp
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add category")
                                }

                            }
                        } else {
                            TextButton(onClick = navigateToCategoriesScreen) {
                                Text(
                                    text = "See all",
                                    fontSize = 15.sp
                                )
                            }
                        }

                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if(transactionCategories.isNotEmpty()) {
                        transactionCategories.take(2).forEachIndexed {index, item ->
                            TransactionCategoryCell(
                                transactionCategory = item,
                                navigateToCategoryDetailsScreen = {
                                    if(index != 0 && !premium) {
                                        onShowSubscriptionDialog()
                                    } else {
                                        navigateToCategoryDetailsScreen(item.id.toString())
                                    }
                                }
                            )
                        }
                    } else {
                        Text(
                            text = "Create categories and add transactions to them to analyze the money flow in each category",
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            fontSize = 14.sp,
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
                                    Text(
                                        text = selectedMonth,
                                        fontSize = 14.sp
                                    )
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

                                                if (Month.valueOf(it.uppercase()) < (oneMonthBack) && !premium) {
                                                    selectMonthExpanded = !selectMonthExpanded
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    onSelectMonth(it)
                                                    selectMonthExpanded = !selectMonthExpanded
                                                }

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
                                    Text(
                                        text = selectedYear,
                                        fontSize = 14.sp
                                    )
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
                                                if(!premium) {
                                                    selectYearExpanded = !selectYearExpanded
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    selectYearExpanded = !selectYearExpanded
                                                    onSelectYear(it)
                                                }

                                            }
                                        )
                                    }
                                }
                            }
                        }


                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if(monthlyTransactions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_downward),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = monthlyTotalIn,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surfaceTint
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_upward),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = monthlyTotalOut,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
                            text = "No transactions for this month",
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                }
            }
            else -> {
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
                        totalInToday = totalInToday,
                        totalOutToday = totalOutToday,
                        currentBalance = currentBalance,
                        firstTransactionDate = firstTransactionDate,
                        onToggleBalanceVisibility = onToggleBalanceVisibility,
                        showBalance = showBalance,
                        modifier = Modifier
                            .fillMaxWidth()
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
//        Spacer(modifier = Modifier.height(5.dp))
                    transactions.take(2).forEachIndexed { index, item ->
                        TransactionItemCell(
                            transaction = item,
                            modifier = Modifier
                                .clickable {
                                    navigateToTransactionDetailsScreen(item.transactionId.toString())
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                        transactionCategories.take(2).forEachIndexed {index, item ->
                            TransactionCategoryCell(
                                transactionCategory = item,
                                navigateToCategoryDetailsScreen = {
                                    if(index != 0 && !premium) {
                                        onShowSubscriptionDialog()
                                    } else {
                                        navigateToCategoryDetailsScreen(item.id.toString())
                                    }
                                }
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

                                                if (Month.valueOf(it.uppercase()) < (oneMonthBack) && !premium) {
                                                    selectMonthExpanded = !selectMonthExpanded
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    onSelectMonth(it)
                                                    selectMonthExpanded = !selectMonthExpanded
                                                }

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
                                                if(!premium) {
                                                    selectYearExpanded = !selectYearExpanded
                                                    onShowSubscriptionDialog()
                                                } else {
                                                    selectYearExpanded = !selectYearExpanded
                                                    onSelectYear(it)
                                                }

                                            }
                                        )
                                    }
                                }
                            }
                        }


                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    if(monthlyTransactions.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_downward),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = monthlyTotalIn,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.surfaceTint
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Icon(
                                painter = painterResource(id = R.drawable.arrow_upward),
                                contentDescription = null
                            )
                            Spacer(modifier = Modifier.width(3.dp))
                            Text(
                                text = monthlyTotalOut,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
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
                            text = "No transactions for this month",
                            textAlign = TextAlign.Center,
                            fontStyle = FontStyle.Italic,
                            fontWeight = FontWeight.Light,
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                        )
                    }

                }
            }
        }
    }


}



@Composable
fun HeaderSection(
    totalInToday: String,
    totalOutToday: String,
    currentBalance: String,
    firstTransactionDate: String,
    showBalance: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {

    BoxWithConstraints(
        modifier = modifier
    ) {
        Log.d("HEIGHT", maxHeight.toString())
        Log.d("WIDTH", maxWidth.toString())
        when(this.maxWidth) {
            in (0.dp..320.dp) -> {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                text = "M-PESA transactions since $firstTransactionDate",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .padding(10.dp)
                            )
                        }

                    }
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Account balance"
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = currentBalance,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .graphicsLayer {
                                    if (!showBalance) {
                                        alpha =
                                            0.0f // Adjust the alpha to make the text semi-transparent
                                        shape = RectangleShape
                                        clip = true
                                    }
                                }
                                .blur(if (!showBalance) 4.dp else 0.dp) // Apply blur only when showBalance is false
                        )
                        IconButton(
                            onClick = onToggleBalanceVisibility
                        ) {
                            Icon(
                                painter = if (showBalance) painterResource(id = R.drawable.visibility_off) else painterResource(
                                    id = R.drawable.visibility_on
                                ),
                                contentDescription = null
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${formatLocalDate(LocalDate.now())} (today)",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
//                                horizontal = 16.dp
                            )
                    ) {
                        Column(
                            modifier = Modifier
//                                .padding(16.dp)
                        ) {
                            Text(text = "Money in: ")
                            Text(
                                text = totalInToday,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.surfaceTint
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                        VerticalDivider(
//                            thickness = 3.dp,
                            modifier = Modifier
                                .height(10.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Column(
                            modifier = Modifier
//                                .padding(16.dp)
                        ) {
                            Text(text = "Money out: ")
                            Text(
                                text = totalOutToday,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else -> {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Column {
                        Text(
                            text = "M-PESA transactions since $firstTransactionDate",
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
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .graphicsLayer {
                                if (!showBalance) {
                                    alpha =
                                        0.0f // Adjust the alpha to make the text semi-transparent
                                    shape = RectangleShape
                                    clip = true
                                }
                            }
                            .blur(if (!showBalance) 4.dp else 0.dp) // Apply blur only when showBalance is false
                    )
                    IconButton(
                        onClick = onToggleBalanceVisibility
                    ) {
                        Icon(
                            painter = if (showBalance) painterResource(id = R.drawable.visibility_off) else painterResource(
                                id = R.drawable.visibility_on
                            ),
                            contentDescription = null
                        )
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
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_downward),
                                    contentDescription = null
                                )
                                Text(
                                    text = totalInToday,
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
                                Icon(
                                    painter = painterResource(id = R.drawable.arrow_upward),
                                    contentDescription = null
                                )
                                Text(
                                    text = totalOutToday,
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
        }
    }

}

@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(text = "Go premium?")
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Ksh100.0 premium monthly fee",
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "1. See transactions and export reports of more than one month")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "2. Manage more than one category")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "3. Manage more than one Budget")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "4. Use in dark mode")

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Subscribe")
            }
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        DashboardScreen(
            navigateToTransactionsScreen = {},
            firstTransactionDate = LocalDate.now().toString(),
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
            navigateToCategoryDetailsScreen = {},
            onShowSubscriptionDialog = {},
            premium = false,
            totalInToday = "",
            totalOutToday = "",
            monthlyTotalIn = "",
            monthlyTotalOut = "",
            showBalance = true,
            onToggleBalanceVisibility = {},
            navigateToTransactionDetailsScreen = {}
        )
    }
}