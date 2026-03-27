package com.records.pesa.ui.screens.dashboard.budget

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.BudgetCycleLog
import com.records.pesa.nav.AppNavigation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

object BudgetCycleHistoryScreenDestination : AppNavigation {
    override val title = "Cycle History"
    override val route = "budget-cycle-history-screen"
    const val budgetId = "budgetId"
    val routeWithArgs = "$route/{$budgetId}"
}

data class BudgetCycleHistoryUiState(
    val budgetId: Int = 0,
    val budgetName: String = "",
    val cycles: List<BudgetCycleLog> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetCycleHistoryScreenViewModel(
    savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository
) : ViewModel() {

    private val budgetIdArg: Int =
        savedStateHandle.get<String>(BudgetCycleHistoryScreenDestination.budgetId)?.toIntOrNull() ?: 0

    private val _uiState = MutableStateFlow(BudgetCycleHistoryUiState())
    val uiState: StateFlow<BudgetCycleHistoryUiState> = _uiState.asStateFlow()

    init {
        _uiState.update { it.copy(budgetId = budgetIdArg) }
        viewModelScope.launch {
            dbRepository.getBudgetById(budgetIdArg).collect { budget ->
                _uiState.update { it.copy(budgetName = budget?.name ?: "", isLoading = false) }
            }
        }
        viewModelScope.launch {
            dbRepository.getBudgetCycleLogs(budgetIdArg).collect { logs ->
                _uiState.update { it.copy(cycles = logs.sortedByDescending { it.cycleNumber }) }
            }
        }
    }
}

@Composable
fun BudgetCycleHistoryScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToBudgetTransactions: (budgetId: Int, startDate: String, endDate: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val viewModel: BudgetCycleHistoryScreenViewModel =
        viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    Box(modifier = modifier.safeDrawingPadding()) {
        BudgetCycleHistoryScreen(
            uiState = uiState,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToBudgetTransactions = navigateToBudgetTransactions
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BudgetCycleHistoryScreen(
    uiState: BudgetCycleHistoryUiState,
    navigateToPreviousScreen: () -> Unit,
    navigateToBudgetTransactions: (budgetId: Int, startDate: String, endDate: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val monthFormatter = remember { DateTimeFormatter.ofPattern("MMMM yyyy") }
    val grouped = remember(uiState.cycles) {
        uiState.cycles
            .groupBy { it.cycleEndDate.format(monthFormatter) }
            .entries
            .sortedByDescending { it.value.first().cycleEndDate }
    }

    Column(modifier = modifier.fillMaxSize()) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scaleX = -1f, scaleY = 1f),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                    Text(
                        text = "Cycle History",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (uiState.budgetName.isNotBlank()) {
                        Text(
                            text = uiState.budgetName,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (uiState.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        if (uiState.cycles.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No completed cycles yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            return
        }

        LazyColumn(contentPadding = PaddingValues(bottom = 24.dp)) {
            item {
                Text(
                    text = "${uiState.cycles.size} completed cycle${if (uiState.cycles.size != 1) "s" else ""}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                )
            }

            grouped.forEach { (monthLabel, cyclesInMonth) ->
                stickyHeader(key = "header_$monthLabel") {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = monthLabel,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                        )
                    }
                }
                items(cyclesInMonth, key = { it.id }) { log ->
                    BudgetCycleLogCard(
                        log = log,
                        onClick = {
                            navigateToBudgetTransactions(
                                uiState.budgetId,
                                log.cycleStartDate.toString(),
                                log.cycleEndDate.toString()
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
