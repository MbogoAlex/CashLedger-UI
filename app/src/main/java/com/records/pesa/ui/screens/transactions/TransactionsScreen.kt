package com.records.pesa.ui.screens.transactions

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManagerFactory
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.TransactionScreenTabItem
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.moneyInSortedTransactionItems
import com.records.pesa.reusables.moneyOutSortedTransactionItems
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

object TransactionsScreenDestination: AppNavigation {
    override val title = "Transactions Screen"
    override val route = "transactions-screen"
    val categoryId: String = "categoryId"
    val budgetId: String = "budgetId"
    val categoryName: String = "categoryName"
    val budgetName: String = "budgetName"
    val startDate: String = "startDate"
    val endDate: String = "endDate"
    val transactionType: String = "transactionType"
    val moneyDirection: String = "moneyDirection"
    val comment: String = "Comment"
    val routeWithCategoryId: String = "$route/{$categoryId}"
    val routeWithBudgetId: String = "$route/{$categoryId}/{$budgetId}/{$startDate}/{$endDate}"
    val routeWithTransactionType: String = "$route/{$comment}/{$transactionType}/{$moneyDirection}/{$startDate}/{$endDate}"
}

@OptIn(ExperimentalPermissionsApi::class, ExperimentalMaterialApi::class)
@Composable
fun TransactionsScreenComposable(
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    showBackArrow: Boolean,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: TransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.errorCode == 401 && uiState.loadingStatus == LoadingStatus.FAIL) {
        navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.password)
        viewModel.resetLoadingStatus()
    }

    BackHandler(onBack = {
        if(showBackArrow) {
            navigateToPreviousScreen()
        } else {
            navigateToHomeScreen()
        }
    })

    val reviewManager = remember {
        ReviewManagerFactory.create(context)
    }

    var reviewInfo: ReviewInfo? by remember {
        mutableStateOf(null)
    }

    reviewManager.requestReviewFlow().addOnCompleteListener {
        if(it.isSuccessful) {
            reviewInfo = it.result
        } else {
            Log.d("TransactionsScreen", "requestReviewFlow failed, ${it.exception}")
        }
    }

    val tabs = listOf(
        TransactionScreenTabItem(
            name = "All",
            tab = TransactionScreenTab.ALL_TRANSACTIONS,
            icon = R.drawable.list
        ),
        TransactionScreenTabItem(
            name = "Grouped",
            tab = TransactionScreenTab.GROUPED,
            icon = R.drawable.grouped
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

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showDownloadReportDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var reportType by rememberSaveable {
        mutableStateOf("PDF")
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            if(currentTab == TransactionScreenTab.ALL_TRANSACTIONS) {
                viewModel.getTransactions()
            } else if(currentTab == TransactionScreenTab.GROUPED) {
                viewModel.getGroupedByEntityTransactions()
            }

        }
    )

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.fetchReportAndSave(
                context = context,
                saveUri = it,
                reportType = reportType
            )
        }
    }

    var shouldPromptForRating by rememberSaveable {
        mutableStateOf(false)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycle = lifecycleOwner.lifecycle

    LaunchedEffect(lifecycle) {
        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                Log.d("TransactionsScreen", "onResume, shouldPromptForRating: $shouldPromptForRating")
                Log.d("TransactionsScreen", reviewInfo.toString())
                if (shouldPromptForRating && reviewInfo != null) {
                    Log.d("TransactionsScreen", "reviewInfo not null")
                    reviewManager.launchReviewFlow(context as Activity, reviewInfo!!)
                    shouldPromptForRating = false // Reset the flag after showing the review
                }
            }
        })
    }

    if(uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri

        if(reportType == "PDF") {
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            shouldPromptForRating = true

            context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))
        } else if(reportType == "CSV") {
            val csvIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }

            shouldPromptForRating = true

            context.startActivity(Intent.createChooser(csvIntent, "Open CSV with:"))
        }

    } else if(uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
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

    if(showDownloadReportDialog) {
        DownloadReportDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = {
                showDownloadReportDialog = !showDownloadReportDialog
            },
            onConfirm = { type ->
                reportType = type
                showDownloadReportDialog = !showDownloadReportDialog
                if(reportType == "PDF") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.pdf")
                } else if(reportType == "CSV") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.csv")
                }
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        TransactionsScreen(
            premium = uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0179189199",
            transactions = uiState.transactions,
            groupedTransactionItems = uiState.groupedTransactionItems,
            moneyOutsortedTransactionItems = uiState.moneyOutSorted,
            totalMoneyIn = formatMoneyValue(uiState.totalMoneyIn),
            totalMoneyOut = formatMoneyValue(uiState.totalMoneyOut),
            bottomTabItems = tabs,
            defaultTransactionType = uiState.defaultTransactionType,
            transactionTypes = uiState.selectableTransactionTypes,
            currentTab = currentTab,
            onTabSelected = {
                transactionsLoaded = false
                currentTab = it
                if(it == TransactionScreenTab.GROUPED && !transactionsLoaded) {
                    transactionsLoaded = true
                    viewModel.getGroupedByEntityTransactions()
                } else if(it == TransactionScreenTab.ALL_TRANSACTIONS && !transactionsLoaded) {
                    transactionsLoaded = true
                    viewModel.getTransactions()
                }
            },
            typeMenuExpanded = typeMenuExpanded,
            showDateRangePicker = showDateRangePicker,
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
            },
            searchText = uiState.entity,
            onChangeSearchText = { searchText ->
                viewModel.changeEntity(searchText, currentTab)
            },
            onClearSearch = {
                viewModel.clearSearch(currentTab)
            },
            onSelectDateRange = {
                showDateRangePicker = !showDateRangePicker
            },
            navigateToEntityTransactionsScreen = {transactionType, entity, times ->
                Log.d("MONEY_DIRECTION", uiState.moneyDirection.toString())
                navigateToEntityTransactionsScreen(uiState.userDetails.id.toString(), transactionType, entity, uiState.startDate, uiState.endDate, times, if(uiState.moneyDirection == null || uiState.moneyDirection == "null") "all" else uiState.moneyDirection!!)
            },
            categoryName = uiState.categoryName,
            budgetName = uiState.budgetName,
            navigateToPreviousScreen = navigateToPreviousScreen,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = !showSubscriptionDialog
            },
            showBackArrow = showBackArrow,
            onDownloadReport = {
                showDownloadReportDialog = !showDownloadReportDialog
            },
            downloadingStatus = uiState.downloadingStatus,
            loadingStatus = uiState.loadingStatus,
            pullRefreshState = pullRefreshState,
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen
        )

    }


}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun TransactionsScreen(
    premium: Boolean,
    transactions: List<TransactionItem>,
    groupedTransactionItems: List<SortedTransactionItem>,
    moneyOutsortedTransactionItems: List<SortedTransactionItem>,
    transactionTypes: List<String>,
    defaultTransactionType: String?,
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
    searchText: String,
    categoryName: String?,
    budgetName: String?,
    showDateRangePicker: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    onChangeSearchText: (searchText: String) -> Unit,
    onClearSearch: () -> Unit,
    onSelectDateRange: () -> Unit,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    showBackArrow: Boolean,
    onDownloadReport: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    downloadingStatus: DownloadingStatus,
    loadingStatus: LoadingStatus,
    pullRefreshState: PullRefreshState?,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        Scaffold(
            backgroundColor = MaterialTheme.colorScheme.background,
//            floatingActionButton = {
//                Button(
//                    onClick = { /*TODO*/ },
//                    modifier = Modifier
//                ) {
//                    Row(
//                        verticalAlignment = Alignment.CenterVertically
//                    ) {
//                        Text(text = "Statement")
//                        Icon(painter = painterResource(id = R.drawable.download), contentDescription = null)
//                    }
//
//                }
//            },
            modifier = Modifier
                .weight(1f)
        ) {
            Column(
                modifier = Modifier
                    .padding(it)
            ) {
                if(showBackArrow) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                    ) {
                        IconButton(
                            onClick = navigateToPreviousScreen
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Previous screen",
                                modifier = Modifier
                                    .size(screenWidth(x = 25.0))
                            )
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
                    }
                } else {
                    OutlinedTextField(
                        shape = RoundedCornerShape(screenWidth(x = 10.0)),
                        value = searchText,
                        placeholder = {
                            Text(
                                text = "Search transaction",
                                fontSize = screenFontSize(x = 14.0).sp,
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
                                vertical = screenHeight(x = 16.0),
                                horizontal = screenWidth(x = 16.0)
                            )
                            .fillMaxWidth()
//                                .height(50.dp)
                    )
                }

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
                        defaultStartDate = defaultStartDate,
                        defaultEndDate = defaultEndDate,
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
                    TransactionScreenTab.ALL_TRANSACTIONS -> {
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
                            Spacer(modifier = Modifier.weight(1f))
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
                    TransactionScreenTab.GROUPED -> {
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
                            Spacer(modifier = Modifier.weight(1f))
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

//        Spacer(modifier = Modifier.height(5.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(
                            horizontal = screenWidth(x = 16.0)
                        )
//                .align(Alignment.End)
                ) {

                    Column {
                        TextButton(onClick = onExpandTypeMenu) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = defaultTransactionType ?: selectedType,
                                    fontSize = screenFontSize(x = 14.0).sp,
                                )
                                Icon(imageVector = Icons.Default.KeyboardArrowDown, contentDescription = null)
                            }
                        }
                        DropdownMenu(expanded = typeMenuExpanded && defaultTransactionType == null, onDismissRequest = onExpandTypeMenu) {
                            Column(
                                modifier = Modifier
                                    .heightIn(max = screenHeight(x = 250.0))
                                    .padding(
                                        horizontal = screenWidth(x = 5.0)
                                    )
                                    .verticalScroll(rememberScrollState())
                            ) {
                                transactionTypes.forEach {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = it,
                                                fontSize = screenFontSize(x = 14.0).sp,
                                            )
                                        },
                                        onClick = { onSelectType(it) },
                                        modifier = Modifier
                                    )
                                    Divider()
                                }
                            }
                        }
                    }
                    if(currentTab == TransactionScreenTab.ALL_TRANSACTIONS) {
                        Spacer(modifier = Modifier.weight(1f))
                        TextButton(
                            enabled = downloadingStatus != DownloadingStatus.LOADING,
                            onClick = onDownloadReport
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Statement",
                                    fontSize = screenFontSize(x = 14.0).sp,
                                )
                                if(downloadingStatus == DownloadingStatus.LOADING) {
                                    CircularProgressIndicator(
                                        modifier = Modifier
                                            .size(screenWidth(x = 15.0))
                                    )
                                } else {
                                    Icon(
                                        painter = painterResource(id = R.drawable.download),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .width(screenWidth(x = 24.0))
                                    )
                                }

                            }
                        }
                    }

                }
                when(currentTab) {
                    TransactionScreenTab.ALL_TRANSACTIONS -> {
                        AllTransactionsScreenComposable(
                            pullRefreshState = pullRefreshState!!,
                            transactions = transactions,
                            loadingStatus = loadingStatus,
                            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                            modifier = Modifier
                                .padding(
                                    horizontal = screenWidth(x = 16.0)
                                )
                                .weight(1f)
                        )
                    }
                    TransactionScreenTab.GROUPED -> {
                        GroupedTransactionsScreenComposable(
                            pullRefreshState = pullRefreshState!!,
                            loadingStatus = loadingStatus,
                            groupedTransactionItems = groupedTransactionItems,
                            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                            modifier = Modifier
                                .padding(
                                    horizontal = screenWidth(x = 16.0)
                                )
                                .weight(1f)
                        )
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

@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Go premium?",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold
            )
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
                        text = "Ksh50.0 premium monthly fee",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. See transactions and export reports of more than one months",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "2. Backup your transactions",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "3. Manage more than one category",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "4. Use in dark mode",
                        fontSize = screenFontSize(x = 14.0).sp
                    )

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Subscribe",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
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
            name = "Grouped",
            tab = TransactionScreenTab.GROUPED,
            icon = R.drawable.arrow_downward
        ),
    )
    CashLedgerTheme {
        TransactionsScreen(
            premium = false,
            transactions = transactions,
            moneyOutsortedTransactionItems = moneyOutSortedTransactionItems,
            groupedTransactionItems = moneyInSortedTransactionItems,
            totalMoneyIn = "Ksh 12,900",
            totalMoneyOut = "Ksh 4500",
            bottomTabItems = tabs,
            onTabSelected = {},
            transactionTypes = emptyList(),
            defaultTransactionType = null,
            currentTab = TransactionScreenTab.ALL_TRANSACTIONS,
            onExpandTypeMenu = {},
            selectedType = "All types",
            typeMenuExpanded = false,
            onSelectType = {},
            onExpandSortMenu = {},
            selectedSortCriteria = "Amount",
            onSelectSortCriteria = {},
            sortMenuExpanded = false,
            showDateRangePicker = false,
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            defaultStartDate = "2024-06-01",
            defaultEndDate = "2024-06-25",
            onChangeStartDate ={},
            onChangeLastDate = {},
            onDismiss = {},
            onConfirm = {},
            onSelectDateRange = {},
            searchText = "",
            onChangeSearchText = {},
            onClearSearch = {},
            categoryName = null,
            budgetName = null,
            navigateToEntityTransactionsScreen = {transactionType, entity, times ->  },
            navigateToPreviousScreen = {},
            onShowSubscriptionDialog = {},
            showBackArrow = true,
            onDownloadReport = {},
            downloadingStatus = DownloadingStatus.INITIAL,
            pullRefreshState = null,
            navigateToTransactionDetailsScreen = {},
            loadingStatus = LoadingStatus.INITIAL
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
                    if(defaultStartDate != null) {
                        Text(
                            text = "Select date range (within $defaultStartDate and $defaultEndDate)",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 18.0).sp,
                            modifier = Modifier
                                .padding(
                                    start = screenWidth(x = 16.0)
                                )
                        )
                    } else {
                        Text(
                            text = "Select date range",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 18.0).sp,
                            modifier = Modifier
                                .padding(
                                    start = screenWidth(x = 16.0)
                                )
                        )
                    }

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
private  fun DownloadReportDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onConfirm: (type: String) -> Unit,
) {
    val types = listOf("PDF", "CSV")
    var selectedType by rememberSaveable {
        mutableStateOf("PDF")
    }

    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        title = {
            Text(
                text = "Report for $startDate to $endDate",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Row(
//                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Export to ",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                expanded = !expanded
                            }
                    ) {
                        Text(
                            text = selectedType,
                            color = MaterialTheme.colorScheme.surfaceTint,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select report type",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {expanded = !expanded},
                        modifier = Modifier
                    ) {
                        types.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = it,
                                        fontSize = screenFontSize(x = 14.0).sp
                                    )
                                },
                                onClick = {
                                    selectedType = it
                                    expanded = !expanded
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }

        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType) }) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
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
