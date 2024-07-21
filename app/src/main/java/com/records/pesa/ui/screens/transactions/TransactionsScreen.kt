package com.records.pesa.ui.screens.transactions

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.SortedTransactionItem
import com.records.pesa.models.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.TransactionScreenTabItem
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.moneyInSortedTransactionItems
import com.records.pesa.reusables.moneyOutSortedTransactionItems
import com.records.pesa.reusables.sortTypes
import com.records.pesa.reusables.transactionTypes
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.ZoneId

object TransactionsScreenDestination: AppNavigation {
    override val route = "Transactions Screen"
    override val title = "transactions-screen"
    val categoryId: String = "categoryId"
    val budgetId: String = "budgetId"
    val categoryName: String = "categoryName"
    val budgetName: String = "budgetName"
    val startDate: String = "startDate"
    val endDate: String = "endDate"
    val routeWithArgs: String = "$route/{$categoryId}/{$budgetId}/{$categoryName}/{$budgetName}/{$startDate}/{$endDate}"
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransactionsScreenComposable(
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyIn: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TransactionsScreenViewModelScreen = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val tabs = listOf(
        TransactionScreenTabItem(
            name = "All",
            tab = TransactionScreenTab.ALL_TRANSACTIONS,
            icon = R.drawable.list
        ),
        TransactionScreenTabItem(
            name = "Money in",
            tab = TransactionScreenTab.MONEY_IN,
            icon = R.drawable.arrow_downward
        ),
        TransactionScreenTabItem(
            name = "Money out",
            tab = TransactionScreenTab.MONEY_OUT,
            icon = R.drawable.arrow_upward
        ),
        TransactionScreenTabItem(
            name = "Chart",
            tab = TransactionScreenTab.CHART,
            icon = R.drawable.chart
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(TransactionScreenTab.ALL_TRANSACTIONS)
    }

    var selectedSortCriteria by rememberSaveable {
        mutableStateOf("Amount")
    }

    var typeMenuExpanded by remember {
        mutableStateOf(false)
    }

    var sortMenuExpanded by remember {
        mutableStateOf(false)
    }

    var transactionsLoaded by rememberSaveable {
        mutableStateOf(false)
    }

    var showDateRangePicker by rememberSaveable {
        mutableStateOf(false)
    }

    if(showDateRangePicker) {
        DateRangePickerDialog(
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse((uiState.endDate)),
            defaultStartDate = uiState.defaultStartDate,
            defaultEndDate = uiState.defaultEndDate,
            onChangeStartDate = {
                viewModel.changeStartDate(it, currentTab)
            },
            onChangeLastDate = {
                viewModel.changeEndDate(it, currentTab)
            },
            onDismiss = {
                showDateRangePicker = !showDateRangePicker
            },
            onConfirm = {
                showDateRangePicker = !showDateRangePicker
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        TransactionsScreen(
            transactions = uiState.transactions,
            moneyInsortedTransactionItems = uiState.moneyInSorted,
            moneyOutsortedTransactionItems = uiState.moneyOutSorted,
            totalMoneyIn = formatMoneyValue(uiState.totalMoneyIn),
            totalMoneyOut = formatMoneyValue(uiState.totalMoneyOut),
            bottomTabItems = tabs,
            currentTab = currentTab,
            onTabSelected = {
                transactionsLoaded = false
                currentTab = it
                if(it == TransactionScreenTab.MONEY_IN && !transactionsLoaded) {
                    transactionsLoaded = true
                    viewModel.getMoneyInSortedTransactions()
                } else if(it == TransactionScreenTab.MONEY_OUT && !transactionsLoaded) {
                    transactionsLoaded = true
                    viewModel.getMoneyOutSortedTransactions()
                } else if(it == TransactionScreenTab.ALL_TRANSACTIONS && !transactionsLoaded) {
                    transactionsLoaded = true
                    viewModel.getTransactions()
                }
            },
            typeMenuExpanded = typeMenuExpanded,
            onExpandTypeMenu = {
                typeMenuExpanded = !typeMenuExpanded
            },
            selectedType = uiState.transactionType,
            onSelectType = {
                viewModel.changeTransactionType(it, currentTab)
                typeMenuExpanded = !typeMenuExpanded
            },
            sortMenuExpanded = sortMenuExpanded,
            onExpandSortMenu = {
                sortMenuExpanded = !sortMenuExpanded
            },
            selectedSortCriteria = selectedSortCriteria,
            onSelectSortCriteria = {
                selectedSortCriteria = it
                sortMenuExpanded = !sortMenuExpanded
            },
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            searchText = uiState.entity,
            onChangeSearchText = {
                viewModel.changeEntity(it, currentTab)
            },
            onClearSearch = {
                viewModel.clearSearch(currentTab)
            },
            onSelectDateRange = {
                showDateRangePicker = !showDateRangePicker
            },
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyIn ->
                navigateToEntityTransactionsScreen("1", transactionType, entity, uiState.startDate, uiState.endDate, times, moneyIn)
            },
            categoryName = uiState.categoryName,
            budgetName = uiState.budgetName
        )

    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun TransactionsScreen(
    transactions: List<TransactionItem>,
    moneyInsortedTransactionItems: List<SortedTransactionItem>,
    moneyOutsortedTransactionItems: List<SortedTransactionItem>,
    totalMoneyIn: String,
    totalMoneyOut: String,
    bottomTabItems: List<TransactionScreenTabItem>,
    currentTab: TransactionScreenTab,
    onTabSelected: (TransactionScreenTab) -> Unit,
    onExpandTypeMenu: () -> Unit,
    typeMenuExpanded: Boolean,
    selectedType: String,
    onSelectType: (type: String) -> Unit,
    onExpandSortMenu: () -> Unit,
    sortMenuExpanded: Boolean,
    selectedSortCriteria: String,
    onSelectSortCriteria: (type: String) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    searchText: String,
    categoryName: String?,
    budgetName: String?,
    onChangeSearchText: (searchText: String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectDateRange: () -> Unit,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyIn: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            floatingActionButton = {
                Button(
                    onClick = { /*TODO*/ },
                    modifier = Modifier
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Statement")
                        Icon(painter = painterResource(id = R.drawable.download), contentDescription = null)
                    }

                }
            },
            modifier = Modifier
                .weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .padding(it)
            ) {
                Column() {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        IconButton(onClick = { /*TODO*/ }) {
                            Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
                        }
                        OutlinedTextField(
                            shape = RoundedCornerShape(10.dp),
                            value = searchText,
                            placeholder = {
                                Text(text = "Search transaction")
                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done
                            ),
                            onValueChange = onChangeSearchText,
                            trailingIcon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(Color.LightGray)
                                        .padding(5.dp)
                                        .clickable {
                                            onClearSearch()
                                        }

                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search",
                                        modifier = Modifier
                                            .size(16.dp)
                                    )
                                }

                            },
                            modifier = Modifier
                                .padding(
                                    vertical = 10.dp,
                                    horizontal = 16.dp
                                )
                                .fillMaxWidth()
                                .height(50.dp)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(onClick = { /*TODO*/ }) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.calendar),
                                    contentDescription = "Apply filtering",
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Icon(
                                    painter = painterResource(id = R.drawable.search),
                                    contentDescription = "Apply filtering",
                                    modifier = Modifier
                                        .size(20.dp)
                                )
                            }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp,
                            )
                    ) {
                        Text(
                            text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                            fontWeight = FontWeight.Bold,
//                        textAlign = TextAlign.Center,
                            modifier = Modifier

//                            .align(Alignment.CenterHorizontally)
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            painter = painterResource(id = R.drawable.calendar),
                            contentDescription = "Select date range",
                            modifier = Modifier
                                .size(20.dp)
                                .clickable {
                                    onSelectDateRange()
                                }
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    when (currentTab) {
                        TransactionScreenTab.ALL_TRANSACTIONS -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                            ) {
                                Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
                                Text(
                                    text = totalMoneyIn,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Green
                                )
                                Spacer(modifier = Modifier.weight(1f))
                                Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
                                Text(
                                    text = totalMoneyOut,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                        }
                        TransactionScreenTab.MONEY_IN -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                            ) {
                                Icon(painter = painterResource(id = R.drawable.arrow_downward), contentDescription = null)
                                Text(
                                    text = totalMoneyIn,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Green
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            onExpandSortMenu()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedSortCriteria)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.sort),
                                            contentDescription = "Sort"
                                        )
                                    }
                                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = onExpandSortMenu) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 250.dp)
                                                .padding(
                                                    horizontal = 5.dp
                                                )
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            sortTypes.forEach {
                                                DropdownMenuItem(onClick = { onSelectSortCriteria(it) }) {
                                                    Text(text = it)
                                                }
                                                Divider()
                                            }
                                        }
                                    }

                                }
                            }
                        }
                        TransactionScreenTab.MONEY_OUT -> {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                            ) {
                                Icon(painter = painterResource(id = R.drawable.arrow_upward), contentDescription = null)
                                Text(
                                    text = totalMoneyOut,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(
                                    modifier = Modifier
                                        .clickable {
                                            onExpandSortMenu()
                                        }
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(text = selectedSortCriteria)
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Icon(
                                            painter = painterResource(id = R.drawable.sort),
                                            contentDescription = "Sort"
                                        )
                                    }
                                    DropdownMenu(expanded = sortMenuExpanded, onDismissRequest = onExpandSortMenu) {
                                        Column(
                                            modifier = Modifier
                                                .heightIn(max = 250.dp)
                                                .padding(
                                                    horizontal = 5.dp
                                                )
                                                .verticalScroll(rememberScrollState())
                                        ) {
                                            sortTypes.forEach {
                                                DropdownMenuItem(onClick = { onSelectSortCriteria(it) }) {
                                                    Text(text = it)
                                                }
                                                Divider()
                                            }
                                        }
                                    }

                                }

                            }
                        }

                        TransactionScreenTab.CHART -> TODO()
                    }

