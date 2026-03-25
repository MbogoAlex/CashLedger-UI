package com.records.pesa.ui.screens.dashboard.category

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.app.DatePickerDialog
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
import com.records.pesa.nav.AppNavigation
import com.records.pesa.service.category.CategoryService
import com.records.pesa.service.transaction.TransactionService
import com.records.pesa.service.userAccount.UserAccountService
import com.records.pesa.ui.screens.components.DownloadReportDialog
import com.records.pesa.ui.screens.components.EditManualTransactionDialog
import com.records.pesa.ui.screens.components.SubscriptionDialog
import com.records.pesa.ui.screens.components.TxDateHeader
import com.records.pesa.ui.screens.components.TxEmptyState
import com.records.pesa.ui.screens.components.TxSummaryBar
import com.records.pesa.ui.screens.components.txAvatarColor
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import com.records.pesa.models.TimePeriod
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

object CategoryAllTransactionsScreenDestination : AppNavigation {
    override val title = "All Transactions"
    override val route = "category-all-transactions-screen"
    const val categoryId = "categoryId"
    const val startDate = "startDate"
    const val endDate = "endDate"
    val routeWithArgs = "$route/{$categoryId}/{$startDate}/{$endDate}"
}

enum class CombinedFilter { ALL, MPESA, MANUAL, BY_MEMBER }

sealed class CombinedTransactionItem {
    data class MpesaItem(val tx: Transaction) : CombinedTransactionItem()
    data class ManualItem(val tx: ManualTransaction) : CombinedTransactionItem()
}

data class CategoryAllTransactionsUiState(
    val categoryName: String = "",
    val mpesaItems: List<Transaction> = emptyList(),
    val manualItems: List<ManualTransaction> = emptyList(),
    val filter: CombinedFilter = CombinedFilter.ALL,
    val searchText: String = "",
    val selectedPeriod: TimePeriod = TimePeriod.THIS_MONTH,
    val startDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    val endDate: LocalDate = LocalDate.now(),
    val isPremium: Boolean = false,
    val isLoading: Boolean = true,
    val userId: Int = 0,
    val backUpUserId: Long = 0,
    val downloadingStatus: DownloadingStatus = DownloadingStatus.INITIAL,
    val downloadUri: Uri? = null
)

class CategoryAllTransactionsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService,
    private val dataStoreRepository: DataStoreRepository,
    private val transactionService: TransactionService,
    private val userAccountService: UserAccountService,
    private val application: android.app.Application
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryAllTransactionsUiState())
    val uiState: StateFlow<CategoryAllTransactionsUiState> = _uiState.asStateFlow()

    private val categoryIdInt: Int = savedStateHandle.get<String>(CategoryAllTransactionsScreenDestination.categoryId)
        ?.toIntOrNull() ?: 0

    init {
        // Initialize dates from nav args if provided
        val navStart = savedStateHandle.get<String>(CategoryAllTransactionsScreenDestination.startDate)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        val navEnd = savedStateHandle.get<String>(CategoryAllTransactionsScreenDestination.endDate)
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }
        if (navStart != null && navEnd != null) {
            _uiState.update { it.copy(startDate = navStart, endDate = navEnd, selectedPeriod = TimePeriod.CUSTOM) }
        }
        loadCategoryName()
        loadData()
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

    private fun loadCategoryName() {
        viewModelScope.launch {
            try {
                categoryService.getCategoryById(categoryIdInt).collect { cat ->
                    _uiState.update { it.copy(categoryName = cat.category.name) }
                }
            } catch (e: Exception) {
                Log.e("CategoryAllTx", "Error loading category: $e")
            }
        }
    }

    private fun loadData() {
        viewModelScope.launch {
            try {
                dbRepository.getManualTransactionsForCategory(categoryIdInt).collect { manual ->
                    _uiState.update { it.copy(manualItems = manual, isLoading = false) }
                }
            } catch (e: Exception) {
                Log.e("CategoryAllTx", "Error loading manual txs: $e")
                _uiState.update { it.copy(isLoading = false) }
            }
        }
        viewModelScope.launch {
            try {
                dbRepository.getTransactionsForCategory(categoryIdInt).collect { txList ->
                    _uiState.update { it.copy(mpesaItems = txList) }
                }
            } catch (e: Exception) {
                Log.e("CategoryAllTx", "Error loading mpesa txs: $e")
            }
        }
    }

    fun setFilter(filter: CombinedFilter) {
        _uiState.update { it.copy(filter = filter) }
    }

    fun setStartDate(date: LocalDate) = _uiState.update { it.copy(startDate = date, selectedPeriod = TimePeriod.CUSTOM) }
    fun setEndDate(date: LocalDate) = _uiState.update { it.copy(endDate = date, selectedPeriod = TimePeriod.CUSTOM) }

    fun updatePeriod(period: TimePeriod) {
        val isPremium = _uiState.value.isPremium
        val safePeriod = if (!isPremium && (period is TimePeriod.LAST_MONTH || period is TimePeriod.THIS_YEAR || period is TimePeriod.ENTIRE)) TimePeriod.THIS_MONTH else period
        val (start, end) = safePeriod.getDateRange()
        _uiState.update { it.copy(selectedPeriod = safePeriod, startDate = start, endDate = end) }
    }

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
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
                    // Use only the visible (filtered) items, further narrowed by the report date range
                    val reportItems = visibleItems.filter { item ->
                        val d = when (item) {
                            is CombinedTransactionItem.MpesaItem -> item.tx.date
                            is CombinedTransactionItem.ManualItem -> item.tx.date
                        }
                        !d.isBefore(startDate) && !d.isAfter(endDate)
                    }

                    val categoryName = uiState.value.categoryName.ifBlank { "-" }
                    val models = ArrayList<com.records.pesa.service.transaction.function.AllTransactionsReportModel>()

                    for (item in reportItems) {
                        val model = com.records.pesa.service.transaction.function.AllTransactionsReportModel()
                        when (item) {
                            is CombinedTransactionItem.MpesaItem -> {
                                val tx = item.tx
                                model.datetime = "${tx.date} ${tx.time}"
                                model.transactionType = tx.transactionType
                                model.category = categoryName
                                model.entity = tx.entity
                                if (tx.transactionAmount > 0) {
                                    model.moneyIn = "Ksh${tx.transactionAmount}"; model.moneyOut = "-"
                                } else {
                                    model.moneyIn = "-"; model.moneyOut = "Ksh${kotlin.math.abs(tx.transactionAmount)}"
                                }
                                model.transactionCost = if (tx.transactionCost != 0.0)
                                    "Ksh${kotlin.math.abs(tx.transactionCost)}" else "-"
                            }
                            is CombinedTransactionItem.ManualItem -> {
                                val tx = item.tx
                                model.datetime = "${tx.date} ${tx.time ?: ""}"
                                model.transactionType = tx.transactionTypeName
                                model.category = categoryName
                                model.entity = tx.memberName
                                if (!tx.isOutflow) {
                                    model.moneyIn = "Ksh${tx.amount}"; model.moneyOut = "-"
                                } else {
                                    model.moneyIn = "-"; model.moneyOut = "Ksh${tx.amount}"
                                }
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
                    Log.e("CatAllTxReport", "Error: ${e.message}")
                    _uiState.update { it.copy(downloadingStatus = DownloadingStatus.FAIL) }
                }
            }
        }
    }

    fun resetDownloadingStatus() {
        _uiState.update { it.copy(downloadingStatus = DownloadingStatus.INITIAL) }
    }
}

