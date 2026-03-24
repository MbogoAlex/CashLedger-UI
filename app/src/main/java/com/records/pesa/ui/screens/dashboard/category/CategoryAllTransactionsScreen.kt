package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

object CategoryAllTransactionsScreenDestination : AppNavigation {
    override val title = "All Transactions"
    override val route = "category-all-transactions-screen"
    const val categoryId = "categoryId"
    val routeWithArgs = "$route/{$categoryId}"
}

enum class CombinedFilter { ALL, MPESA, MANUAL }

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
    val isLoading: Boolean = true
)

class CategoryAllTransactionsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val categoryService: CategoryService
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryAllTransactionsUiState())
    val uiState: StateFlow<CategoryAllTransactionsUiState> = _uiState.asStateFlow()

    private val categoryIdInt: Int = savedStateHandle.get<String>(CategoryAllTransactionsScreenDestination.categoryId)
        ?.toIntOrNull() ?: 0

    init {
        loadCategoryName()
        loadData()
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

    fun setSearchText(text: String) {
        _uiState.update { it.copy(searchText = text) }
    }
}

@Composable
fun CategoryAllTransactionsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: CategoryAllTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    CategoryAllTransactionsScreen(
        uiState = uiState,
        onFilterChange = { viewModel.setFilter(it) },
        onSearchTextChange = { viewModel.setSearchText(it) },
        onNavigateBack = navigateToPreviousScreen,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CategoryAllTransactionsScreen(
    uiState: CategoryAllTransactionsUiState,
    onFilterChange: (CombinedFilter) -> Unit,
    onSearchTextChange: (String) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else { keyboard?.hide(); onSearchTextChange("") }
    }

    val filtered: List<CombinedTransactionItem> = remember(uiState) {
        val search = uiState.searchText
        val mpesaList = uiState.mpesaItems.let { list ->
            if (search.isBlank()) list
            else list.filter { tx ->
                tx.entity?.contains(search, ignoreCase = true) == true ||
                tx.transactionType.contains(search, ignoreCase = true)
            }
        }.map { CombinedTransactionItem.MpesaItem(it) }

        val manualList = uiState.manualItems.let { list ->
            if (search.isBlank()) list
            else list.filter { tx ->
                tx.memberName.contains(search, ignoreCase = true) ||
                tx.transactionTypeName.contains(search, ignoreCase = true)
            }
        }.map { CombinedTransactionItem.ManualItem(it) }

        when (uiState.filter) {
            CombinedFilter.ALL -> (mpesaList + manualList)
            CombinedFilter.MPESA -> mpesaList
            CombinedFilter.MANUAL -> manualList
        }
    }

    val groupedByDate = remember(filtered) {
        filtered.groupBy { item ->
            when (item) {
                is CombinedTransactionItem.MpesaItem -> item.tx.date.toString()
                is CombinedTransactionItem.ManualItem -> item.tx.date.toString()
            }
        }.entries.sortedByDescending { it.key }
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
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            if (uiState.categoryName.isNotBlank()) {
                                Text(
                                    uiState.categoryName,
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

            // Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
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
                                is CombinedTransactionItem.MpesaItem -> {
                                    MpesaTxRow(tx = item.tx)
                                }
                                is CombinedTransactionItem.ManualItem -> {
                                    ManualTxRow(tx = item.tx)
                                }
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

@Composable
private fun MpesaTxRow(tx: Transaction) {
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
private fun ManualTxRow(tx: ManualTransaction) {
    val amountColor = if (tx.isOutflow) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
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
                        text = tx.transactionTypeName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = "· ${tx.date.format(dateFormatter)}",
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
