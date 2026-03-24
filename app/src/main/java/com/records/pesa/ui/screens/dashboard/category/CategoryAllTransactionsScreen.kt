package com.records.pesa.ui.screens.dashboard.category

import android.util.Log
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
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
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

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
    val isLoading: Boolean = true
)

class CategoryAllTransactionsScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryAllTransactionsUiState())
    val uiState: StateFlow<CategoryAllTransactionsUiState> = _uiState.asStateFlow()

    private val categoryIdInt: Int = savedStateHandle.get<String>(CategoryAllTransactionsScreenDestination.categoryId)
        ?.toIntOrNull() ?: 0

    init {
        loadData()
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
        onNavigateBack = navigateToPreviousScreen,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryAllTransactionsScreen(
    uiState: CategoryAllTransactionsUiState,
    onFilterChange: (CombinedFilter) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")

    val combined: List<CombinedTransactionItem> = when (uiState.filter) {
        CombinedFilter.ALL -> {
            val mpesa = uiState.mpesaItems.map { CombinedTransactionItem.MpesaItem(it) }
            val manual = uiState.manualItems.map { CombinedTransactionItem.ManualItem(it) }
            (mpesa + manual).sortedByDescending { item: CombinedTransactionItem ->
                when (item) {
                    is CombinedTransactionItem.MpesaItem -> item.tx.date.toString()
                    is CombinedTransactionItem.ManualItem -> item.tx.date.toString()
                }
            }
        }
        CombinedFilter.MPESA -> uiState.mpesaItems.map { CombinedTransactionItem.MpesaItem(it) }
        CombinedFilter.MANUAL -> uiState.manualItems.map { CombinedTransactionItem.ManualItem(it) }
    }

    Column(modifier.fillMaxSize()) {
        Row(
            Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onNavigateBack) {
                Icon(painter = painterResource(R.drawable.arrow_back), contentDescription = "Back")
            }
            Column {
                Text("All Transactions", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                if (uiState.categoryName.isNotBlank()) {
                    Text(uiState.categoryName, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        Row(
            Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            CombinedFilter.values().forEach { f ->
                FilterChip(
                    selected = uiState.filter == f,
                    onClick = { onFilterChange(f) },
                    label = { Text(f.name.lowercase().replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (combined.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No transactions found", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            LazyColumn(Modifier.fillMaxSize()) {
                items(combined) { item ->
                    when (item) {
                        is CombinedTransactionItem.MpesaItem -> {
                            val tx = item.tx
                            ElevatedCard(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(Modifier.weight(1f)) {
                                        Text(tx.entity ?: "", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(tx.transactionType, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(tx.date.toString(), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "KES ${tx.transactionAmount.toLong()}",
                                        color = if (tx.transactionType.contains("received", true) || tx.transactionType.contains("deposit", true)) Color(0xFF388E3C) else Color(0xFFD32F2F),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                        is CombinedTransactionItem.ManualItem -> {
                            val tx = item.tx
                            ElevatedCard(
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                            ) {
                                Row(
                                    Modifier.fillMaxWidth().padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.edit),
                                        contentDescription = "Manual",
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(4.dp))
                                    Column(Modifier.weight(1f)) {
                                        Text(tx.memberName, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                                        Text(tx.transactionTypeName, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        Text(tx.date.format(dateFormatter), fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    Text(
                                        "KES ${tx.amount.toLong()}",
                                        color = if (tx.isOutflow) Color(0xFFD32F2F) else Color(0xFF388E3C),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
                item { Spacer(Modifier.height(32.dp)) }
            }
        }
    }
}
