package com.records.pesa.ui.screens.dashboard.budget

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.Transaction
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.records.pesa.models.TimePeriod
import com.records.pesa.nav.AppNavigation
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.ui.screens.components.DownloadReportDialog
import com.records.pesa.ui.screens.components.EditManualTransactionDialog
import com.records.pesa.ui.screens.components.TxDateHeader
import com.records.pesa.ui.screens.components.TxEmptyState
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.dashboard.category.CombinedFilter
import com.records.pesa.ui.screens.dashboard.category.CombinedTransactionItem
import com.records.pesa.ui.screens.dashboard.category.DownloadingStatus
import com.records.pesa.workers.BudgetRecalculationWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.runtime.snapshotFlow
import com.records.pesa.ui.screens.components.formatTxDateHeader
import com.records.pesa.ui.screens.transactions.DateRangePickerDialog

// ─── Navigation ──────────────────────────────────────────────────────────────
object BudgetAllTransactionsScreenDestination : AppNavigation {
    override val title = "Budget Transactions"
    override val route = "budget-all-transactions-screen"
    const val budgetId = "budgetId"
    const val startDate = "startDate"
    const val endDate = "endDate"
    val routeWithArgs = "$route/{$budgetId}/{$startDate}/{$endDate}"
}

// ─── UiState ─────────────────────────────────────────────────────────────────
data class BudgetAllTransactionsUiState(
    val budgetName: String = "",
    val categoryId: Int? = null,
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val startDate: LocalDate = LocalDate.now().minusMonths(1),
    val endDate: LocalDate = LocalDate.now(),
    val isDateLocked: Boolean = false,
    val mpesaItems: List<Transaction> = emptyList(),
    val manualItems: List<ManualTransaction> = emptyList(),
    val memberNames: List<String> = emptyList(),
    val filter: CombinedFilter = CombinedFilter.ALL,
    val searchText: String = "",
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val userId: Int = 0,
    val backUpUserId: Long = 0,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL,
    val downloadUri: Uri? = null
)

