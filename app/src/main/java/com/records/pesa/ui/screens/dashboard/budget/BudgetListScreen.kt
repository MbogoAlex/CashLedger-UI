package com.records.pesa.ui.screens.dashboard.budget

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Button
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    BudgetStatus.WARNING  -> Color(0xFFE65100)
    BudgetStatus.EXCEEDED -> Color(0xFFD32F2F)
    BudgetStatus.EXPIRED  -> Color(0xFF757575)
}

private fun budgetStatusLabel(status: BudgetStatus): String = when (status) {
    BudgetStatus.ON_TRACK -> "On Track"
    BudgetStatus.WARNING  -> "Warning"
    BudgetStatus.EXCEEDED -> "Exceeded"
    BudgetStatus.EXPIRED  -> "Expired"
}

// Status-specific gradient: each card gets a distinct feel
@Composable
private fun budgetCardGradient(status: BudgetStatus): Brush {
    val primary   = MaterialTheme.colorScheme.primary
    val container = MaterialTheme.colorScheme.primaryContainer
    val error     = MaterialTheme.colorScheme.error
    val errorCont = MaterialTheme.colorScheme.errorContainer
    return Brush.linearGradient(
        when (status) {
            BudgetStatus.ON_TRACK -> listOf(primary, container)
            BudgetStatus.WARNING  -> listOf(Color(0xFFBF360C), Color(0xFFE64A19))
            BudgetStatus.EXCEEDED -> listOf(error, errorCont)
            BudgetStatus.EXPIRED  -> listOf(Color(0xFF424242), Color(0xFF757575))
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BudgetListScreenComposable(
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit = {},
    navigateToSubscriptionScreen: () -> Unit = {},
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
            navigateToSubscriptionScreen = navigateToSubscriptionScreen,
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
    navigateToSubscriptionScreen: () -> Unit = {},
    showBackArrow: Boolean,
    modifier: Modifier = Modifier
) {
    var searchingOn by rememberSaveable { mutableStateOf(false) }
    var showPremiumGateDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val primaryColor = MaterialTheme.colorScheme.primary

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
            text = { Text("Free accounts can only have 1 active budget. Upgrade to unlock unlimited budgets, AI insights, and more.") },
            confirmButton = { TextButton(onClick = { showPremiumGateDialog = false; navigateToSubscriptionScreen() }) { Text("Upgrade") } },
            dismissButton = { TextButton(onClick = { showPremiumGateDialog = false }) { Text("Not now") } }
        )
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (!isPremium && activeCount >= 1) showPremiumGateDialog = true
                    else if (categoryId == null) navigateToBudgetCreationScreen()
                    else navigateToBudgetCreationScreenWithCategoryId(categoryId)
                }
            ) {
                Icon(painter = painterResource(R.drawable.ic_add), contentDescription = "Add budget")
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            // ── Fixed top bar ─────────────────────────────────────────────
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
                    if (showBackArrow) {
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
                    }
                    if (searchingOn) {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = onChangeSearchQuery,
                            placeholder = {
                                Text(
                                    "Search budgets…",
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                )
                            },
                            trailingIcon = {
                                IconButton(onClick = { onClearSearch(); searchingOn = false }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Close search",
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                focusedContainerColor = Color.Transparent,
                                unfocusedContainerColor = Color.Transparent
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 4.dp)
                        ) {
                            Text(
                                text = categoryName?.let { "$it Budgets" } ?: "My Budgets",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        IconButton(onClick = { searchingOn = true }) {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = "Search",
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

            // ── Scrollable content ────────────────────────────────────────
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 12.dp, bottom = 80.dp)
            ) {

            // ── Summary card (CatPeriodPicker-style gradient) ─────────────
            item {
                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
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
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                BudgetSumStat(label = "Active", value = "$activeCount")
                                BudgetSumStat(
                                    label = "Exceeded", value = "$totalExceeded",
                                    valueColor = if (totalExceeded > 0) Color(0xFFD32F2F) else null
                                )
                                BudgetSumStat(
                                    label = "Over limit",
                                    value = formatMoneyValue(totalOverBudgetAmount),
                                    valueColor = if (totalOverBudgetAmount > 0.0) Color(0xFFD32F2F) else Color(0xFF388E3C)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = primaryColor.copy(alpha = 0.1f))
                            Spacer(modifier = Modifier.height(12.dp))

                            // Sort pills
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "SORT",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                listOf("Default" to "default", "Most Used" to "most_used", "Created" to "created")
                                    .forEach { (label, key) ->
                                        val selected = sortBy == key
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (selected) primaryColor else primaryColor.copy(alpha = 0.10f))
                                                .clickable(
                                                    indication = null,
                                                    interactionSource = remember { MutableInteractionSource() }
                                                ) { onUpdateSortBy(key) }
                                                .padding(horizontal = 10.dp, vertical = 5.dp)
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (selected) MaterialTheme.colorScheme.onPrimary else primaryColor,
                                                letterSpacing = 0.3.sp
                                            )
                                        }
                                    }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            // Filter pills
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "FILTER",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = primaryColor.copy(alpha = 0.6f),
                                    letterSpacing = 1.sp
                                )
                                listOf(
                                    "All" to "all",
                                    "On Track" to "on_track",
                                    "Warning" to "warning",
                                    "Exceeded" to "exceeded",
                                    "Expired" to "expired"
                                ).forEach { (label, key) ->
                                    val selected = filterStatus == key
                                    val chipColor = when (key) {
                                        "on_track" -> Color(0xFF388E3C)
                                        "warning"  -> Color(0xFFE65100)
                                        "exceeded" -> Color(0xFFD32F2F)
                                        "expired"  -> Color(0xFF757575)
                                        else       -> primaryColor
                                    }
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (selected) chipColor else chipColor.copy(alpha = 0.10f))
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) { onUpdateFilterStatus(key) }
                                            .padding(horizontal = 10.dp, vertical = 5.dp)
                                    ) {
                                        Text(
                                            text = label,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = if (selected) Color.White else chipColor,
                                            letterSpacing = 0.3.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── Free user limit banner ────────────────────────────────────────
            if (!isPremium && activeCount >= 1) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 4.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.6f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Free plan: 1 active budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Text(
                                    text = "Upgrade for unlimited budgets + standalone budgets",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.8f)
                                )
                            }
                            TextButton(
                                onClick = navigateToSubscriptionScreen,
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    "Upgrade →",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                            }
                        }
                    }
                }
            }

            // ── Empty state ──────────────────────────────────────────────────
            if (budgets.isEmpty()) {
                item {
                    if (searchQuery.isNotEmpty()) {
                        // Search returned nothing
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.fillMaxWidth().padding(32.dp)
                        ) {
                            Icon(painter = painterResource(R.drawable.search), contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(12.dp))
                            Text("No budgets match \"$searchQuery\"", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                        }
                    } else {
                        // No budgets at all — entice user
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth().background(
                                    Brush.linearGradient(listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                                        MaterialTheme.colorScheme.tertiary.copy(alpha = 0.07f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.04f)
                                    ))
                                )
                            ) {
                                Column(
                                    modifier = Modifier.fillMaxWidth().padding(24.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.budget_2),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(52.dp)
                                    )
                                    Spacer(Modifier.height(14.dp))
                                    Text(
                                        text = "Take Control of Your Spending",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "Set a budget for any category — rent, groceries, transport — and get notified before you overspend. Know exactly how much you have left, every day.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    // Tips row
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        listOf("📊 Track spending", "🔔 Get alerts", "📅 Set periods").forEach { tip ->
                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(8.dp))
                                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                                                    .padding(horizontal = 8.dp, vertical = 5.dp)
                                            ) {
                                                Text(tip, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Medium)
                                            }
                                        }
                                    }
                                    Spacer(Modifier.height(20.dp))
                                    Button(
                                        onClick = navigateToBudgetCreationScreen,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(8.dp))
                                        Text("Create Your First Budget", fontWeight = FontWeight.SemiBold)
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Text(
                                        text = "💡 Tip: Create a category first, then set a budget for it.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ── Budget items ─────────────────────────────────────────────────
            items(budgets, key = { it.budget.id }) { budgetWithProgress ->
                val dismissState = rememberDismissState(
                    confirmStateChange = { value ->
                        if (value == DismissValue.DismissedToStart) {
                            val deleted = budgetWithProgress.budget
                            onDeleteBudget(deleted)
                            scope.launch {
                                val result = snackbarHostState.showSnackbar(
                                    message = "\"${deleted.name}\" deleted",
                                    actionLabel = "Undo"
                                )
                                if (result == SnackbarResult.ActionPerformed) onUndoDelete(deleted)
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
                                .padding(horizontal = 16.dp, vertical = 6.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color(0xFFD32F2F)),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.remove),
                                contentDescription = "Delete",
                                tint = Color.White,
                                modifier = Modifier.padding(end = 20.dp)
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
        } // end LazyColumn
        } // end Column
    }
}

// ─── Summary stat chip ────────────────────────────────────────────────────────
@Composable
private fun BudgetSumStat(
    label: String,
    value: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(primary.copy(alpha = 0.10f))
            .padding(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = valueColor ?: primary,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Individual budget card ────────────────────────────────────────────────────
@Composable
fun BudgetListItem(
    budgetWithProgress: BudgetWithProgress,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val gradient    = budgetCardGradient(budgetWithProgress.status)
    val statusColor = budgetStatusColor(budgetWithProgress.status)
    val statusLabel = budgetStatusLabel(budgetWithProgress.status)
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }

    val animatedProgress by animateFloatAsState(
        targetValue = (budgetWithProgress.percentUsed / 100f).coerceIn(0f, 1f),
        animationSpec = tween(600),
        label = "budget_list_progress"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { navigateToBudgetInfoScreen(budgetWithProgress.budget.id.toString()) }
    ) {
        Box(modifier = Modifier.fillMaxWidth().background(gradient)) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                // Row 1: name + status pill
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budgetWithProgress.budget.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.18f))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(statusColor)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = statusLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Amount spent
                Text(
                    text = formatMoneyValue(budgetWithProgress.actualSpending),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary
                )
                Text(
                    text = "of ${formatMoneyValue(budgetWithProgress.budget.budgetLimit)} limit · ${budgetWithProgress.percentUsed}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = MaterialTheme.colorScheme.onPrimary,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f),
                    strokeCap = StrokeCap.Round
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Footer: category · ends · days
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (budgetWithProgress.categoryName.isNotBlank()) {
                            Text(
                                text = budgetWithProgress.categoryName,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f),
                                maxLines = 1
                            )
                            Text(
                                text = "·",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.5f)
                            )
                        }
                        Text(
                            text = "Ends ${budgetWithProgress.budget.limitDate.format(dateFormatter)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
                        )
                    }
                    Text(
                        text = when {
                            budgetWithProgress.status == BudgetStatus.EXPIRED -> "Expired"
                            budgetWithProgress.daysLeft == 0 -> "Last day"
                            else -> "${budgetWithProgress.daysLeft}d left"
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
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
