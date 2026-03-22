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
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
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
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.TransactionScreenTabItem
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import kotlin.math.absoluteValue

object TransactionsScreenDestination : AppNavigation {
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

// ─── Avatar palette ───────────────────────────────────────────────────────────

private val txAvatarPalette = listOf(
    Color(0xFF006A65), Color(0xFF4A6361), Color(0xFF48607B),
    Color(0xFF7B5EA7), Color(0xFFB5542D), Color(0xFF2D6A8A),
    Color(0xFF5A7A2B), Color(0xFF8A3D4A),
)
private fun txAvatarColor(name: String): Color =
    txAvatarPalette[(name.firstOrNull()?.code ?: 0) % txAvatarPalette.size]

// ─── Composable entry point ───────────────────────────────────────────────────

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

    if (uiState.errorCode == 401 && uiState.loadingStatus == LoadingStatus.FAIL) {
        navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.password)
        viewModel.resetLoadingStatus()
    }

    BackHandler(onBack = {
        if (showBackArrow) navigateToPreviousScreen() else navigateToHomeScreen()
    })

    val reviewManager = remember { ReviewManagerFactory.create(context) }
    var reviewInfo: ReviewInfo? by remember { mutableStateOf(null) }
    reviewManager.requestReviewFlow().addOnCompleteListener {
        if (it.isSuccessful) reviewInfo = it.result
    }

    val tabs = listOf(
        TransactionScreenTabItem("All", R.drawable.list, TransactionScreenTab.ALL_TRANSACTIONS),
        TransactionScreenTabItem("By Contact", R.drawable.grouped, TransactionScreenTab.GROUPED),
    )

    var currentTab by rememberSaveable { mutableStateOf(TransactionScreenTab.ALL_TRANSACTIONS) }
    var selectedPeriod by remember { mutableStateOf<TimePeriod>(TimePeriod.THIS_MONTH) }
    var transactionsLoaded by rememberSaveable { mutableStateOf(false) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }
    var showDownloadReportDialog by rememberSaveable { mutableStateOf(false) }
    var reportType by rememberSaveable { mutableStateOf("PDF") }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            if (currentTab == TransactionScreenTab.ALL_TRANSACTIONS) viewModel.getTransactions()
            else viewModel.getGroupedByEntityTransactions()
        }
    )

    val createDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
            uri?.let { viewModel.fetchReportAndSave(context, it, reportType) }
        }

    var shouldPromptForRating by rememberSaveable { mutableStateOf(false) }
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner.lifecycle) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (shouldPromptForRating && reviewInfo != null) {
                    reviewManager.launchReviewFlow(context as Activity, reviewInfo!!)
                    shouldPromptForRating = false
                }
            }
        })
    }

    if (uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        val mimeType = if (reportType == "PDF") "application/pdf" else "text/csv"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        shouldPromptForRating = true
        context.startActivity(Intent.createChooser(intent, "Open with:"))
    } else if (uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = { showSubscriptionDialog = false; navigateToSubscriptionScreen() }
        )
    }
    if (showDownloadReportDialog) {
        DownloadReportDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = { showDownloadReportDialog = false },
            onConfirm = { type ->
                reportType = type
                showDownloadReportDialog = false
                val fileName = "MPESA-Transactions_${LocalDateTime.now()}"
                if (type == "PDF") createDocumentLauncher.launch("$fileName.pdf")
                else createDocumentLauncher.launch("$fileName.csv")
            }
        )
    }

    TransactionsScreen(
        premium = uiState.preferences.paid || uiState.userDetails.phoneNumber == "0888888888" || uiState.preferences.permanent,
        transactions = uiState.transactions,
        groupedTransactionItems = uiState.groupedTransactionItems,
        totalMoneyInRaw = uiState.totalMoneyIn,
        totalMoneyOutRaw = uiState.totalMoneyOut,
        transactionTypes = uiState.selectableTransactionTypes,
        defaultTransactionType = uiState.defaultTransactionType,
        currentTab = currentTab,
        onTabSelected = { tab ->
            transactionsLoaded = false
            currentTab = tab
            if (!transactionsLoaded) {
                transactionsLoaded = true
                if (tab == TransactionScreenTab.GROUPED) viewModel.getGroupedByEntityTransactions()
                else viewModel.getTransactions()
            }
        },
        selectedType = uiState.transactionType,
        onSelectType = { viewModel.changeTransactionType(it, currentTab) },
        startDate = LocalDate.parse(uiState.startDate),
        endDate = LocalDate.parse(uiState.endDate),
        defaultStartDate = uiState.defaultStartDate,
        defaultEndDate = uiState.defaultEndDate,
        onChangeStartDate = { viewModel.changeStartDate(it, currentTab) },
        onChangeLastDate = { viewModel.changeEndDate(it, currentTab) },
        selectedPeriod = selectedPeriod,
        onPeriodSelected = { period ->
            selectedPeriod = period
            val (start, end) = period.getDateRange()
            viewModel.changeStartDate(start, currentTab)
            viewModel.changeEndDate(end, currentTab)
        },
        showDateRangePicker = showDateRangePicker,
        onToggleDatePicker = { showDateRangePicker = !showDateRangePicker },
        onDismissDatePicker = { showDateRangePicker = false },
        searchText = uiState.entity,
        onChangeSearchText = { viewModel.changeEntity(it, currentTab) },
        onClearSearch = { viewModel.clearSearch(currentTab) },
        categoryName = uiState.categoryName,
        budgetName = uiState.budgetName,
        navigateToEntityTransactionsScreen = { transactionType, entity, times ->
            Log.d("SingleEntityTransactions", "type:: $transactionType, entity:: $entity, times:: $times")
            navigateToEntityTransactionsScreen(
                uiState.userDetails.id.toString(),
                transactionType, entity,
                uiState.startDate, uiState.endDate,
                times,
                if (uiState.moneyDirection == null || uiState.moneyDirection == "null") "all" else uiState.moneyDirection!!
            )
        },
        navigateToPreviousScreen = navigateToPreviousScreen,
        onShowSubscriptionDialog = { showSubscriptionDialog = true },
        showBackArrow = showBackArrow,
        onDownloadReport = { showDownloadReportDialog = true },
        downloadingStatus = uiState.downloadingStatus,
        loadingStatus = uiState.loadingStatus,
        pullRefreshState = pullRefreshState,
        navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
        modifier = modifier
    )
}

