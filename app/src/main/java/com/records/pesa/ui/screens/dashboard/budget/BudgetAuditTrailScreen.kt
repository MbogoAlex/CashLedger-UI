package com.records.pesa.ui.screens.dashboard.budget

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.DBRepository
import com.records.pesa.db.models.BudgetRecalcLog
import com.records.pesa.datastore.DataStoreRepository
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.utils.screenFontSize
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

object BudgetAuditTrailScreenDestination : AppNavigation {
    override val title = "Budget Audit Trail"
    override val route = "budget-audit-trail-screen"
    const val budgetId = "budgetId"
    val routeWithArgs = "$route/{$budgetId}"
}

data class BudgetAuditTrailUiState(
    val isPremium: Boolean = false,
    val budgetId: Int = 0,
    val budgetName: String = "",
    val budgetStartDate: String? = null,
    val budgetEndDate: String? = null,
    val logs: List<BudgetRecalcLog> = emptyList(),
    val isLoading: Boolean = true
)

class BudgetAuditTrailScreenViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val dbRepository: DBRepository,
    private val dataStoreRepository: DataStoreRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetAuditTrailUiState())
    val uiState: StateFlow<BudgetAuditTrailUiState> = _uiState.asStateFlow()

    private val budgetIdArg: Int = savedStateHandle.get<String>(BudgetAuditTrailScreenDestination.budgetId)?.toIntOrNull() ?: 0

    init {
        _uiState.update { it.copy(budgetId = budgetIdArg) }
        viewModelScope.launch {
            dataStoreRepository.getUserPreferences().collect { prefs ->
                val isPremium = prefs.permanent ||
                    (prefs.expiryDate != null && prefs.expiryDate.isAfter(java.time.LocalDateTime.now()))
                _uiState.update { it.copy(isPremium = isPremium) }
            }
        }
        viewModelScope.launch {
            dbRepository.getBudgetById(budgetIdArg).collect { budget ->
                _uiState.update {
                    it.copy(
                        budgetName = budget?.name ?: "",
                        budgetStartDate = budget?.startDate?.toString(),
                        budgetEndDate = budget?.limitDate?.toString(),
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            dbRepository.getLogsForBudget(budgetIdArg).collect { logs ->
                _uiState.update { it.copy(logs = logs) }
            }
        }
    }
}

@Composable
fun BudgetAuditTrailScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToBudgetTransactions: (budgetId: Int, startDate: String, endDate: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    val viewModel: BudgetAuditTrailScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    BudgetAuditTrailScreen(
        uiState = uiState,
        navigateToPreviousScreen = navigateToPreviousScreen,
        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
        navigateToBudgetTransactions = navigateToBudgetTransactions,
        modifier = modifier
    )
}

@Composable
fun BudgetAuditTrailScreen(
    uiState: BudgetAuditTrailUiState,
    navigateToPreviousScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToBudgetTransactions: (budgetId: Int, startDate: String, endDate: String) -> Unit = { _, _, _ -> },
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .safeDrawingPadding()
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
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
                        text = "Audit Trail",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
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
        HorizontalDivider()

        if (!uiState.isPremium) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFFFFF8E1),
                                        Color(0xFFFFF3E0)
                                    )
                                )
                            )
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Premium Feature",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF5D4037)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Track every recalculation and see exactly when your spending crossed thresholds. Upgrade to unlock.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF795548),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Button(
                                onClick = navigateToSubscriptionScreen,
                                shape = RoundedCornerShape(50.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFA000)
                                ),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.star),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Upgrade to Premium", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        } else if (uiState.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val changedLogs = remember(uiState.logs) {
                uiState.logs.filter { it.oldExpenditure != it.newExpenditure }
            }
            if (changedLogs.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            painter = painterResource(R.drawable.list),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No changes recorded yet",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Entries appear here when spending changes",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(changedLogs) { log ->
                        val start = log.cycleStartDate ?: uiState.budgetStartDate
                        val end = log.cycleEndDate ?: uiState.budgetEndDate
                        AuditLogEntry(
                            log = log,
                            onClick = if (start != null && end != null) {
                                { navigateToBudgetTransactions(uiState.budgetId, start, end) }
                            } else null
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogEntry(
    log: BudgetRecalcLog,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm")
    val increased = log.newExpenditure > log.oldExpenditure
    Card(
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.refresh),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = log.timestamp.format(formatter),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                if (log.thresholdCrossed != null) {
                    Surface(
                        shape = RoundedCornerShape(50.dp),
                        color = if (log.thresholdCrossed == "100%") Color(0xFFFFEBEE) else Color(0xFFFFF8E1)
                    ) {
                        Text(
                            text = "⚠ ${log.thresholdCrossed} crossed",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (log.thresholdCrossed == "100%") Color(0xFFD32F2F) else Color(0xFFF57F17),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    painter = painterResource(
                        if (increased) R.drawable.ic_arrow_right else R.drawable.ic_arrow_right
                    ),
                    contentDescription = null,
                    tint = if (increased) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier
                        .size(14.dp)
                        .then(
                            if (!increased) Modifier.scale(scaleX = 1f, scaleY = -1f) else Modifier
                        )
                )
                Text(
                    text = "KES ${String.format("%,.2f", log.oldExpenditure)} → KES ${String.format("%,.2f", log.newExpenditure)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = if (increased) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                )
            }
            if (onClick != null) {
                HorizontalDivider(
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                    modifier = Modifier.padding(top = 2.dp)
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.End,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "View transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Spacer(Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
    }
}