// ─── ViewModel ───────────────────────────────────────────────────────────────
class BudgetAllTransactionsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository,
    private val transactionService: TransactionService,
    private val userAccountService: UserAccountService,
    private val application: android.app.Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetAllTransactionsUiState())
    val uiState: StateFlow<BudgetAllTransactionsUiState> = _uiState.asStateFlow()

    private val budgetIdInt: Int = savedStateHandle.get<String>(BudgetAllTransactionsScreenDestination.budgetId)
        ?.toIntOrNull() ?: 0

    init {
        // Initialize dates from nav args if provided
        val navStart = savedStateHandle.get<String>(BudgetAllTransactionsScreenDestination.startDate)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val navEnd = savedStateHandle.get<String>(BudgetAllTransactionsScreenDestination.endDate)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (navStart != null && navEnd != null) {
            // Detect which period the nav dates correspond to; fall back to CUSTOM only if no match
            val matchedPeriod = listOf(
                TimePeriod.TODAY, TimePeriod.YESTERDAY,
                TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
                TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
                TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
            ).firstOrNull { period ->
                val (s, e) = period.getDateRange()
                s == navStart && e == navEnd
            } ?: TimePeriod.CUSTOM
            _uiState.update { it.copy(startDate = navStart, endDate = navEnd, selectedPeriod = matchedPeriod, isDateLocked = true) }
        }
        loadBudget()
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val premium = prefs.permanent ||
                    (prefs.expiryDate?.isAfter(java.time.LocalDateTime.now()) == true)
                _uiState.update { it.copy(isPremium = premium) }
            }
        }
        viewModelScope.launch {
            val users = dbRepository.getUsers().first()
            if (users.isNotEmpty()) _uiState.update {
                it.copy(userId = users[0].userId, backUpUserId = users[0].backUpUserId)
            }
        }
    }

    private fun loadBudget() {
        viewModelScope.launch {
            try {
                dbRepository.getBudgetById(budgetIdInt).filterNotNull().collectLatest { budget ->
                    val members = dbRepository.getBudgetMembersOnce(budget.id).map { it.memberName }
                    // Only set dates from budget if nav args didn't provide them
                    val hasNavDates = _uiState.value.selectedPeriod == TimePeriod.CUSTOM
                    _uiState.update {
                        it.copy(
                            budgetName = budget.name,
                            categoryId = budget.categoryId,
                            startDate = if (hasNavDates) it.startDate else budget.startDate,
                            endDate = if (hasNavDates) it.endDate else budget.limitDate,
                            memberNames = members,
                            isLoading = false
                        )
                    }
                    budget.categoryId?.let { catId -> observeTransactions(catId) }
                }
            } catch (e: Exception) {
                Log.e("BudgetAllTx", "Error loading budget: $e")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
    }

    private var txJob: kotlinx.coroutines.Job? = null
    private var manualTxJob: kotlinx.coroutines.Job? = null

    private fun observeTransactions(categoryId: Int) {
        txJob?.cancel()
        manualTxJob?.cancel()

        txJob = viewModelScope.launch {
            try {
                dbRepository.getTransactionsForCategory(categoryId).collect { txList ->
                    _uiState.update { it.copy(mpesaItems = txList) }
                }
            } catch (e: Exception) {
                Log.e("BudgetAllTx", "Error loading M-PESA txs: $e")
            }
        }
        manualTxJob = viewModelScope.launch {
            try {
                dbRepository.getManualTransactionsForCategory(categoryId).collect { txList ->
                    _uiState.update { it.copy(manualItems = txList) }
                }
            } catch (e: Exception) {
                Log.e("BudgetAllTx", "Error loading manual txs: $e")
            }
        }
    }

    fun setStartDate(date: LocalDate) = _uiState.update { it.copy(startDate = date, selectedPeriod = TimePeriod.CUSTOM) }
    fun setEndDate(date: LocalDate) = _uiState.update { it.copy(endDate = date, selectedPeriod = TimePeriod.CUSTOM) }
    fun setFilter(filter: CombinedFilter) = _uiState.update { it.copy(filter = filter) }
    fun setSearchText(text: String) = _uiState.update { it.copy(searchText = text) }

    fun updatePeriod(period: TimePeriod) {
        val isPremium = _uiState.value.isPremium
        val safePeriod = if (!isPremium && (period is TimePeriod.LAST_MONTH || period is TimePeriod.THIS_YEAR || period is TimePeriod.ENTIRE)) TimePeriod.THIS_MONTH else period
        val (start, end) = safePeriod.getDateRange()
        _uiState.update { it.copy(selectedPeriod = safePeriod, startDate = start, endDate = end) }
    }

    fun updateManualTransaction(tx: ManualTransaction) {
        viewModelScope.launch {
            dbRepository.updateManualCategoryTransaction(tx)
            WorkManager.getInstance(application).enqueueUniqueWork(
                "budget_recalc_manual_tx_update",
                ExistingWorkPolicy.REPLACE,
                OneTimeWorkRequestBuilder<BudgetRecalculationWorker>().build()
            )
        }
    }

    fun fetchReportAndSave(
        context: Context,
        saveUri: Uri?,
        reportType: String,
        startDate: LocalDate,
        endDate: LocalDate,
        visibleItems: List<CombinedTransactionItem>
    ) {
        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.LOADING) }
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                try {
                    val budgetName = uiState.value.budgetName.ifBlank { "-" }

                    // Use only the visible items, further narrowed by the report date range
                    val reportItems = visibleItems.filter { item ->
                        val d = when (item) {
                            is CombinedTransactionItem.MpesaItem -> item.tx.date
                            is CombinedTransactionItem.ManualItem -> item.tx.date
                        }
                        !d.isBefore(startDate) && !d.isAfter(endDate)
                    }

                    val models = ArrayList<com.records.pesa.service.transaction.function.AllTransactionsReportModel>()
                    for (item in reportItems) {
                        val model = com.records.pesa.service.transaction.function.AllTransactionsReportModel()
                        when (item) {
                            is CombinedTransactionItem.MpesaItem -> {
                                val tx = item.tx
                                model.datetime = "${tx.date} ${tx.time}"
                                model.transactionType = tx.transactionType
                                model.category = budgetName
                                model.entity = tx.entity
                                model.moneyIn = "-"
                                model.moneyOut = "Ksh${kotlin.math.abs(tx.transactionAmount)}"
                                model.transactionCost = if (tx.transactionCost != 0.0)
                                    "Ksh${kotlin.math.abs(tx.transactionCost)}" else "-"
                            }
                            is CombinedTransactionItem.ManualItem -> {
                                val tx = item.tx
                                model.datetime = "${tx.date} ${tx.time ?: ""}"
                                model.transactionType = tx.transactionTypeName
                                model.category = budgetName
                                model.entity = tx.memberName
                                model.moneyIn = "-"
                                model.moneyOut = "Ksh${tx.amount}"
                                model.transactionCost = "-"
                            }
                        }
                        models.add(model)
                    }
                    models.sortByDescending { it.datetime }

                    val report = transactionService.generateReportFromPrebuiltModels(
                        models = models,
                        userAccount = userAccountService.getUserAccount(userId = uiState.value.userId).first(),
                        reportType = reportType,
                        startDate = startDate.toString(),
                        endDate = endDate.toString(),
                        context = context
                    )
                    if (report != null && report.isNotEmpty()) {
                        if (saveUri != null) {
                            context.contentResolver.openOutputStream(saveUri)?.use { it.write(report) }
                        }
                        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.SUCCESS, downloadUri = saveUri) }
                    } else {
                        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.FAIL) }
                    }
                } catch (e: Exception) {
                    Log.e("BudgetAllTxReport", "Error: ${e.message}")
                    _uiState.update { it.copy(downloadingStatus = DownloadingStatus.FAIL) }
                }
            }
        }
    }

    fun resetDownloadingStatus() {
        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.INITIAL) }
    }
}

