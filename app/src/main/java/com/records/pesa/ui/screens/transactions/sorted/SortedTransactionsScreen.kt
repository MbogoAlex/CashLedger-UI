package com.records.pesa.ui.screens.transactions.sorted

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.DropdownMenu
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.individualSortedTransactionItems
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SortedTransactionsScreenComposable(
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = navigateToHomeScreen)
    val viewModel: SortedTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getTransactions()
        }
    )

    var showSortDropdown by rememberSaveable {
        mutableStateOf(false)
    }

    val tabs = listOf(
        SortedTransactionsTabItem(
            name = "Money in",
            icon = R.drawable.in_transactions,
            tab = SortedTransactionsTab.MONEY_IN
        ),
        SortedTransactionsTabItem(
            name = "Money out",
            icon = R.drawable.out_transactions,
            tab = SortedTransactionsTab.MONEY_OUT
        ),
    )
    
    var currentTab by rememberSaveable {
        mutableStateOf(SortedTransactionsTab.MONEY_IN)
    }
    var showDateRangePicker by rememberSaveable {
        mutableStateOf(false)
    }
    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = !showSubscriptionDialog
            },
            onConfirm = {
                showSubscriptionDialog = !showSubscriptionDialog
                navigateToSubscriptionScreen()
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SortedTransactionsScreen(
            premium = uiState.userDetails.paymentStatus,
            pullRefreshState = pullRefreshState,
            moneyInTransactions = uiState.moneyInTransactions,
            moneyOutTransactions = uiState.moneyOutTransactions,
            currentTab = currentTab,
            tabs = tabs,
            searchText = uiState.entity,
            onChangeSearchText = {
                viewModel.changeEntity(it)
            },
            onClearSearch = {
                viewModel.changeEntity("")
            },
            showDateRangePicker = showDateRangePicker,
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            onSelectDateRange = {
                showDateRangePicker = !showDateRangePicker
            },
            onChangeStartDate = {
                viewModel.changeStartDate(it)
            },
            onChangeLastDate = {
                viewModel.changeEndDate(it)
            },
            onDismiss = { showDateRangePicker = !showDateRangePicker },
            onConfirm = { showDateRangePicker = !showDateRangePicker },
            totalMoneyIn = formatMoneyValue(uiState.totalIn),
            totalMoneyOut = formatMoneyValue(uiState.totalOut),
            onTabSelected = {
                viewModel.changeTab(it)
                currentTab = it
            },
            showSortDropdown = showSortDropdown,
            omShowSortDropDown = {
                showSortDropdown = !showSortDropdown
            },
            onChangeSortCriteria = {
                viewModel.changeOrderBy(it)
            },
            sortedByAmount = uiState.orderByAmount,
            loadingStatus = uiState.loadingStatus,
            onShowSubscriptionDialog = { showSubscriptionDialog = !showSubscriptionDialog },
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyDirection ->
                navigateToEntityTransactionsScreen(uiState.userDetails.id.toString(), transactionType, entity, uiState.startDate, uiState.endDate, times, moneyDirection)
            },
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SortedTransactionsScreen(
    premium: Boolean,
    pullRefreshState: PullRefreshState?,
    moneyInTransactions: List<IndividualSortedTransactionItem>,
    moneyOutTransactions: List<IndividualSortedTransactionItem>,
    currentTab: SortedTransactionsTab,
    tabs: List<SortedTransactionsTabItem>,
    searchText: String,
    onChangeSearchText: (value: String) -> Unit,
    onClearSearch: () -> Unit,
    showDateRangePicker: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    onSelectDateRange: () -> Unit,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    totalMoneyIn: String,
    totalMoneyOut: String,
    showSortDropdown: Boolean,
    sortedByAmount: Boolean,
    omShowSortDropDown: () -> Unit,
    onChangeSortCriteria: (sortByAmount: Boolean) -> Unit,
    onTabSelected: (tab: SortedTransactionsTab) -> Unit,
    loadingStatus: LoadingStatus,
    onShowSubscriptionDialog: () -> Unit,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyDirection: String) -> Unit,
    modifier: Modifier = Modifier
) {

    var sortCriteria by rememberSaveable {
        mutableStateOf("")
    }

    val sortOptions = listOf(
        "Amount",
        "Times"
    )

    if(sortedByAmount) {
        sortCriteria = "Amount"
    } else {
        sortCriteria = "Times"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(horizontal = screenWidth(x = 16.0))
        ) {
            Text(
                text = "Sorted by ",
                fontSize = screenFontSize(x = 14.0).sp
            )

            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable {
                            omShowSortDropDown()
                        }
                ) {
                    Text(
                        text = sortCriteria,
                        color = MaterialTheme.colorScheme.surfaceTint,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Select sort criteria",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }

                DropdownMenu(
                    expanded = showSortDropdown,
                    onDismissRequest = omShowSortDropDown,
                    modifier = Modifier
                ) {
                    sortOptions.forEach {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = it,
                                    fontSize = screenFontSize(x = 14.0).sp
                                )
                            },
                            onClick = {
                                if(it == "Amount") {
                                    onChangeSortCriteria(true)
                                    omShowSortDropDown()
                                } else {
                                    onChangeSortCriteria(false)
                                    omShowSortDropDown()
                                }
                            }
                        )
                    }
                }
            }

        }
        OutlinedTextField(
            shape = RoundedCornerShape(screenWidth(x = 10.0)),
            value = searchText,
            placeholder = {
                Text(
                    text = "Search transaction",
                    fontSize = screenFontSize(x = 14.0).sp
                )
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
                        .background(MaterialTheme.colorScheme.inverseOnSurface)
                        .padding(screenWidth(x = 5.0))
                        .clickable {
                            onClearSearch()
                        }

                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search",
                        modifier = Modifier
                            .size(screenWidth(x = 16.0))
                    )
                }

            },
            modifier = Modifier
                .padding(
                    vertical = screenHeight(x = 10.0),
                    horizontal = screenWidth(x = 16.0)
                )
                .fillMaxWidth()
