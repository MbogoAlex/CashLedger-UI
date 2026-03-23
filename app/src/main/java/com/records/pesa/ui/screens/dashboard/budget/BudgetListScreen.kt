package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DismissDirection
import androidx.compose.material.DismissValue
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.SwipeToDismiss
import androidx.compose.material.rememberDismissState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.models.Budget
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.nav.AppNavigation
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.launch
import java.time.format.DateTimeFormatter

object BudgetListScreenDestination : AppNavigation {
    override val title: String = "Budget list screen"
    override val route: String = "budget-list-screen"
    val categoryId: String = "categoryId"
    val categoryName: String = "categoryName"
    val routeWithArgs = "$route/{$categoryId}/{$categoryName}"
}

private fun budgetStatusColor(status: BudgetStatus): Color = when (status) {
    BudgetStatus.ON_TRACK -> Color(0xFF388E3C)
    BudgetStatus.WARNING  -> Color(0xFFFFA000)
    BudgetStatus.EXCEEDED -> Color(0xFFD32F2F)
    BudgetStatus.EXPIRED  -> Color(0xFF9E9E9E)
}

private fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.ON_TRACK -> "On Track"
    BudgetStatus.WARNING  -> "Warning"
    BudgetStatus.EXCEEDED -> "Exceeded"
    BudgetStatus.EXPIRED  -> "Expired"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BudgetListScreenComposable(
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit = {},
    showBackArrow: Boolean,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = {
        if (showBackArrow) navigateToPreviousScreen() else navigateToHomeScreen()
    })

    val viewModel: BudgetListScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
    }

    Box(modifier = modifier.safeDrawingPadding()) {
        BudgetListScreen(
            budgets = viewModel.filteredBudgets,
            searchQuery = uiState.searchQuery,
            categoryId = uiState.categoryId,
            categoryName = uiState.categoryName,
            sortBy = uiState.sortBy,
            filterStatus = uiState.filterStatus,
            isPremium = uiState.isPremium,
            activeCount = uiState.activeCount,
            totalExceeded = uiState.totalExceeded,
            totalOverBudgetAmount = uiState.totalOverBudgetAmount,
            onChangeSearchQuery = { viewModel.updateSearchQuery(it) },
            onClearSearch = { viewModel.clearSearch() },
            onUpdateSortBy = { viewModel.updateSortBy(it) },
            onUpdateFilterStatus = { viewModel.updateFilterStatus(it) },
            onDeleteBudget = { viewModel.deleteBudget(it) },
            onUndoDelete = { viewModel.undoDelete(it) },
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
            navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
            navigateToPreviousScreen = navigateToPreviousScreen,
            showBackArrow = showBackArrow
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BudgetListScreen(
    budgets: List<BudgetWithProgress>,
    searchQuery: String,
    categoryId: String?,
    categoryName: String?,
    sortBy: String,
    filterStatus: String,
    isPremium: Boolean,
    activeCount: Int,
    totalExceeded: Int,
    totalOverBudgetAmount: Double,
    onChangeSearchQuery: (String) -> Unit,
    onClearSearch: () -> Unit,
    onUpdateSortBy: (String) -> Unit,
    onUpdateFilterStatus: (String) -> Unit,
    onDeleteBudget: (Budget) -> Unit,
    onUndoDelete: (Budget) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    showBackArrow: Boolean,
    modifier: Modifier = Modifier
) {
    var searchingOn by rememberSaveable { mutableStateOf(false) }
    var showPremiumGateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    if (showPremiumGateDialog) {
        AlertDialog(
            onDismissRequest = { showPremiumGateDialog = false },
            icon = {
                Icon(
                    painter = painterResource(R.drawable.star),
                    contentDescription = null,
                    tint = Color(0xFFFFA000)
                )
            },
            title = { Text("Premium Feature") },
            text = {
                Text("Free accounts can only have 1 active budget. Upgrade to Premium to create unlimited budgets, get AI-powered insights, and more.")
            },
            confirmButton = {
                TextButton(onClick = { showPremiumGateDialog = false }) { Text("Upgrade") }
            },
            dismissButton = {
                TextButton(onClick = { showPremiumGateDialog = false }) { Text("Not now") }
            }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPremium && activeCount >= 1) {
                        showPremiumGateDialog = true
                    } else {
                        if (categoryId == null) navigateToBudgetCreationScreen()
                        else navigateToBudgetCreationScreenWithCategoryId(categoryId)
                    }
                }
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_add),
                    contentDescription = "Add budget"
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(horizontal = screenWidth(x = 16.0))
                .fillMaxSize()
        ) {
            // Top bar
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (showBackArrow) {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_back),
                            contentDescription = "Previous screen",
                            modifier = Modifier.size(screenWidth(x = 24.0))
                        )
                    }
                }
                if (searchingOn) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        TextField(
                            leadingIcon = {
                                Icon(
                                    painter = painterResource(R.drawable.list),
                                    contentDescription = null,
                                    modifier = Modifier.size(screenWidth(x = 24.0))
                                )
                            },
                            value = searchQuery,
                            placeholder = { Text("Budget / Category") },
                            trailingIcon = {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.inverseOnSurface)
                                        .padding(screenWidth(x = 5.0))
                                        .clickable { onClearSearch() }
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.remove),
                                        contentDescription = "Clear search",
                                        modifier = Modifier.size(screenWidth(x = 16.0))
                                    )
                                }
                            },
                            colors = TextFieldDefaults.colors(
                                focusedIndicatorColor = Color.Transparent,
                                unfocusedIndicatorColor = Color.Transparent
                            ),
                            keyboardOptions = KeyboardOptions.Default.copy(
                                imeAction = ImeAction.Done,
                                keyboardType = KeyboardType.Text
                            ),
                            onValueChange = onChangeSearchQuery,
                            modifier = Modifier.weight(0.9f)
                        )
                        IconButton(
                            onClick = { searchingOn = false },
                            modifier = Modifier.weight(0.1f)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.remove),
                                contentDescription = "Stop searching",
                                modifier = Modifier.size(screenWidth(x = 24.0))
                            )
                        }
                    }
                } else {
                    if (categoryName != null) {
                        Text(
                            text = "$categoryName budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Text(
                            text = "User budgets",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    if (budgets.isNotEmpty()) {
                        IconButton(onClick = { searchingOn = true }) {
                            Icon(
                                painter = painterResource(R.drawable.list),
                                contentDescription = "Search budgets",
                                modifier = Modifier.size(screenWidth(x = 24.0))
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Summary card
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$activeCount",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Active",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$totalExceeded",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Exceeded",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    VerticalDivider(modifier = Modifier.fillMaxHeight())
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "KES ${formatMoneyValue(totalOverBudgetAmount)}",
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Bold,
                            color = if (totalOverBudgetAmount == 0.0) Color(0xFF388E3C) else Color(0xFFD32F2F)
                        )
                        Text(
                            text = "over limit",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Sort + Filter chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sort group
                FilterChip(
                    selected = sortBy == "default",
                    onClick = { onUpdateSortBy("default") },
                    label = { Text("Default") }
                )
                FilterChip(
                    selected = sortBy == "most_used",
                    onClick = { onUpdateSortBy("most_used") },
                    label = { Text("Most Used") }
                )
                FilterChip(
                    selected = sortBy == "created",
                    onClick = { onUpdateSortBy("created") },
                    label = { Text("Created") }
                )
                Spacer(modifier = Modifier.width(8.dp))
                // Filter group
                FilterChip(
                    selected = filterStatus == "all",
                    onClick = { onUpdateFilterStatus("all") },
                    label = { Text("All") }
                )
                FilterChip(
                    selected = filterStatus == "on_track",
                    onClick = { onUpdateFilterStatus("on_track") },
                    label = { Text("On Track") }
                )
                FilterChip(
                    selected = filterStatus == "warning",
                    onClick = { onUpdateFilterStatus("warning") },
                    label = { Text("Warning") }
                )
                FilterChip(
                    selected = filterStatus == "exceeded",
                    onClick = { onUpdateFilterStatus("exceeded") },
                    label = { Text("Exceeded") }
                )
                FilterChip(
                    selected = filterStatus == "expired",
                    onClick = { onUpdateFilterStatus("expired") },
                    label = { Text("Expired") }
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            if (budgets.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize()
                ) {
                    Text(
                        text = "Create budgets for your transactions. A budget must belong to a category. Create a category first then create a budget for it."
                    )
                }
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(budgets, key = { it.budget.id }) { budgetWithProgress ->
                        val dismissState = rememberDismissState(
                            confirmStateChange = { dismissValue ->
                                if (dismissValue == DismissValue.DismissedToStart ||
                                    dismissValue == DismissValue.DismissedToEnd
                                ) {
                                    val deleted = budgetWithProgress.budget
                                    onDeleteBudget(deleted)
                                    scope.launch {
                                        val result = snackbarHostState.showSnackbar(
                                            message = "Budget deleted",
                                            actionLabel = "Undo"
                                        )
                                        if (result == SnackbarResult.ActionPerformed) {
                                            onUndoDelete(deleted)
                                        }
                                    }
                                    true
                                } else false
                            }
                        )
                        SwipeToDismiss(
                            state = dismissState,
                            directions = setOf(DismissDirection.EndToStart),
                            background = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color(0xFFD32F2F))
                                        .padding(end = 16.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.remove),
                                        contentDescription = "Delete",
                                        tint = Color.White
                                    )
                                }
                            },
                            dismissContent = {
                                BudgetListItem(
                                    budgetWithProgress = budgetWithProgress,
                                    navigateToBudgetInfoScreen = navigateToBudgetInfoScreen
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BudgetListItem(
    budgetWithProgress: BudgetWithProgress,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = budgetStatusColor(budgetWithProgress.status)
    val statusLabel = budgetStatusLabel(budgetWithProgress.status)
    val dateFormatter = DateTimeFormatter.ofPattern("d MMM")

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = screenHeight(x = 10.0))
            .clickable { navigateToBudgetInfoScreen(budgetWithProgress.budget.id.toString()) }
    ) {
        Row(modifier = Modifier.height(IntrinsicSize.Min)) {
            // Left accent bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .fillMaxHeight()
                    .background(statusColor)
            )

            Column(
                modifier = Modifier
                    .padding(12.dp)
                    .weight(1f)
            ) {
                // Row 1: name + status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budgetWithProgress.budget.name,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = statusColor.copy(alpha = 0.15f)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(CircleShape)
                                    .background(statusColor)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = statusLabel,
                                style = MaterialTheme.typography.labelSmall,
                                color = statusColor,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Row 2: spent / limit + percent
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${formatMoneyValue(budgetWithProgress.actualSpending)} / ${formatMoneyValue(budgetWithProgress.budget.budgetLimit)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = "${budgetWithProgress.percentUsed}%",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = statusColor
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { (budgetWithProgress.percentUsed / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = statusColor,
                    trackColor = statusColor.copy(alpha = 0.15f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Row 3: category · ends · days left
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (budgetWithProgress.categoryName.isNotBlank()) {
                        Text(
                            text = "\uD83D\uDCC1 ${budgetWithProgress.categoryName}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "Ends ${budgetWithProgress.budget.limitDate.format(dateFormatter)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (budgetWithProgress.daysLeft > 0) {
                        Text(
                            text = "· ${budgetWithProgress.daysLeft}d left",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else if (budgetWithProgress.status == BudgetStatus.EXPIRED) {
                        Text(
                            text = "· Expired",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF9E9E9E)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetListScreenPreview(modifier: Modifier = Modifier) {
    CashLedgerTheme {
        BudgetListScreen(
            budgets = listOf(
                BudgetWithProgress(
                    budget = Budget(
                        name = "Food Budget",
                        active = true,
                        expenditure = 0.0,
                        budgetLimit = 10000.0,
                        createdAt = java.time.LocalDateTime.now(),
                        startDate = java.time.LocalDate.now().withDayOfMonth(1),
                        limitDate = java.time.LocalDate.now().plusDays(30),
                        limitReached = false,
                        limitReachedAt = null,
                        exceededBy = 0.0,
                        categoryId = 1
                    ),
                    actualSpending = 5000.0,
                    percentUsed = 50,
                    remaining = 5000.0,
                    isOverBudget = false,
                    daysLeft = 15,
                    status = BudgetStatus.ON_TRACK,
                    categoryName = "Food"
                )
            ),
            searchQuery = "",
            categoryId = null,
            categoryName = null,
            sortBy = "default",
            filterStatus = "all",
            isPremium = false,
            activeCount = 1,
            totalExceeded = 0,
            totalOverBudgetAmount = 0.0,
            onChangeSearchQuery = {},
            onClearSearch = {},
            onUpdateSortBy = {},
            onUpdateFilterStatus = {},
            onDeleteBudget = {},
            onUndoDelete = {},
            navigateToBudgetInfoScreen = {},
            navigateToBudgetCreationScreen = {},
            navigateToBudgetCreationScreenWithCategoryId = {},
            navigateToPreviousScreen = {},
            showBackArrow = true
        )
    }
}