// ─── Screen ───────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterialApi::class, ExperimentalFoundationApi::class)
@Composable
fun TransactionsScreen(
    premium: Boolean,
    transactions: List<TransactionItem>,
    groupedTransactionItems: List<SortedTransactionItem>,
    totalMoneyInRaw: Double,
    totalMoneyOutRaw: Double,
    transactionTypes: List<String>,
    defaultTransactionType: String?,
    currentTab: TransactionScreenTab,
    onTabSelected: (TransactionScreenTab) -> Unit,
    selectedType: String,
    onSelectType: (String) -> Unit,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (LocalDate) -> Unit,
    onChangeLastDate: (LocalDate) -> Unit,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    showDateRangePicker: Boolean,
    onToggleDatePicker: () -> Unit,
    onDismissDatePicker: () -> Unit,
    searchText: String,
    onChangeSearchText: (String) -> Unit,
    onClearSearch: () -> Unit,
    categoryName: String?,
    budgetName: String?,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    showBackArrow: Boolean,
    onDownloadReport: () -> Unit,
    downloadingStatus: DownloadingStatus,
    loadingStatus: LoadingStatus,
    pullRefreshState: PullRefreshState?,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val net = totalMoneyInRaw - totalMoneyOutRaw
    val isLoading = loadingStatus == LoadingStatus.LOADING
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else { keyboard?.hide(); onClearSearch() }
    }

    val groupedByDate = remember(transactions) {
        transactions.groupBy { it.date }
            .entries.sortedByDescending { it.key }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top app bar ───────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showBackArrow || !showSearch) {
                        IconButton(onClick = {
                            if (showSearch) { showSearch = false }
                            else navigateToPreviousScreen()
                        }) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_right),
                                contentDescription = "Back",
                                modifier = Modifier
                                    .size(22.dp)
                                    .scale(scaleX = -1f, scaleY = 1f)
                            )
                        }
                    }

                    if (showSearch) {
                        OutlinedTextField(
                            value = searchText,
                            onValueChange = onChangeSearchText,
                            placeholder = {
                                Text(
                                    "Search by name, type…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSearch = false }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Close search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(focusRequester)
                        )
                    } else {
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = categoryName ?: budgetName ?: "Transactions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if ((categoryName != null || budgetName != null)) {
                                Text(
                                    text = "filtered view",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    // Search icon
                    if (!showSearch) {
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Download / Statement
                    if (currentTab == TransactionScreenTab.ALL_TRANSACTIONS) {
                        IconButton(
                            onClick = onDownloadReport,
                            enabled = downloadingStatus != DownloadingStatus.LOADING
                        ) {
                            if (downloadingStatus == DownloadingStatus.LOADING) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = "Download statement",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // ── Scrollable body ───────────────────────────────────────────────
            val lazyListState = rememberLazyListState()

            // Track current date section: update whenever firstVisibleItemIndex changes.
            // Since stickyDate persists, it stays set while scrolling through a section
            // that has no header in view.
            var stickyDate by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(lazyListState.firstVisibleItemIndex) {
                if (currentTab == TransactionScreenTab.ALL_TRANSACTIONS) {
                    val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                    val dateHeaders = visibleItems.filter {
                        (it.key as? String)?.startsWith("header_") == true
                    }
                    val onScreenHeader = dateHeaders.firstOrNull { it.offset >= 0 }
                    if (onScreenHeader != null) {
                        // Date header is in view — no need for the banner
                        stickyDate = null
                    } else {
                        // Header scrolled off — show last known date in banner
                        dateHeaders.maxByOrNull { it.index }?.let {
                            stickyDate = (it.key as? String)?.removePrefix("header_")
                        }
                    }
                }
            }

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Hero summary card
                    item {
                        TxHeroCard(
                            totalIn = totalMoneyInRaw,
                            totalOut = totalMoneyOutRaw,
                            net = net,
                            txCount = if (currentTab == TransactionScreenTab.ALL_TRANSACTIONS)
                                transactions.size else groupedTransactionItems.size,
                            startDate = startDate,
                            endDate = endDate,
                            selectedPeriod = selectedPeriod,
                            onPeriodSelected = onPeriodSelected,
                            onOpenCustomPicker = onToggleDatePicker,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                        )
                    }

                    // Date picker
                    if (showDateRangePicker) {
                        item {
                            DateRangePickerDialog(
                                premium = premium,
                                startDate = startDate,
                                endDate = endDate,
                                defaultStartDate = defaultStartDate,
                                defaultEndDate = defaultEndDate,
                                onChangeStartDate = onChangeStartDate,
                                onChangeLastDate = onChangeLastDate,
                                onDismiss = onDismissDatePicker,
                                onConfirm = onDismissDatePicker,
                                onShowSubscriptionDialog = onShowSubscriptionDialog,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Sticky tab + filter row (always one sticky header — stacks date below tabs)
                    stickyHeader {
                        TxTabAndFilterRow(
                            currentTab = currentTab,
                            onTabSelected = onTabSelected,
                            selectedType = selectedType,
                            defaultTransactionType = defaultTransactionType,
                            transactionTypes = transactionTypes,
                            onSelectType = onSelectType,
                            currentSectionDate = if (currentTab == TransactionScreenTab.ALL_TRANSACTIONS) stickyDate else null
                        )
                    }

                    // ── Content ───────────────────────────────────────────────
                    when (currentTab) {
                        TransactionScreenTab.ALL_TRANSACTIONS -> {
                            if (!isLoading) {
                                if (transactions.isEmpty()) {
                                    item {
                                        TxEmptyState(
                                            message = if (searchText.isNotEmpty())
                                                "No transactions matching \"$searchText\""
                                            else "No transactions for this period"
                                        )
                                    }
                                } else {
                                    groupedByDate.forEach { (date, txsForDate) ->
                                        item(key = "header_$date") {
                                            TxDateHeader(date = date)
                                        }
                                        items(txsForDate, key = { it.transactionId ?: it.transactionCode }) { tx ->
                                            TxItemRow(
                                                transaction = tx,
                                                onClick = { navigateToTransactionDetailsScreen(tx.transactionId.toString()) },
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp)
                                            )
                                            HorizontalDivider(
                                                modifier = Modifier.padding(horizontal = 16.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        TransactionScreenTab.GROUPED -> {
                            if (!isLoading) {
                                if (groupedTransactionItems.isEmpty()) {
                                    item { TxEmptyState("No grouped data for this period") }
                                } else {
                                    items(groupedTransactionItems, key = { it.entity }) { item ->
                                        SortedTxItemRow(
                                            item = item,
                                            onClick = {
                                                navigateToEntityTransactionsScreen(
                                                    item.transactionType,
                                                    Uri.encode(item.entity),
                                                    item.times.toString()
                                                )
                                            },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp)
                                        )
                                        HorizontalDivider(
                                            modifier = Modifier.padding(horizontal = 16.dp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Pull-to-refresh indicator
                if (pullRefreshState != null) {
                    PullRefreshIndicator(
                        refreshing = isLoading,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }

                // Full-screen loading
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero summary card ────────────────────────────────────────────────────────

@Composable
private fun TxHeroCard(
    totalIn: Double,
    totalOut: Double,
    net: Double,
    txCount: Int,
    startDate: LocalDate,
    endDate: LocalDate,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    onOpenCustomPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val periodOptions = remember {
        listOf(
            TimePeriod.TODAY, TimePeriod.YESTERDAY,
            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
            TimePeriod.THIS_YEAR
        )
    }
    var showPeriodMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), spotColor = primaryColor.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            primaryColor.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                // Period chip row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primaryColor.copy(alpha = 0.10f))
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { showPeriodMenu = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = selectedPeriod.getDisplayName().uppercase(),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = primaryColor,
                                letterSpacing = 1.sp
                            )
                            Icon(
                                painter = painterResource(R.drawable.arrow_downward),
                                contentDescription = "Select period",
                                tint = primaryColor,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            periodOptions.forEach { period ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = period.getDisplayName(),
                                            fontSize = 14.sp,
                                            fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                                            color = if (period == selectedPeriod) primaryColor else MaterialTheme.colorScheme.onSurface
                                        )
                                    },
                                    onClick = {
                                        onPeriodSelected(period)
                                        showPeriodMenu = false
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.calendar),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp),
                                            tint = primaryColor
                                        )
                                        Text(
                                            text = "Custom",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = primaryColor
                                        )
                                    }
                                },
                                onClick = {
                                    showPeriodMenu = false
                                    onOpenCustomPicker()
                                }
                            )
                        }
                    }

                    // Date range label + count
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${dateFormatter.format(startDate)} – ${dateFormatter.format(endDate)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$txCount txn${if (txCount != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Net flow
                Text(
                    text = "Net Flow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.2f", net.absoluteValue)}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                Spacer(modifier = Modifier.height(12.dp))

                // In / Out / Net row
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    HeroStatCol(label = "Money In", value = "Ksh ${String.format("%,.2f", totalIn)}", color = MaterialTheme.colorScheme.tertiary)
                    HeroStatCol(label = "Money Out", value = "Ksh ${String.format("%,.2f", totalOut)}", color = MaterialTheme.colorScheme.error)
                    HeroStatCol(
                        label = "Net",
                        value = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.2f", net.absoluteValue)}",
                        color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroStatCol(label: String, value: String, color: Color) {
    Column {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

// ─── Tab + filter sticky row ──────────────────────────────────────────────────

@Composable
private fun TxTabAndFilterRow(
    currentTab: TransactionScreenTab,
    onTabSelected: (TransactionScreenTab) -> Unit,
    selectedType: String,
    defaultTransactionType: String?,
    transactionTypes: List<String>,
    onSelectType: (String) -> Unit,
    currentSectionDate: String? = null
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Tab pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(
                    TransactionScreenTab.ALL_TRANSACTIONS to "All Transactions",
                    TransactionScreenTab.GROUPED to "By Contact"
                ).forEach { (tab, label) ->
                    val selected = currentTab == tab
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        animationSpec = tween(200),
                        label = "tabColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "tabTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onTabSelected(tab) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(label, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal, color = textColor)
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                // Type filter chip
                TxTypeFilterChip(
                    selected = selectedType,
                    defaultType = defaultTransactionType,
                    types = transactionTypes,
                    onSelect = onSelectType
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))
            // Date section banner — only shown when header has scrolled off screen
            if (currentSectionDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.calendar),
                        contentDescription = null,
                        modifier = Modifier.size(11.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Text(
                        text = formatTxDateHeader(currentSectionDate),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                        letterSpacing = 0.3.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun TxTypeFilterChip(
    selected: String,
    defaultType: String?,
    types: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val displayType = defaultType ?: selected
    val isFiltered = displayType != "All types"

    Box {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                    if (isFiltered) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
                )
                .clickable(
                    enabled = defaultType == null,
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) { expanded = true }
                .padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = if (displayType.length > 16) "${displayType.take(14)}…" else displayType,
                fontSize = 12.sp,
                fontWeight = if (isFiltered) FontWeight.SemiBold else FontWeight.Normal,
                color = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (defaultType == null) {
                Icon(
                    painter = painterResource(R.drawable.arrow_downward),
                    contentDescription = null,
                    modifier = Modifier.size(11.dp),
                    tint = if (isFiltered) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            Column(
                modifier = Modifier
                    .heightIn(max = 250.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                types.forEach { type ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = type,
                                fontSize = 14.sp,
                                fontWeight = if (type == selected) FontWeight.Bold else FontWeight.Normal,
                                color = if (type == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                        },
                        onClick = { onSelect(type); expanded = false }
                    )
                }
            }
        }
    }
}

// ─── Date header ─────────────────────────────────────────────────────────────

private fun formatTxShortDate(dateStr: String): String = try {
    LocalDate.parse(dateStr).format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
} catch (e: Exception) { dateStr }

private fun formatTxShortTime(timeStr: String): String = try {
    val t = java.time.LocalTime.parse(timeStr)
    t.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))
} catch (e: Exception) { timeStr }

private fun formatTxDateHeader(dateStr: String): String {
    return try {
        val date = LocalDate.parse(dateStr)
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(java.time.format.DateTimeFormatter.ofPattern("EEEE, d MMM yyyy"))
        }
    } catch (e: Exception) {
        dateStr
    }
}

@Composable
private fun TxDateHeader(date: String) {
    Text(
        text = formatTxDateHeader(date),
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.75f),
        letterSpacing = 0.4.sp,
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

// ─── Transaction item row ─────────────────────────────────────────────────────

@Composable
private fun TxItemRow(
    transaction: TransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isIn = transaction.transactionAmount > 0
    val displayName = (transaction.nickName?.ifEmpty { null } ?: transaction.entity)
        .replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(transaction.entity)
    val amountColor = if (isIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
    val hasFee = !isIn && transaction.transactionCost.absoluteValue > 0

    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar with subtle colored ring
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
            }
        }

        // Middle
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = transaction.transactionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "· ${formatTxShortDate(transaction.date)}  ${formatTxShortTime(transaction.time)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }

        // Right: amount + optional fee chip + direction indicator
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
            // Direction dot + amount
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(amountColor)
                )
                Text(
                    text = "${if (isIn) "+" else "-"}Ksh ${String.format("%,.0f", transaction.transactionAmount.absoluteValue)}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = amountColor
                )
            }
            if (hasFee) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.35f))
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Text(
                        text = "Fee",
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                    )
                    Text(
                        text = "Ksh ${String.format("%,.2f", transaction.transactionCost.absoluteValue)}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

// ─── Sorted / grouped item row ────────────────────────────────────────────────

@Composable
private fun SortedTxItemRow(
    item: SortedTransactionItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = (item.nickName?.ifEmpty { null } ?: item.entity)
        .replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(item.entity)
    val hasFee = item.transactionCost.absoluteValue > 0
    val net = item.totalIn - item.totalOut

    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Avatar — identical style to TxItemRow
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White, letterSpacing = 0.5.sp)
            }
        }

        // Middle
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.transactionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                // Times badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(avatarColor.copy(alpha = 0.12f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${item.times}×",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = avatarColor
                    )
                }
            }
            if (hasFee) {
                Text(
                    text = "Fees: Ksh ${String.format("%,.2f", item.transactionCost.absoluteValue)}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
                )
            }
        }

        // Right: amounts
        Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
            if (item.totalIn > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.tertiary))
                    Text(
                        text = "+Ksh ${String.format("%,.0f", item.totalIn)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
            if (item.totalOut > 0) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(MaterialTheme.colorScheme.error))
                    Text(
                        text = "-Ksh ${String.format("%,.0f", item.totalOut)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (item.totalIn > 0 && item.totalOut > 0) {
                Text(
                    text = "Net ${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.0f", net.absoluteValue)}",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Empty state ──────────────────────────────────────────────────────────────

@Composable
private fun TxEmptyState(message: String) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(R.drawable.transactions),
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = message,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
            )
        }
    }
}

// ─── Preview ─────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterialApi::class)
@androidx.compose.ui.tooling.preview.Preview(showBackground = true, showSystemUi = true)
@Composable
fun TransactionsScreenPreview() {
    CashLedgerTheme {
        TransactionsScreen(
            premium = false,
            transactions = com.records.pesa.reusables.transactions,
            groupedTransactionItems = com.records.pesa.reusables.moneyInSortedTransactionItems,
            totalMoneyInRaw = 12900.0,
            totalMoneyOutRaw = 4500.0,
            transactionTypes = listOf("All types", "Send Money", "Pay Bill", "Airtime & Bundles"),
            defaultTransactionType = null,
            currentTab = TransactionScreenTab.ALL_TRANSACTIONS,
            onTabSelected = {},
            selectedType = "All types",
            onSelectType = {},
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            defaultStartDate = null,
            defaultEndDate = null,
            onChangeStartDate = {},
            onChangeLastDate = {},
            selectedPeriod = TimePeriod.THIS_MONTH,
            onPeriodSelected = {},
            showDateRangePicker = false,
            onToggleDatePicker = {},
            onDismissDatePicker = {},
            searchText = "",
            onChangeSearchText = {},
            onClearSearch = {},
            categoryName = null,
            budgetName = null,
            navigateToEntityTransactionsScreen = { _, _, _ -> },
            navigateToPreviousScreen = {},
            onShowSubscriptionDialog = {},
            showBackArrow = true,
            onDownloadReport = {},
            downloadingStatus = DownloadingStatus.INITIAL,
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            navigateToTransactionDetailsScreen = {}
        )
    }
}

// ─── Subscription dialog ──────────────────────────────────────────────────────

@Composable
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(text = "Go premium?", fontWeight = FontWeight.Bold, fontSize = 16.sp)
        },
        text = {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Text("Ksh100.0 premium monthly fee", fontSize = 14.sp, fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Premium version allows you to:", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(10.dp))
                    listOf(
                        "See transactions and export reports of more than one months",
                        "Backup your transactions",
                        "Manage more than one category",
                        "Use in dark mode"
                    ).forEachIndexed { i, text ->
                        Spacer(modifier = Modifier.height(5.dp))
                        Text("${i + 1}. $text", fontSize = 14.sp)
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Dismiss") } },
        confirmButton = { Button(onClick = onConfirm) { Text("Subscribe") } }
    )
}

// ─── Download report dialog ───────────────────────────────────────────────────

@Composable
private fun DownloadReportDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onConfirm: (type: String) -> Unit,
) {
    val types = listOf("PDF", "CSV")
    var selectedType by rememberSaveable { mutableStateOf("PDF") }
    var expanded by rememberSaveable { mutableStateOf(false) }

    AlertDialog(
        title = { Text("Report for $startDate to $endDate", fontSize = 14.sp) },
        text = {
            Row(modifier = Modifier.fillMaxWidth()) {
                Text("Export to ", fontSize = 14.sp)
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { expanded = !expanded }
                    ) {
                        Text(selectedType, color = MaterialTheme.colorScheme.primary, fontSize = 14.sp)
                        Icon(painter = painterResource(R.drawable.arrow_downward), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(16.dp))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        types.forEach {
                            DropdownMenuItem(
                                text = { Text(it, fontSize = 14.sp) },
                                onClick = { selectedType = it; expanded = false }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        confirmButton = { Button(onClick = { onConfirm(selectedType) }) { Text("Confirm") } }
    )
}

// ─── Date range picker dialog ─────────────────────────────────────────────────

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
    ElevatedCard(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = "Custom date range",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (defaultStartDate != null) {
                        Text(
                            text = "Within $defaultStartDate – $defaultEndDate",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        painter = painterResource(R.drawable.baseline_clear_24),
                        contentDescription = "Close",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Date selectors
            DateRangePicker(
                premium = premium,
                startDate = startDate,
                endDate = endDate,
                defaultStartDate = defaultStartDate,
                defaultEndDate = defaultEndDate,
                onChangeStartDate = onChangeStartDate,
                onChangeLastDate = onChangeLastDate,
                onShowSubscriptionDialog = onShowSubscriptionDialog,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Done button
            androidx.compose.material3.FilledTonalButton(
                onClick = onConfirm,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Apply", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

// ─── Date range picker ────────────────────────────────────────────────────────

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
    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }
    val defaultStartMillis = defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis = defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val oneMonthAgo = java.time.LocalDateTime.now().minusMonths(1)

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selected = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selected.isBefore(endDate) || selected.isEqual(endDate)) {
                        if (selected.isBefore(oneMonthAgo.toLocalDate())) {
                            if (premium) onChangeStartDate(selected) else onShowSubscriptionDialog()
                        } else onChangeStartDate(selected)
                    } else Toast.makeText(context, "Start date must be before end date", Toast.LENGTH_LONG).show()
                } else {
                    if (selected.isAfter(startDate) || selected.isEqual(startDate)) onChangeLastDate(selected)
                    else Toast.makeText(context, "End date must be after start date", Toast.LENGTH_LONG).show()
                }
            },
            initialDate.year, initialDate.monthValue - 1, initialDate.dayOfMonth
        ).also { picker ->
            defaultStartMillis?.let { picker.datePicker.minDate = it }
            defaultEndMillis?.let { picker.datePicker.maxDate = it }
            picker.show()
        }
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // From date button
        DateChipButton(
            label = "From",
            date = startDate,
            onClick = { showDatePicker(true) },
            modifier = Modifier.weight(1f)
        )

        Icon(
            painter = painterResource(R.drawable.ic_arrow_right),
            contentDescription = null,
            modifier = Modifier.size(16.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )

        // To date button
        DateChipButton(
            label = "To",
            date = endDate,
            onClick = { showDatePicker(false) },
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun DateChipButton(
    label: String,
    date: LocalDate,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayDate = date.format(java.time.format.DateTimeFormatter.ofPattern("d MMM yyyy"))
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.primary,
            letterSpacing = 0.5.sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.calendar),
                contentDescription = null,
                modifier = Modifier.size(14.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                text = displayDate,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