//                                    .height(50.dp)
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = screenWidth(x = 16.0),
                )
        ) {
            Text(
                text = "${dateFormatter.format(startDate)} to ${dateFormatter.format(endDate)}",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
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
                    .size(screenWidth(x = 20.0))
                    .clickable {
                        onSelectDateRange()
                    }
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        if(showDateRangePicker) {
            DateRangePickerDialog(
                premium = premium,
                startDate = startDate,
                endDate = endDate,
                defaultStartDate = null,
                defaultEndDate = null,
                onChangeStartDate = onChangeStartDate,
                onChangeLastDate = onChangeLastDate,
                onDismiss = onDismiss,
                onConfirm = onConfirm,
                onShowSubscriptionDialog = onShowSubscriptionDialog,
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        when (currentTab) {
            SortedTransactionsTab.MONEY_IN -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            horizontal = screenWidth(x = 16.0)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_downward),
                        contentDescription = null,
                        modifier = Modifier
                            .size(screenWidth(x = 25.0))
                    )
                    Text(
                        text = totalMoneyIn,
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.surfaceTint
                    )
                    
                }
            }
            SortedTransactionsTab.MONEY_OUT -> {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            horizontal = screenWidth(x = 16.0)
                        )
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.arrow_upward),
                        contentDescription = null,
                        modifier = Modifier
                            .size(screenWidth(x = 25.0))
                    )
                    Text(
                        text = totalMoneyOut,
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        when(currentTab) {
            SortedTransactionsTab.MONEY_IN -> {
                MoneyInSortedScreenComposable(
                    pullRefreshState = pullRefreshState!!,
                    loadingStatus = loadingStatus,
                    transactions = moneyInTransactions,
                    navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                    modifier = Modifier
                        .weight(1f)
                )
            }
            SortedTransactionsTab.MONEY_OUT -> {
                MoneyOutSortedScreenComposable(
                    pullRefreshState = pullRefreshState!!,
                    loadingStatus = loadingStatus,
                    transactions = moneyOutTransactions,
                    navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                    modifier = Modifier
                        .weight(1f)
                )
            }
            
        }
        BottomNavBar(
            tabItems = tabs,
            onTabSelected = onTabSelected,
            currentTab = currentTab
        )

    }
}

