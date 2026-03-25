package com.records.pesa.ui.screens.dashboard.budget

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
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.nav.AppNavigation
import com.records.pesa.service.category.CategoryService
import com.records.pesa.ui.screens.components.TxDateHeader
import com.records.pesa.ui.screens.components.TxEmptyState
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.dashboard.category.CombinedFilter
import com.records.pesa.ui.screens.dashboard.category.CombinedTransactionItem
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

// ─── Navigation ──────────────────────────────────────────────────────────────
object BudgetAllTransactionsScreenDestination : AppNavigation {
    override val title = "Budget Transactions"
    override val route = "budget-all-transactions-screen"
    const val budgetId = "budgetId"
    val routeWithArgs = "$route/{$budgetId}"
}

// ─── UiState ─────────────────────────────────────────────────────────────────
data class BudgetAllTransactionsUiState(
    val budgetName: String = "",
    val categoryId: Int? = null,
    val startDate: LocalDate = LocalDate.now().minusMonths(1),
    val endDate: LocalDate = LocalDate.now(),
    val mpesaItems: List<Transaction> = emptyList(),
    val manualItems: List<ManualTransaction> = emptyList(),
    val memberNames: List<String> = emptyList(),   // empty = all members
    val filter: CombinedFilter = CombinedFilter.ALL,
    val searchText: String = "",
    val isLoading: Boolean = true
)

// ─── ViewModel ───────────────────────────────────────────────────────────────
class BudgetAllTransactionsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetAllTransactionsUiState())
    val uiState: StateFlow<BudgetAllTransactionsUiState> = _uiState.asStateFlow()

    private val budgetIdInt: Int = savedStateHandle.get<String>(BudgetAllTransactionsScreenDestination.budgetId)
        ?.toIntOrNull() ?: 0

    init {
        loadBudget()
    }

    private fun loadBudget() {
        viewModelScope.launch {
            try {
                dbRepository.getBudgetById(budgetIdInt).filterNotNull().collectLatest { budget ->
                    val members = dbRepository.getBudgetMembersOnce(budget.id).map { it.memberName }
                    _uiState.update {
                        it.copy(
                            budgetName = budget.name,
                            categoryId = budget.categoryId,
                            startDate = budget.startDate,
                            endDate = budget.limitDate,
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

    fun setStartDate(date: LocalDate) = _uiState.update { it.copy(startDate = date) }
    fun setEndDate(date: LocalDate) = _uiState.update { it.copy(endDate = date) }
    fun setFilter(filter: CombinedFilter) = _uiState.update { it.copy(filter = filter) }
    fun setSearchText(text: String) = _uiState.update { it.copy(searchText = text) }
}

// ─── Composable entry point ───────────────────────────────────────────────────
@Composable
fun BudgetAllTransactionsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: BudgetAllTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    BudgetAllTransactionsScreen(
        uiState = uiState,
        onFilterChange = viewModel::setFilter,
        onSearchTextChange = viewModel::setSearchText,
        onStartDateChange = viewModel::setStartDate,
        onEndDateChange = viewModel::setEndDate,
        onNavigateBack = navigateToPreviousScreen,
        modifier = modifier
    )
}

// ─── Main screen ─────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun BudgetAllTransactionsScreen(
    uiState: BudgetAllTransactionsUiState,
    onFilterChange: (CombinedFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onStartDateChange: (LocalDate) -> Unit,
    onEndDateChange: (LocalDate) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    val dateFmt = remember { DateTimeFormatter.ofPattern("d MMM yy") }

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else { keyboard?.hide(); onSearchTextChange("") }
    }

    // Filter + search
    val filtered: List<CombinedTransactionItem> = remember(uiState) {
        val search = uiState.searchText
        val start = uiState.startDate
        val end = uiState.endDate
        val members = uiState.memberNames

        val mpesaList = uiState.mpesaItems
            .filter { tx ->
                tx.transactionAmount < 0 &&   // expenses only
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
            CombinedFilter.ALL -> mpesaList + manualList
            CombinedFilter.MPESA -> mpesaList
            CombinedFilter.MANUAL -> manualList
        }
    }

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

    Box(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top bar ───────────────────────────────────────────────────────
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
                        if (showSearch) showSearch = false else onNavigateBack()
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.budgetName.isNotBlank()) {
                                Text(
                                    text = uiState.budgetName,
                                    fontSize = 13.sp,
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
                }
            }

            // ── Date range row ────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(16.dp)
                )
                DateChip(
                    label = uiState.startDate.format(dateFmt),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> onStartDateChange(LocalDate.of(y, m + 1, d)) },
                            uiState.startDate.year,
                            uiState.startDate.monthValue - 1,
                            uiState.startDate.dayOfMonth
                        ).show()
                    }
                )
                Text("→", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                DateChip(
                    label = uiState.endDate.format(dateFmt),
                    onClick = {
                        DatePickerDialog(
                            context,
                            { _, y, m, d -> onEndDateChange(LocalDate.of(y, m + 1, d)) },
                            uiState.endDate.year,
                            uiState.endDate.monthValue - 1,
                            uiState.endDate.dayOfMonth
                        ).show()
                    }
                )
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${filtered.size} txn${if (filtered.size != 1) "s" else ""}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // ── Filter chips ──────────────────────────────────────────────────
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                CombinedFilter.values().forEach { f ->
                    FilterChip(
                        selected = uiState.filter == f,
                        onClick = { onFilterChange(f) },
                        label = {
                            Text(
                                when (f) {
                                    CombinedFilter.ALL -> "All"
                                    CombinedFilter.MPESA -> "M-PESA"
                                    CombinedFilter.MANUAL -> "Manual"
                                }
                            )
                        }
                    )
                }
            }

            // ── Content ───────────────────────────────────────────────────────
            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (filtered.isEmpty()) {
                TxEmptyState(
                    message = if (uiState.searchText.isNotBlank())
                        "No transactions matching \"${uiState.searchText}\""
                    else "No expenses in this date range"
                )
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
                            when (item) {
                                is CombinedTransactionItem.MpesaItem -> BudgetAllMpesaTxRow(tx = item.tx)
                                is CombinedTransactionItem.ManualItem -> BudgetAllManualTxRow(tx = item.tx)
                            }
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
}

// ─── Date chip ───────────────────────────────────────────────────────────────
@Composable
private fun DateChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.secondaryContainer,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

// ─── Row composables ─────────────────────────────────────────────────────────
@Composable
private fun BudgetAllMpesaTxRow(tx: Transaction) {
    val displayName = tx.entity.replaceFirstChar { it.uppercase() }
    val initials = displayName.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { displayName.take(2).uppercase() }
    val avatarColor = txAvatarColor(tx.entity)
    val amountColor = MaterialTheme.colorScheme.error

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                text = "-Ksh ${String.format("%,.0f", tx.transactionAmount.absoluteValue)}",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = amountColor
            )
        }
    }
}

@Composable
private fun BudgetAllManualTxRow(tx: ManualTransaction) {
    val amountColor = MaterialTheme.colorScheme.error
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    Row(
        modifier = Modifier
            .fillMaxWidth()
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
                    .align(Alignment.BottomEnd),
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