@Composable
fun CategoryAllTransactionsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToTransactionDetails: (String) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val viewModel: CategoryAllTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
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
                createDocumentLauncher.launch("Category-Transactions_${LocalDateTime.now()}$ext")
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
        val mpesaList = uiState.mpesaItems
            .filter { tx ->
                !tx.date.isBefore(start) && !tx.date.isAfter(end) &&
                (search.isBlank() || tx.entity?.contains(search, ignoreCase = true) == true ||
                 tx.transactionType.contains(search, ignoreCase = true))
            }
            .map { CombinedTransactionItem.MpesaItem(it) }
        val manualList = uiState.manualItems
            .filter { tx ->
                !tx.date.isBefore(start) && !tx.date.isAfter(end) &&
                (search.isBlank() || tx.memberName.contains(search, ignoreCase = true) ||
                 tx.transactionTypeName.contains(search, ignoreCase = true))
            }
            .map { CombinedTransactionItem.ManualItem(it) }
        when (uiState.filter) {
            CombinedFilter.ALL, CombinedFilter.BY_MEMBER -> (mpesaList + manualList)
            CombinedFilter.MPESA -> mpesaList
            CombinedFilter.MANUAL -> manualList
        }
    }
    filteredSnapshot = filtered

    var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }

    editingTx?.let { tx ->
        EditManualTransactionDialog(
            tx = tx,
            members = allMembers,
            onSave = { updated -> viewModel.updateManualTransaction(updated); editingTx = null },
            onDismiss = { editingTx = null }
        )
    }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = { showSubscriptionDialog = false }
        )
    }

    CategoryAllTransactionsScreen(
        uiState = uiState,
        filteredItems = filtered,
        onFilterChange = { viewModel.setFilter(it) },
        onStartDateChange = viewModel::setStartDate,
        onEndDateChange = viewModel::setEndDate,
        onPeriodSelected = { viewModel.updatePeriod(it) },
        onShowSubscriptionDialog = { showSubscriptionDialog = true },
        onSearchTextChange = { viewModel.setSearchText(it) },
        onNavigateBack = navigateToPreviousScreen,
        onEditManualTx = { editingTx = it },
        onDownloadReport = { showDownloadDialog = true },
        onNavigateToTransactionDetails = navigateToTransactionDetails,
        isDownloading = uiState.downloadingStatus == DownloadingStatus.LOADING,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryAllTransactionsScreen(
    uiState: CategoryAllTransactionsUiState,
    filteredItems: List<CombinedTransactionItem> = emptyList(),
    onFilterChange: (CombinedFilter) -> Unit,
    onStartDateChange: (LocalDate) -> Unit = {},
    onEndDateChange: (LocalDate) -> Unit = {},
    onPeriodSelected: (TimePeriod) -> Unit = {},
    onShowSubscriptionDialog: () -> Unit = {},
    onSearchTextChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    onEditManualTx: (ManualTransaction) -> Unit = {},
    onDownloadReport: () -> Unit = {},
    onNavigateToTransactionDetails: (String) -> Unit = {},
    isDownloading: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yy") }
    // Free users: 1-month limit — transactions older than this are blurred
    val freeLimit: LocalDate = remember { LocalDate.now().minusMonths(1) }
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else { keyboard?.hide(); onSearchTextChange("") }
    }

    val filtered: List<CombinedTransactionItem> = filteredItems

    val groupedByDate = remember(filtered) {
        // Sort newest-first: by date DESC, then by time DESC (null time = earliest within day)
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
                    timeA == null -> 1   // null goes after real times
                    timeB == null -> -1
                    else -> timeB.compareTo(timeA) // DESC
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

    Box(
        modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Top bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 2.dp,
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
                        if (showSearch) { showSearch = false }
                        else onNavigateBack()
                    }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Back",
                            modifier = Modifier.size(22.dp),
                            tint = MaterialTheme.colorScheme.onSurface
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
                                text = "All Transactions",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.categoryName.isNotBlank()) {
                                Text(
                                    uiState.categoryName,
                                    fontSize = 13.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Spacer(Modifier.weight(1f))
                        IconButton(onClick = { showSearch = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    // Download always visible regardless of search state
                    IconButton(onClick = onDownloadReport, enabled = !isDownloading) {
                        if (isDownloading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Download report",
                                modifier = Modifier.size(22.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }

            // ── Period picker row ────────────────────────────────────────────
            AllTxnsPeriodRow(
                selectedPeriod = uiState.selectedPeriod,
                startDate = uiState.startDate,
                endDate = uiState.endDate,
                txCount = filtered.size,
                isPremium = uiState.isPremium,
                context = context,
                dateFmt = dateFmt,
                onPeriodSelected = onPeriodSelected,
                onStartDateChange = onStartDateChange,
                onEndDateChange = onEndDateChange,
                onShowSubscriptionDialog = onShowSubscriptionDialog
            )

            // Filter chips — All | M-PESA | Manual | By Member (scrollable)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CombinedFilter.values().forEach { f ->
                    FilterChip(
                        selected = uiState.filter == f,
                        onClick = { onFilterChange(f) },
                        leadingIcon = if (f == CombinedFilter.BY_MEMBER) ({
                            Icon(
                                painter = painterResource(R.drawable.contact),
                                contentDescription = null,
                                modifier = Modifier.size(14.dp)
                            )
                        }) else null,
                        label = {
                            Text(
                                when (f) {
                                    CombinedFilter.ALL -> "All"
                                    CombinedFilter.MPESA -> "M-PESA"
                                    CombinedFilter.MANUAL -> "Manual"
                                    CombinedFilter.BY_MEMBER -> "By Member"
                                }
                            )
                        }
                    )
                }
            }

            // Summary bar
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
            TxSummaryBar(totalIn = summaryIn, totalOut = summaryOut)

            // Content
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                TxEmptyState(
                    message = if (uiState.searchText.isNotBlank())
                        "No transactions matching \"${uiState.searchText}\""
                    else "No transactions found"
                )
            } else if (uiState.filter == CombinedFilter.BY_MEMBER) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedByMember.forEach { (memberName, memberItems) ->
                        stickyHeader(key = "member_$memberName") {
                            Surface(
                                modifier = Modifier.fillMaxWidth(),
                                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    val avatarColor = txAvatarColor(memberName)
                                    Box(
                                        modifier = Modifier.size(28.dp).clip(CircleShape).background(avatarColor),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(memberName.take(1).uppercase(), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                    }
                                    Text(memberName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${memberItems.size} tx", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        items(memberItems) { item ->
                            val isLocked = !uiState.isPremium && itemDate(item).isBefore(freeLimit)
                            PremiumTxWrapper(isLocked = isLocked, onLockedClick = onShowSubscriptionDialog) {
                                when (item) {
                                    is CombinedTransactionItem.MpesaItem -> MpesaTxRow(
                                        tx = item.tx,
                                        onClick = { if (!isLocked) onNavigateToTransactionDetails("${item.tx.id}") }
                                    )
                                    is CombinedTransactionItem.ManualItem -> ManualTxRow(
                                        tx = item.tx,
                                        onEdit = { if (!isLocked) onEditManualTx(item.tx) },
                                        onClick = { if (!isLocked) onNavigateToTransactionDetails("m_${item.tx.id}") }
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedByDate.forEach { (dateStr, itemsForDate) ->
                        stickyHeader(key = "header_$dateStr") {
                            TxDateHeader(date = dateStr)
                        }
                        items(itemsForDate) { item ->
                            val isLocked = !uiState.isPremium && itemDate(item).isBefore(freeLimit)
                            PremiumTxWrapper(isLocked = isLocked, onLockedClick = onShowSubscriptionDialog) {
                                when (item) {
                                    is CombinedTransactionItem.MpesaItem -> MpesaTxRow(
                                        tx = item.tx,
                                        onClick = { if (!isLocked) onNavigateToTransactionDetails("${item.tx.id}") }
                                    )
                                    is CombinedTransactionItem.ManualItem -> ManualTxRow(
                                        tx = item.tx,
                                        onEdit = { if (!isLocked) onEditManualTx(item.tx) },
                                        onClick = { if (!isLocked) onNavigateToTransactionDetails("m_${item.tx.id}") }
                                    )
                                }
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f))
                        }
                    }
                }
            }
        }
    }
}

private fun itemDate(item: CombinedTransactionItem): LocalDate = when (item) {
    is CombinedTransactionItem.MpesaItem -> item.tx.date
    is CombinedTransactionItem.ManualItem -> item.tx.date
}

@Composable
private fun PremiumTxWrapper(isLocked: Boolean, onLockedClick: () -> Unit = {}, content: @Composable () -> Unit) {
    Box {
        Box(modifier = if (isLocked) Modifier.blur(6.dp) else Modifier) { content() }
        if (isLocked) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.4f))
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

@Composable
internal fun AllTxnsPeriodRow(
    selectedPeriod: TimePeriod,
    startDate: LocalDate,
    endDate: LocalDate,
    txCount: Int,
    isPremium: Boolean,
    context: android.content.Context,
    dateFmt: java.time.format.DateTimeFormatter,
    onPeriodSelected: (TimePeriod) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onShowSubscriptionDialog: () -> Unit
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
    var showMenu by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Period dropdown chip
        Box {
            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(primaryColor.copy(alpha = 0.10f))
                    .clickable(
                        indication = null,
                        interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
                    ) { showMenu = true }
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = selectedPeriod.getDisplayName().uppercase(),
                    fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                    color = primaryColor, letterSpacing = 1.sp
                )
                Icon(
                    painter = painterResource(R.drawable.arrow_downward),
                    contentDescription = "Select period",
                    tint = primaryColor, modifier = Modifier.size(11.dp)
                )
            }
            DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                periodOptions.forEach { period ->
                    val requiresPremium = !isPremium && (period == TimePeriod.LAST_MONTH || period == TimePeriod.THIS_YEAR || period == TimePeriod.ENTIRE)
                    DropdownMenuItem(
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = period.getDisplayName(),
                                    fontSize = 14.sp,
                                    fontWeight = if (period == selectedPeriod) FontWeight.Bold else FontWeight.Normal,
                                    color = if (period == selectedPeriod) primaryColor else MaterialTheme.colorScheme.onSurface
                                )
                                if (requiresPremium) {
                                    Icon(
                                        painter = painterResource(R.drawable.lock),
                                        contentDescription = "Premium",
                                        modifier = Modifier.size(12.dp),
                                        tint = MaterialTheme.colorScheme.tertiary
                                    )
                                }
                            }
                        },
                        onClick = {
                            showMenu = false
                            if (requiresPremium) onShowSubscriptionDialog()
                            else onPeriodSelected(period)
                        }
                    )
                }
                HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                // Custom date pickers
                DropdownMenuItem(
                    text = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(painter = painterResource(R.drawable.calendar), contentDescription = null, modifier = Modifier.size(14.dp), tint = primaryColor)
                            Text("${startDate.format(dateFmt)}  →  ${endDate.format(dateFmt)}", fontSize = 13.sp)
                        }
                    },
                    onClick = {
                        showMenu = false
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> onStartDateChange(LocalDate.of(y, m + 1, d)) },
                            startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
                        ).show()
                    }
                )
            }
        }
        Text(
            "${txCount} tx",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.weight(1f))
        // Current date range as chips
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.clickable {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onStartDateChange(LocalDate.of(y, m + 1, d)) },
                    startDate.year, startDate.monthValue - 1, startDate.dayOfMonth
                ).show()
            }
        ) {
            Text(startDate.format(dateFmt), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
        Text("→", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer,
            modifier = Modifier.clickable {
                DatePickerDialog(
                    context,
                    { _, y, m, d -> onEndDateChange(LocalDate.of(y, m + 1, d)) },
                    endDate.year, endDate.monthValue - 1, endDate.dayOfMonth
                ).show()
            }
        ) {
            Text(endDate.format(dateFmt), fontSize = 11.sp, fontWeight = FontWeight.Medium, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
        }
    }
}

@Composable
private fun MpesaTxRow(tx: Transaction, onClick: () -> Unit = {}) {
    val isIn = tx.transactionAmount > 0
    val displayName = tx.entity.replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(tx.entity)
    val amountColor = if (isIn) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error

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
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(amountColor))
            Text(
                text = "${if (isIn) "+" else "-"}Ksh ${String.format("%,.0f", tx.transactionAmount.absoluteValue)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun ManualTxRow(tx: ManualTransaction, onEdit: () -> Unit = {}, onClick: () -> Unit = {}) {
    val amountColor = if (tx.isOutflow) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
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
            Box(modifier = Modifier.size(44.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                val initials = tx.memberName.trim().split(" ")
                    .mapNotNull { it.firstOrNull()?.uppercase() }
                    .take(2).joinToString("").ifEmpty { tx.memberName.take(2).uppercase() }
                Text(initials, fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
            // Small "edit" badge to mark as manual
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
                        text = tx.transactionTypeName,
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
                text = "${if (tx.isOutflow) "-" else "+"}Ksh ${String.format("%,.0f", tx.amount)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}