//        Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp
                            )
//                .align(Alignment.End)
                    ) {

                        Column {
                            TextButton(onClick = onExpandTypeMenu) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = selectedType)
                                    Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                                }
                            }
                            DropdownMenu(expanded = typeMenuExpanded, onDismissRequest = onExpandTypeMenu) {
                                Column(
                                    modifier = Modifier
                                        .heightIn(max = 250.dp)
                                        .padding(
                                            horizontal = 5.dp
                                        )
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    transactionTypes.forEach {
                                        DropdownMenuItem(onClick = { onSelectType(it) }) {
                                            Text(text = it)
                                        }
                                        Divider()
                                    }
                                }
                            }
                        }


                    }
                    when(currentTab) {
                        TransactionScreenTab.ALL_TRANSACTIONS -> {
                            AllTransactionsScreenComposable(
                                transactions = transactions,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                                    .weight(1f)
                            )
                        }
                        TransactionScreenTab.MONEY_IN -> {
                            MoneyInScreenComposable(
                                sortedTransactionItems = moneyInsortedTransactionItems,
                                navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                                    .weight(1f)
                            )
                        }
                        TransactionScreenTab.MONEY_OUT -> {
                            MoneyOutScreenComposable(
                                sortedTransactionItems = moneyOutsortedTransactionItems,
                                navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                                modifier = Modifier
                                    .padding(
                                        horizontal = 16.dp
                                    )
                                    .weight(1f)
                            )
                        }
                        TransactionScreenTab.CHART -> {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxSize()
                            ) {
                                Text(text = "Chart")
                            }
                        }
                    }
                }
            }
        }
        BottomNavBar(
            tabItems = bottomTabItems,
            currentTab = currentTab,
            onTabSelected = onTabSelected
        )
    }
}