// ─── Composable entry point ───────────────────────────────────────────────────
@Composable
fun BudgetAllTransactionsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit = {},
    navigateToTransactionDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: BudgetAllTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    var editingTx by remember { mutableStateOf<ManualTransaction?>(null) }
    var showDownloadDialog by rememberSaveable { mutableStateOf(false) }
    var pendingReportType by rememberSaveable { mutableStateOf("PDF") }
    var pendingStartDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var pendingEndDate by remember { mutableStateOf(LocalDate.now()) }
    var filteredSnapshot by remember { mutableStateOf<List<CombinedTransactionItem>>(emptyList()) }

    val createDocumentLauncher =
        androidx.activity.compose.rememberLauncherForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.CreateDocument()
        ) { uri: Uri? ->
            uri?.let {
                viewModel.fetchReportAndSave(
                    context = context,
                    saveUri = it,
                    reportType = pendingReportType,
                    startDate = pendingStartDate,
                    endDate = pendingEndDate,
                    visibleItems = filteredSnapshot
                )
            }
        }

    if (uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        android.widget.Toast.makeText(context, "Report downloaded", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downloadUri
        val mime = if (pendingReportType == "PDF") "application/pdf" else "text/csv"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mime)
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(intent, "Open with:"))
    } else if (uiState.downloadingStatus == DownloadingStatus.FAIL) {
        android.widget.Toast.makeText(context, "Failed to generate report", android.widget.Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    if (showDownloadDialog) {
        DownloadReportDialog(
            isPremium = uiState.isPremium,
            onDismiss = { showDownloadDialog = false },
            onConfirm = { type, start, end ->
                pendingReportType = type
                pendingStartDate = start
                pendingEndDate = end
                showDownloadDialog = false
                val ext = if (type == "PDF") ".pdf" else ".csv"
                createDocumentLauncher.launch("Budget-Transactions_${LocalDateTime.now()}$ext")
            }
        )
    }

    val allMembers = remember(uiState) {
        (uiState.mpesaItems.map { it.entity.replaceFirstChar { c -> c.uppercase() } } +
                uiState.manualItems.map { it.memberName }).distinct().sorted()
    }

    // Compute filtered list here so it can be captured for the report
    val filtered: List<CombinedTransactionItem> = remember(uiState) {
        val search = uiState.searchText
        val start = uiState.startDate
        val end = uiState.endDate
        val members = uiState.memberNames
        val mpesaList = uiState.mpesaItems
            .filter { tx ->
                tx.transactionAmount < 0 &&
                !tx.date.isBefore(start) && !tx.date.isAfter(end) &&
                (members.isEmpty() || members.any { m -> tx.entity.contains(m, ignoreCase = true) || m.contains(tx.entity, ignoreCase = true) }) &&
                (search.isBlank() || tx.entity.contains(search, ignoreCase = true) || tx.transactionType.contains(search, ignoreCase = true))
            }
            .map { CombinedTransactionItem.MpesaItem(it) }
        val manualList = uiState.manualItems
            .filter { tx ->
                tx.isOutflow &&
                !tx.date.isBefore(start) && !tx.date.isAfter(end) &&
                (members.isEmpty() || tx.memberName in members) &&
                (search.isBlank() || tx.memberName.contains(search, ignoreCase = true) || tx.description.contains(search, ignoreCase = true))
            }
            .map { CombinedTransactionItem.ManualItem(it) }
        when (uiState.filter) {
            CombinedFilter.ALL, CombinedFilter.BY_MEMBER -> mpesaList + manualList
            CombinedFilter.MPESA -> mpesaList
            CombinedFilter.MANUAL -> manualList
        }
    }
    filteredSnapshot = filtered

    editingTx?.let { tx ->
        EditManualTransactionDialog(
            tx = tx,
            members = allMembers,
            onSave = { updated -> viewModel.updateManualTransaction(updated); editingTx = null },
            onDismiss = { editingTx = null }
        )
    }

    var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }
    if (showSubscriptionDialog) {
        com.records.pesa.ui.screens.components.SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = { showSubscriptionDialog = false; navigateToSubscriptionScreen() }
        )
    }

    BudgetAllTransactionsScreen(
        uiState = uiState,
        filteredItems = filtered,
        onFilterChange = viewModel::setFilter,
        onSearchTextChange = viewModel::setSearchText,
        onStartDateChange = viewModel::setStartDate,
        onEndDateChange = viewModel::setEndDate,
        onPeriodSelected = { viewModel.updatePeriod(it) },
        onShowSubscriptionDialog = { showSubscriptionDialog = true },
        onNavigateBack = navigateToPreviousScreen,
        onEditManualTx = { editingTx = it },
        onDownloadReport = { showDownloadDialog = true },
        onNavigateToTransactionDetails = navigateToTransactionDetails,
        isDownloading = uiState.downloadingStatus == DownloadingStatus.LOADING,
        modifier = modifier
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetAllTransactionsScreen(
    uiState: BudgetAllTransactionsUiState,
    filteredItems: List<CombinedTransactionItem> = emptyList(),
    onFilterChange: (CombinedFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onPeriodSelected: (TimePeriod) -> Unit = {},
    onShowSubscriptionDialog: () -> Unit = {},
    onNavigateBack: () -> Unit,
    onEditManualTx: (ManualTransaction) -> Unit = {},
    onDownloadReport: () -> Unit = {},
    onNavigateToTransactionDetails: (String) -> Unit = {},
    isDownloading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val freeLimit: LocalDate = remember { LocalDate.now().minusMonths(1) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    var showDateRangePicker by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else { keyboard?.hide(); onSearchTextChange("") }
    }

    val filtered: List<CombinedTransactionItem> = filteredItems

    val groupedByDate = remember(filtered) {
        val sorted = filtered.sortedWith(
            compareByDescending<CombinedTransactionItem> { item ->
                when (item) {
                    is CombinedTransactionItem.MpesaItem -> item.tx.date.toString()
                    is CombinedTransactionItem.ManualItem -> item.tx.date.toString()
                }
            }.thenComparator { a, b ->
                val timeA: java.time.LocalTime? = when (a) {
                    is CombinedTransactionItem.MpesaItem -> a.tx.time
                    is CombinedTransactionItem.ManualItem -> a.tx.time
                }
                val timeB: java.time.LocalTime? = when (b) {
                    is CombinedTransactionItem.MpesaItem -> b.tx.time
                    is CombinedTransactionItem.ManualItem -> b.tx.time
                }
                when {
                    timeA == null && timeB == null -> 0
                    timeA == null -> 1
                    timeB == null -> -1
                    else -> timeB.compareTo(timeA)
                }
            }
        )
        sorted.groupBy { item ->
            when (item) {
                is CombinedTransactionItem.MpesaItem -> item.tx.date.toString()
                is CombinedTransactionItem.ManualItem -> item.tx.date.toString()
            }
        }.entries.sortedByDescending { it.key }
    }

    val groupedByMember = remember(filtered) {
        filtered.groupBy { item ->
            when (item) {
                is CombinedTransactionItem.MpesaItem -> {
                    val tx = item.tx
                    (tx.nickName?.trim()?.ifBlank { null } ?: tx.entity.trim().replaceFirstChar { it.uppercase() }.ifBlank { "Unknown" })
                }
                is CombinedTransactionItem.ManualItem -> item.tx.memberName
            }
        }.entries.sortedBy { it.key }
    }

    val summaryIn = remember(filtered) {
        filtered.sumOf { item ->
            when (item) {
                is CombinedTransactionItem.MpesaItem -> if (item.tx.transactionAmount > 0) item.tx.transactionAmount else 0.0
                is CombinedTransactionItem.ManualItem -> if (!item.tx.isOutflow) item.tx.amount else 0.0
            }
        }
    }
    val summaryOut = remember(filtered) {
        filtered.sumOf { item ->
            when (item) {
                is CombinedTransactionItem.MpesaItem -> if (item.tx.transactionAmount < 0) kotlin.math.abs(item.tx.transactionAmount) else 0.0
                is CombinedTransactionItem.ManualItem -> if (item.tx.isOutflow) item.tx.amount else 0.0
            }
        }
    }
    val summaryNet = summaryIn - summaryOut

    val lazyListState = rememberLazyListState()
    var stickyDate by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lazyListState, uiState.filter) {
        snapshotFlow {
            lazyListState.firstVisibleItemIndex to lazyListState.firstVisibleItemScrollOffset
        }.collect { (firstIndex, _) ->
            if (uiState.filter != CombinedFilter.BY_MEMBER) {
                val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
                val passedHeader = visibleItems
                    .filter { (it.key as? String)?.startsWith("header_") == true && it.offset < 0 }
                    .maxByOrNull { it.index }
                when {
                    passedHeader != null -> stickyDate = (passedHeader.key as? String)?.removePrefix("header_")
                    firstIndex <= 1 -> stickyDate = null
                }
            } else {
                stickyDate = null
            }
        }
    }

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
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
                    IconButton(onClick = {
                        if (showSearch) showSearch = false else onNavigateBack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scaleX = -1f, scaleY = 1f)
                        )
                    }

                    if (showSearch) {
                        OutlinedTextField(
                            value = uiState.searchText,
                            onValueChange = onSearchTextChange,
                            placeholder = {
                                Text(
                                    "Search transactions…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { showSearch = false }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Close",
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
                                text = "Transactions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.budgetName.isNotBlank()) {
                                Text(
                                    text = uiState.budgetName,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    IconButton(onClick = onDownloadReport, enabled = !isDownloading) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Download report",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Filter+period bar moved into LazyColumn as stickyHeader ──────

            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Hero card or locked date row
                    if (uiState.isDateLocked) {
                        item {
                            val dateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM yy") }
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(painter = painterResource(R.drawable.calendar), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(14.dp))
                                Text(text = "${uiState.startDate.format(dateFmt)} – ${uiState.endDate.format(dateFmt)}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Spacer(Modifier.weight(1f))
                                Text(text = "${filtered.size} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        item {
                            BudgetHeroCard(
                                totalIn = summaryIn,
                                totalOut = summaryOut,
                                net = summaryNet,
                                txCount = filtered.size,
                                startDate = uiState.startDate,
                                endDate = uiState.endDate,
                                selectedPeriod = uiState.selectedPeriod,
                                onPeriodSelected = onPeriodSelected,
                                onOpenCustomPicker = { showDateRangePicker = true },
                                isPremium = uiState.isPremium,
                                onShowSubscriptionDialog = onShowSubscriptionDialog,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    }

                    // Date range picker
                    if (showDateRangePicker && !uiState.isDateLocked) {
                        item {
                            DateRangePickerDialog(
                                premium = uiState.isPremium,
                                startDate = uiState.startDate,
                                endDate = uiState.endDate,
                                defaultStartDate = null,
                                defaultEndDate = null,
                                onChangeStartDate = { onStartDateChange(it) },
                                onChangeLastDate = { onEndDateChange(it) },
                                onDismiss = { showDateRangePicker = false },
                                onConfirm = { showDateRangePicker = false },
                                onShowSubscriptionDialog = onShowSubscriptionDialog,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }

                    // Sticky filter + period bar
                    stickyHeader {
                        BudgetFilterAndPeriodBar(
                            currentFilter = uiState.filter,
                            onFilterSelected = onFilterChange,
                            selectedPeriod = uiState.selectedPeriod,
                            startDate = uiState.startDate,
                            endDate = uiState.endDate,
                            isPremium = uiState.isPremium,
                            isDateLocked = uiState.isDateLocked,
                            onPeriodSelected = onPeriodSelected,
                            onOpenCustomPicker = { showDateRangePicker = true },
                            onShowSubscriptionDialog = onShowSubscriptionDialog,
                            totalIn = summaryIn,
                            totalOut = summaryOut,
                            net = summaryNet,
                            currentSectionDate = if (uiState.filter != CombinedFilter.BY_MEMBER) stickyDate else null
                        )
                    }

                    // Content
                    if (uiState.isLoading) {
                        item {
                            Box(Modifier.fillMaxWidth().height(200.dp), contentAlignment = Alignment.Center) {
                                CircularProgressIndicator()
                            }
                        }
                    } else if (filtered.isEmpty()) {
                        item {
                            TxEmptyState(
                                message = if (uiState.searchText.isNotBlank())
                                    "No transactions matching \"${uiState.searchText}\""
                                else "No expenses in this date range"
                            )
                        }
                    } else if (uiState.filter == CombinedFilter.BY_MEMBER) {
                        groupedByMember.forEach { (memberName, memberItems) ->
                            item(key = "member_$memberName") {
                                Surface(modifier = Modifier.fillMaxWidth(), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(modifier = Modifier.size(28.dp).clip(CircleShape).background(txAvatarColor(memberName)), contentAlignment = Alignment.Center) {
                                            Text(memberName.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                        Text(memberName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                        Text("${memberItems.size} tx", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                            items(memberItems) { item ->
                                val itemDate = when (item) { is CombinedTransactionItem.MpesaItem -> item.tx.date; is CombinedTransactionItem.ManualItem -> item.tx.date }
                                val isLocked = !uiState.isPremium && itemDate.isBefore(freeLimit)
                                BudgetPremiumTxWrapper(isLocked = isLocked, onLockedClick = onShowSubscriptionDialog) {
                                    when (item) {
                                        is CombinedTransactionItem.MpesaItem -> BudgetAllMpesaTxRow(tx = item.tx, onClick = { if (!isLocked) onNavigateToTransactionDetails("${item.tx.id}") })
                                        is CombinedTransactionItem.ManualItem -> BudgetAllManualTxRow(tx = item.tx, onEdit = { if (!isLocked) onEditManualTx(item.tx) }, onClick = { if (!isLocked) onNavigateToTransactionDetails("m_${item.tx.id}") })
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))
                            }
                        }
                    } else {
                        groupedByDate.forEach { (dateStr, itemsForDate) ->
                            item(key = "header_$dateStr") {
                                TxDateHeader(date = dateStr)
                            }
                            items(itemsForDate) { item ->
                                val itemDate = when (item) { is CombinedTransactionItem.MpesaItem -> item.tx.date; is CombinedTransactionItem.ManualItem -> item.tx.date }
                                val isLocked = !uiState.isPremium && itemDate.isBefore(freeLimit)
                                BudgetPremiumTxWrapper(isLocked = isLocked, onLockedClick = onShowSubscriptionDialog) {
                                    when (item) {
                                        is CombinedTransactionItem.MpesaItem -> BudgetAllMpesaTxRow(tx = item.tx, onClick = { if (!isLocked) onNavigateToTransactionDetails("${item.tx.id}") })
                                        is CombinedTransactionItem.ManualItem -> BudgetAllManualTxRow(tx = item.tx, onEdit = { if (!isLocked) onEditManualTx(item.tx) }, onClick = { if (!isLocked) onNavigateToTransactionDetails("m_${item.tx.id}") })
                                    }
                                }
                                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))
                            }
                        }
                    }
                }

                if (uiState.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                    }
                }
            }
        }
    }
}

// ─── Premium blur wrapper ────────────────────────────────────────────────────
@Composable
private fun BudgetPremiumTxWrapper(isLocked: Boolean, onLockedClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box {
        Box(modifier = if (isLocked) Modifier.blur(6.dp) else Modifier) { content() }
        if (isLocked) {
            Box(
                modifier = Modifier.matchParentSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
                    .clickable(onClick = onLockedClick),
                contentAlignment = Alignment.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f)
                ) {
                    Text(
                        "🔒 Premium — upgrade to view",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.tertiary,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

// ─── Row composables ─────────────────────────────────────────────────────────
@Composable
private fun BudgetAllMpesaTxRow(tx: Transaction, onClick: () -> Unit = {}) {
    val displayName = tx.entity.replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(tx.entity)
    val amountColor = MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = displayName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = tx.transactionType,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "· ${tx.date.format(DateTimeFormatter.ofPattern("d MMM"))}  ${tx.time.format(DateTimeFormatter.ofPattern("HH:mm"))}",
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                    maxLines = 1,
                    softWrap = false
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(amountColor))
            Text(
                text = "-Ksh ${String.format("%,.0f", tx.transactionAmount.absoluteValue)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun BudgetAllManualTxRow(tx: ManualTransaction, onEdit: () -> Unit = {}, onClick: () -> Unit = {}) {
    val amountColor = MaterialTheme.colorScheme.error
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            val avatarColor = txAvatarColor(tx.memberName)
            Box(modifier = Modifier.size(50.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)))
            Box(
                modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                val initials = tx.memberName.trim().split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2).joinToString("").ifEmpty { tx.memberName.take(2).uppercase() }
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            // Manual badge
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomEnd)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = null,
                    modifier = Modifier.size(10.dp),
                    tint = Color.White
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(
                text = tx.memberName,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "Manual · ${tx.transactionTypeName}",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = buildString {
                        append("· ${tx.date.format(dateFormatter)}")
                        tx.time?.let { append("  ${it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}") }
                    },
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
            if (tx.description.isNotBlank()) {
                Text(
                    text = tx.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(amountColor))
            Text(
                text = "-Ksh ${String.format("%,.0f", tx.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun BudgetHeroCard(
    totalIn: Double,
    totalOut: Double,
    net: Double,
    txCount: Int,
    startDate: LocalDate,
    endDate: LocalDate,
    selectedPeriod: TimePeriod,
    onPeriodSelected: (TimePeriod) -> Unit,
    onOpenCustomPicker: () -> Unit,
    isPremium: Boolean,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val periodOptions = remember {
        listOf(
            TimePeriod.TODAY, TimePeriod.YESTERDAY,
            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
            TimePeriod.THIS_YEAR, TimePeriod.ENTIRE
        )
    }
    var showPeriodMenu by remember { mutableStateOf(false) }
    val dateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM, yyyy") }

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
                                text = if (selectedPeriod == TimePeriod.CUSTOM)
                                    "${dateFmt.format(startDate)} – ${dateFmt.format(endDate)}"
                                else selectedPeriod.getDisplayName().uppercase(),
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
                        DropdownMenu(expanded = showPeriodMenu, onDismissRequest = { showPeriodMenu = false }) {
                            periodOptions.forEach { period ->
                                val requiresPremium = !isPremium && (period == TimePeriod.LAST_MONTH || period == TimePeriod.THIS_YEAR || period == TimePeriod.ENTIRE)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(
                                                text = period.getDisplayName(),
                                                fontSize = 14.sp,
                                                fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                                                color = if (period == selectedPeriod) primaryColor else MaterialTheme.colorScheme.onSurface
                                            )
                                            if (requiresPremium) {
                                                Icon(painter = painterResource(R.drawable.lock), contentDescription = "Premium", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    },
                                    onClick = {
                                        showPeriodMenu = false
                                        if (requiresPremium) onShowSubscriptionDialog() else onPeriodSelected(period)
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(painter = painterResource(R.drawable.calendar), contentDescription = null, modifier = Modifier.size(14.dp), tint = primaryColor)
                                        Text(text = "Custom", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryColor)
                                    }
                                },
                                onClick = { showPeriodMenu = false; onOpenCustomPicker() }
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        if (selectedPeriod != TimePeriod.CUSTOM) {
                            Text(
                                text = "${dateFmt.format(startDate)} – ${dateFmt.format(endDate)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Text(
                            text = "$txCount txn${if (txCount != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Net Flow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.2f", kotlin.math.abs(net))}",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    BudgetHeroStatCol(label = "Money In", value = "Ksh ${String.format("%,.0f", totalIn)}", color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f))
                    BudgetHeroStatCol(label = "Money Out", value = "Ksh ${String.format("%,.0f", totalOut)}", color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f))
                    BudgetHeroStatCol(
                        label = "Net",
                        value = "${if (net >= 0) "" else "-"}Ksh ${String.format("%,.0f", kotlin.math.abs(net))}",
                        color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetHeroStatCol(label: String, value: String, color: androidx.compose.ui.graphics.Color, modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
        Spacer(modifier = Modifier.height(3.dp))
        Text(value, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = color)
    }
}

@Composable
private fun BudgetFilterAndPeriodBar(
    currentFilter: CombinedFilter,
    onFilterSelected: (CombinedFilter) -> Unit,
    selectedPeriod: TimePeriod,
    startDate: LocalDate,
    endDate: LocalDate,
    isPremium: Boolean,
    isDateLocked: Boolean,
    onPeriodSelected: (TimePeriod) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    totalIn: Double,
    totalOut: Double,
    net: Double,
    currentSectionDate: String? = null
) {
    val primary = MaterialTheme.colorScheme.primary
    val periodOptions = remember {
        listOf(TimePeriod.TODAY, TimePeriod.YESTERDAY, TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK, TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH, TimePeriod.THIS_YEAR, TimePeriod.ENTIRE)
    }
    val dateFmt = remember { java.time.format.DateTimeFormatter.ofPattern("d MMM, yyyy") }

    Surface(
        modifier = Modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.background,
        shadowElevation = 2.dp,
        tonalElevation = 1.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Row 1: 4 filter chip pills
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                listOf(CombinedFilter.ALL, CombinedFilter.MPESA, CombinedFilter.MANUAL, CombinedFilter.BY_MEMBER).forEach { filter ->
                    val selected = currentFilter == filter
                    val bgColor by animateColorAsState(
                        targetValue = if (selected) primary else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        animationSpec = tween(200),
                        label = "chipColor"
                    )
                    val textColor by animateColorAsState(
                        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                        animationSpec = tween(200),
                        label = "chipTextColor"
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(bgColor)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() }
                            ) { onFilterSelected(filter) }
                            .padding(horizontal = 14.dp, vertical = 7.dp)
                    ) {
                        Text(
                            text = when (filter) {
                                CombinedFilter.ALL -> "All Transactions"
                                CombinedFilter.MPESA -> "M-PESA"
                                CombinedFilter.MANUAL -> "Manual"
                                CombinedFilter.BY_MEMBER -> "By Member"
                            },
                            fontSize = 13.sp,
                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                            color = textColor
                        )
                    }
                }
            }

            // Row 2: Period chip + In/Out/Net (horizontally scrollable, hidden if date locked)
            if (!isDateLocked) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box {
                        var showPeriodMenu by remember { mutableStateOf(false) }
                        val chipLabel = if (selectedPeriod == TimePeriod.CUSTOM)
                            "${dateFmt.format(startDate)} – ${dateFmt.format(endDate)}"
                        else selectedPeriod.getDisplayName().uppercase()
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(primary.copy(alpha = 0.08f))
                                .clickable { showPeriodMenu = true }
                                .padding(horizontal = 8.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(chipLabel, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = primary, letterSpacing = 0.5.sp)
                            Icon(painter = painterResource(R.drawable.arrow_downward), contentDescription = null, tint = primary, modifier = Modifier.size(10.dp))
                        }
                        DropdownMenu(expanded = showPeriodMenu, onDismissRequest = { showPeriodMenu = false }) {
                            periodOptions.forEach { period ->
                                val requiresPremium = !isPremium && (period == TimePeriod.LAST_MONTH || period == TimePeriod.THIS_YEAR || period == TimePeriod.ENTIRE)
                                DropdownMenuItem(
                                    text = {
                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            Text(period.getDisplayName(), fontSize = 14.sp, fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal, color = if (period == selectedPeriod) primary else MaterialTheme.colorScheme.onSurface)
                                            if (requiresPremium) {
                                                Icon(painter = painterResource(R.drawable.lock), contentDescription = "Premium", modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.tertiary)
                                            }
                                        }
                                    },
                                    onClick = { showPeriodMenu = false; if (requiresPremium) onShowSubscriptionDialog() else onPeriodSelected(period) }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Icon(painter = painterResource(R.drawable.calendar), contentDescription = null, modifier = Modifier.size(14.dp), tint = primary)
                                        Text("Custom", fontSize = 14.sp, fontWeight = if (selectedPeriod == TimePeriod.CUSTOM) FontWeight.Bold else FontWeight.Medium, color = primary)
                                    }
                                },
                                onClick = { showPeriodMenu = false; onOpenCustomPicker() }
                            )
                        }
                    }

                    Box(modifier = Modifier.width(1.dp).height(20.dp).background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)))

                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("In", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                        Text("Ksh ${String.format("%,.0f", totalIn)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                        Text("Ksh ${String.format("%,.0f", totalOut)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Net", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f), fontWeight = FontWeight.Medium)
                        val netStr = if (net >= 0) "Ksh ${String.format("%,.0f", net)}" else "-Ksh ${String.format("%,.0f", kotlin.math.abs(net))}"
                        Text(netStr, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error)
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))

            if (currentSectionDate != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(painter = painterResource(R.drawable.calendar), contentDescription = null, modifier = Modifier.size(11.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f))
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