@Composable
fun DateRangePickerDialog(
    premium: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color.Red
        ),
        shape = RoundedCornerShape(0.dp),
        modifier = modifier

    ) {
        Popup(
            alignment = Alignment.TopEnd,
            properties = PopupProperties(
                excludeFromSystemGesture = true
            ),
            onDismissRequest = onDismiss,
        ) {
            Card(
                shape = RoundedCornerShape(0.dp),
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                        .fillMaxWidth()
                ) {
                    Text(
                        text = "Select date range",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 18.0).sp,
                        modifier = Modifier
                            .padding(
                                start = screenWidth(x = 16.0)
                            )
                    )
                    DateRangePicker(
                        premium = premium,
                        startDate = startDate,
                        endDate = endDate,
                        defaultStartDate = defaultStartDate,
                        defaultEndDate = defaultEndDate,
                        onChangeStartDate = onChangeStartDate,
                        onChangeLastDate = onChangeLastDate,
                        onShowSubscriptionDialog = onShowSubscriptionDialog,
                        modifier = Modifier
                            .fillMaxWidth()
                    )
                    Button(
                        onClick = onDismiss,
                        modifier = Modifier
                            .padding(
                                horizontal = screenWidth(x = 16.0)
                            )
                            .align(Alignment.End)
                    ) {
                        Text(text = "Dismiss")
                    }
                }
            }
        }
    }

}

@Composable
fun DateRangePicker(
    premium: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // Parse the default start and end dates
    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }

    // Convert LocalDate to milliseconds since epoch
    val defaultStartMillis = defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis = defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    val oneMonthAgo = LocalDateTime.now().minusMonths(1)

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selectedDate.isBefore(endDate) || selectedDate.isEqual(endDate)) {
                        if(selectedDate.isBefore(oneMonthAgo.toLocalDate())) {
                            if(premium) {
                                onChangeStartDate(selectedDate)
                            } else {
                                onShowSubscriptionDialog()
                            }
                        } else {
                            onChangeStartDate(selectedDate)
                        }
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
        elevation = CardDefaults.elevatedCardElevation(screenWidth(x = 10.0)),
        modifier = modifier
            .padding(screenWidth(x = 16.0))
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
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
            Text(
                text = dateFormatter.format(startDate),
                fontSize = screenFontSize(x = 14.0).sp
            )
            Text(
                text = "to",
                fontSize = screenFontSize(x = 14.0).sp
            )

            Text(
                text = dateFormatter.format(endDate),
                fontSize = screenFontSize(x = 14.0).sp
            )
            IconButton(onClick = { showDatePicker(false) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
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
                    Text(text = "1. See transactions and export reports of more than one months")
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


@Composable
private fun BottomNavBar(
    tabItems: List<SortedTransactionsTabItem>,
    currentTab: SortedTransactionsTab,
    onTabSelected: (SortedTransactionsTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar {
        for(item in tabItems) {
            NavigationBarItem(
                label = {
                    Text(
                        text = item.name,
                        fontSize = screenFontSize(x = 14.0).sp,
                    )
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

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SortedTransactionsScreenPreview() {
    val tabs = listOf(
        SortedTransactionsTabItem(
            name = "Money in",
            icon = R.drawable.in_transactions,
            tab = SortedTransactionsTab.MONEY_IN
        ),
        SortedTransactionsTabItem(
            name = "Money out",
            icon = R.drawable.out_transactions,
            tab = SortedTransactionsTab.MONEY_OUT
        ),
    )
    CashLedgerTheme {
        SortedTransactionsScreen(
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            premium = false,
            moneyInTransactions = individualSortedTransactionItems,
            moneyOutTransactions = individualSortedTransactionItems,
            currentTab = SortedTransactionsTab.MONEY_IN,
            tabs = tabs,
            searchText = "",
            onChangeSearchText = {},
            onClearSearch = { /*TODO*/ },
            showDateRangePicker = false,
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7),
            onSelectDateRange = { /*TODO*/ },
            onChangeStartDate = {},
            onChangeLastDate = {},
            onDismiss = { /*TODO*/ },
            onConfirm = { /*TODO*/ },
            totalMoneyIn = formatMoneyValue(3200.0),
            totalMoneyOut = formatMoneyValue(3456.0),
            onTabSelected = {},
            showSortDropdown = false,
            omShowSortDropDown = {},
            onChangeSortCriteria = {},
            sortedByAmount = false,
            onShowSubscriptionDialog = { /*TODO*/ },
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyDirection ->  }
        )
    }
}