@Composable
private fun BottomNavBar(
    tabItems: List<TransactionScreenTabItem>,
    currentTab: TransactionScreenTab,
    onTabSelected: (TransactionScreenTab) -> Unit,
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
@Preview(showBackground = true)
@Composable
fun TransactionsScreenPreview(
    modifier: Modifier = Modifier
) {
    val tabs = listOf(
        TransactionScreenTabItem(
            name = "All",
            tab = TransactionScreenTab.ALL_TRANSACTIONS,
            icon = R.drawable.transactions
        ),
        TransactionScreenTabItem(
            name = "Money in",
            tab = TransactionScreenTab.MONEY_IN,
            icon = R.drawable.arrow_downward
        ),
        TransactionScreenTabItem(
            name = "Money out",
            tab = TransactionScreenTab.MONEY_OUT,
            icon = R.drawable.arrow_downward
        ),
        TransactionScreenTabItem(
            name = "Chart",
            tab = TransactionScreenTab.CHART,
            icon = R.drawable.chart
        ),
    )
    CashLedgerTheme {
        TransactionsScreen(
            transactions = transactions,
            moneyOutsortedTransactionItems = moneyOutSortedTransactionItems,
            moneyInsortedTransactionItems = moneyInSortedTransactionItems,
            totalMoneyIn = "Ksh 12,900",
            totalMoneyOut = "Ksh 4500",
            bottomTabItems = tabs,
            onTabSelected = {},
            currentTab = TransactionScreenTab.MONEY_OUT,
            onExpandTypeMenu = {},
            selectedType = "All types",
            typeMenuExpanded = false,
            onSelectType = {},
            onExpandSortMenu = {},
            selectedSortCriteria = "Amount",
            onSelectSortCriteria = {},
            sortMenuExpanded = false,
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            onSelectDateRange = {},
            searchText = "",
            onChangeSearchText = {},
            onClearSearch = {},
            categoryName = null,
            budgetName = null,
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyIn ->  }
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangePickerDialog(
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Popup(
        onDismissRequest = onDismiss,
        properties = PopupProperties(
            excludeFromSystemGesture = true
        ),
        content = {
            Card(
                shape = RoundedCornerShape(0.dp),
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select date range",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        modifier = Modifier
                            .padding(
                                start = 16.dp
                            )
                    )
                    DateRangePicker(
                        startDate = startDate,
                        endDate = endDate,
                        defaultStartDate = defaultStartDate,
                        defaultEndDate = defaultEndDate,
                        onChangeStartDate = onChangeStartDate,
                        onChangeLastDate = onChangeLastDate,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(
                                horizontal = 16.dp
                            )
                            .align(Alignment.End)
                    ) {
                        Text(text = "Dismiss")
                    }
                }
            }
        },
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun DateRangePicker(
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse the default start and end dates
    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }

    // Convert LocalDate to milliseconds since epoch
    val defaultStartMillis = defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis = defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selectedDate.isBefore(endDate) || selectedDate.isEqual(endDate)) {
                        onChangeStartDate(selectedDate)
                    } else {
                        // Handle case where start date is after end date
                        Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_LONG).show()
                    }
                } else {
                    if (selectedDate.isAfter(startDate) || selectedDate.isEqual(startDate)) {
                        onChangeLastDate(selectedDate)
                    } else {
                        // Handle case where end date is before start date
                        Toast.makeText(context, "End date must be after start date", Toast.LENGTH_LONG).show()
                    }
                }
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )

        // Set minimum and maximum dates
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }
        defaultEndMillis?.let { datePicker.datePicker.maxDate = it }

        datePicker.show()
    }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.onPrimary
        ),
        elevation = CardDefaults.elevatedCardElevation(10.dp),
        modifier = modifier
            .padding(16.dp)
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            IconButton(onClick = { showDatePicker(true) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null
                )
            }
            Text(text = dateFormatter.format(startDate))
            Text(text = "to")

            Text(text = dateFormatter.format(endDate))
            IconButton(onClick = { showDatePicker(false) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null
                )
            }
        }
    }
}
