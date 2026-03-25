package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Checkbox
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.ui.draw.scale
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
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
import com.records.pesa.db.models.Transaction
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.components.EditManualTransactionDialog
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.ExecutionStatus
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.dashboard.category.InsightItem
import com.records.pesa.ui.screens.dashboard.category.TrendPoint
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.ceil

// ─── Navigation destination ───────────────────────────────────────────────────
object BudgetInfoScreenDestination : AppNavigation {
    override val title: String = "Budget info"
    override val route: String = "budget-info-screen"
    const val budgetId = "budgetId"
    val routeWithArgs = "${route}/{${budgetId}}"
}

// ─── Top-level wiring composable ─────────────────────────────────────────────
@Composable
fun BudgetInfoScreenComposable(
    navigateToBudgetAllTransactions: (budgetId: Int) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToAuditTrail: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: BudgetInfoScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    var showEditDialog   by rememberSaveable { mutableStateOf(false) }
    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var editingManualTx  by remember { mutableStateOf<com.records.pesa.db.models.ManualTransaction?>(null) }

    // Handle save feedback
    LaunchedEffect(uiState.loadingStatus) {
        when (uiState.loadingStatus) {
            LoadingStatus.SUCCESS -> {
                Toast.makeText(context, "Budget updated", Toast.LENGTH_SHORT).show()
                showEditDialog = false
                viewModel.resetLoadingStatus()
            }
            LoadingStatus.FAIL -> {
                Toast.makeText(context, "Update failed. Try again.", Toast.LENGTH_SHORT).show()
                viewModel.resetLoadingStatus()
            }
            else -> {}
        }
    }

    // Handle delete feedback
    LaunchedEffect(uiState.executionStatus) {
        when (uiState.executionStatus) {
            ExecutionStatus.SUCCESS -> {
                Toast.makeText(context, "Budget deleted", Toast.LENGTH_SHORT).show()
                navigateToPreviousScreen()
                viewModel.resetLoadingStatus()
            }
            ExecutionStatus.FAIL -> {
                Toast.makeText(context, "Delete failed. Try again.", Toast.LENGTH_SHORT).show()
                viewModel.resetLoadingStatus()
            }
            else -> {}
        }
    }

    if (showEditDialog) {
        BudgetEditDialog(
            uiState = uiState,
            onNameChange = viewModel::updateBudgetName,
            onLimitChange = viewModel::updateBudgetLimit,
            onDateChange = viewModel::updateLimitDate,
            onThresholdChange = viewModel::updateAlertThreshold,
            onToggleMember = viewModel::toggleEditMember,
            onSave = {
                viewModel.saveBudgetEdits()
                viewModel.saveEditedMembers()
            },
            onDismiss = { showEditDialog = false; viewModel.resetLoadingStatus() }
        )
        LaunchedEffect(showEditDialog) {
            viewModel.loadCategoryMembersForEdit()
        }
    }

    if (showDeleteDialog) {
        BudgetDeleteDialog(
            budgetName = uiState.budget?.name ?: "this budget",
            isDeleting = uiState.executionStatus == ExecutionStatus.LOADING,
            onConfirm = viewModel::deleteBudget,
            onDismiss = { showDeleteDialog = false }
        )
    }

    editingManualTx?.let { tx ->
        val allMembers = (uiState.transactions.map { it.entity.replaceFirstChar { c -> c.uppercase() } } +
                uiState.manualTransactions.map { it.memberName }).distinct().sorted()
        EditManualTransactionDialog(
            tx = tx,
            members = allMembers,
            onSave = { updated -> viewModel.updateManualTransaction(updated); editingManualTx = null },
            onDismiss = { editingManualTx = null }
        )
    }

    Box(modifier = modifier.safeDrawingPadding()) {
        BudgetInfoScreen(
            uiState = uiState,
            onEditClick = { showEditDialog = true },
            onDeleteClick = { showDeleteDialog = true },
            navigateToBudgetAllTransactions = navigateToBudgetAllTransactions,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToAuditTrail = navigateToAuditTrail,
            onEditManualTx = { editingManualTx = it }
        )
    }
}

// ─── Main screen ─────────────────────────────────────────────────────────────
@Composable
fun BudgetInfoScreen(
    uiState: BudgetInfoScreenUiState,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    navigateToBudgetAllTransactions: (budgetId: Int) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToAuditTrail: (Int) -> Unit = {},
    onEditManualTx: (com.records.pesa.db.models.ManualTransaction) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val budget = uiState.budget

    Column(modifier = modifier.fillMaxSize()) {

        // ── Fixed top bar ─────────────────────────────────────────────────
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
                Text(
                    text = budget?.name ?: uiState.budgetName,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 4.dp)
                )
                IconButton(onClick = onEditClick) {
                    Icon(
                        painter = painterResource(R.drawable.edit),
                        contentDescription = "Edit budget",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = onDeleteClick) {
                    Icon(
                        painter = painterResource(R.drawable.remove),
                        contentDescription = "Delete budget",
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ── Scrollable content ────────────────────────────────────────────
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {

        // 2. Hero Stats Card
        item {
            BudgetHeroCard(
                budgetName = budget?.name ?: uiState.budgetName,
                actualSpending = uiState.actualSpending,
                budgetLimit = budget?.budgetLimit ?: 0.0,
                percentUsed = uiState.percentUsed,
                daysLeft = uiState.daysLeft,
                remaining = uiState.remaining,
                dailyAvg = uiState.dailyAvg,
                isOverBudget = uiState.isOverBudget,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 3. Spending Trend Chart
        item {
            SpendingTrendCard(
                trendData = uiState.trendData,
                dailyBudget = uiState.dailyBudget,
                dailyLimitLine = uiState.dailyLimitLine,
                isPremium = uiState.isPremium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 4. Smart Insights Card
        item {
            SmartInsightsCard(
                insights = uiState.insights,
                isPremium = uiState.isPremium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Budget Members Card
        item {
            BudgetMembersCard(
                budgetMembers = uiState.budgetMembers,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // 5. Recent Transactions header
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .fillMaxWidth()
            ) {
                Icon(
                    painter = painterResource(R.drawable.wallet),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Transactions in this budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        // 5. Combined Transactions preview (5 most recent) grouped by date
        val previewLimit = 5
        val allTxDates = (uiState.transactions.map { it.date } + uiState.manualTransactions.map { it.date })
            .distinct().sortedDescending()

        // Collect up to previewLimit rows across all dates
        var rowsShown = 0
        allTxDates.forEach { date ->
            if (rowsShown >= previewLimit) return@forEach
            val mpesaOnDate = uiState.transactions.filter { it.date == date }
                .sortedByDescending { it.time }
            val manualOnDate = uiState.manualTransactions.filter { it.date == date }
                .sortedByDescending { it.time }
            val totalOnDate = mpesaOnDate.size + manualOnDate.size
            if (totalOnDate == 0) return@forEach

            val mpesaSlice = mpesaOnDate.take((previewLimit - rowsShown).coerceAtLeast(0))
            val manualSlice = manualOnDate.take((previewLimit - rowsShown - mpesaSlice.size).coerceAtLeast(0))
            val sliceCount = mpesaSlice.size + manualSlice.size
            if (sliceCount == 0) return@forEach

            item(key = "header-$date") {
                BudgetDateHeader(date = date)
            }
            items(mpesaSlice, key = { "mpesa-${it.id}" }) { tx ->
                BudgetTransactionRow(
                    transaction = tx,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            items(manualSlice, key = { "manual-${it.id}" }) { tx ->
                BudgetManualTransactionRow(
                    tx = tx,
                    onEdit = { onEditManualTx(tx) },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 2.dp)
                )
            }
            rowsShown += sliceCount
        }

        if (uiState.transactions.isEmpty() && uiState.manualTransactions.isEmpty() && uiState.loadingStatus != LoadingStatus.LOADING) {
            item {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp)
                ) {
                    Text(
                        text = "No transactions in this budget period yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // 6. View all transactions button
        if (budget != null && budget.categoryId != null) {
            item {
                Button(
                    onClick = { navigateToBudgetAllTransactions(budget.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.list),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("View All Transactions", fontWeight = FontWeight.SemiBold)
                }
            }
        }

        // Audit Trail entry
        if (budget != null) {
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    onClick = { navigateToAuditTrail(budget.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.list),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "Audit Trail",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (!uiState.isPremium) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = null,
                                    tint = Color(0xFFFFA000),
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Premium",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFA000),
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                            }
                        }
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // 7. Danger Zone
        item {
            Spacer(modifier = Modifier.height(16.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.error.copy(alpha = 0.3f),
                        shape = RoundedCornerShape(16.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = "Danger Zone",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = onDeleteClick,
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.error,
                                MaterialTheme.colorScheme.error
                            )
                        )
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.remove),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete Budget")
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }
    } // end LazyColumn
    } // end Column
}

// ─── Top Bar ─────────────────────────────────────────────────────────────────
@Composable
private fun BudgetTopBar(
    title: String,
    onBack: () -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
    ) {
        IconButton(onClick = onBack) {
            Icon(
                painter = painterResource(R.drawable.arrow_back),
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
        )
        IconButton(onClick = onEdit) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = "Edit budget",
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

// ─── Hero Stats Card ─────────────────────────────────────────────────────────
@Composable
private fun BudgetHeroCard(
    budgetName: String,
    actualSpending: Double,
    budgetLimit: Double,
    percentUsed: Int,
    daysLeft: Int,
    remaining: Double,
    dailyAvg: Double,
    isOverBudget: Boolean,
    modifier: Modifier = Modifier
) {
    val primaryColor    = MaterialTheme.colorScheme.primary
    val containerColor  = MaterialTheme.colorScheme.primaryContainer

    val animatedProgress by animateFloatAsState(
        targetValue = (percentUsed / 100f).coerceIn(0f, 1f),
        animationSpec = tween(800),
        label = "budget_progress"
    )

    val progressColor = when {
        percentUsed >= 100 -> MaterialTheme.colorScheme.error
        percentUsed >= 80  -> Color(0xFFE65100)
        percentUsed >= 60  -> Color(0xFFFFA000)
        else               -> Color(0xFF388E3C)
    }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(listOf(primaryColor, containerColor))
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Budget name subtitle
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.budget_2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = budgetName,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Large spent amount
                Text(
                    text = formatMoneyValue(actualSpending),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Text(
                    text = "spent",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Animated progress bar
                LinearProgressIndicator(
                    progress = { animatedProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = progressColor,
                    trackColor = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.2f)
                )

                Spacer(modifier = Modifier.height(6.dp))

                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "$percentUsed% of ${formatMoneyValue(budgetLimit)} limit",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "$daysLeft days left",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Three stat chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    HeroStatChip(label = "Spent", value = formatMoneyValue(actualSpending))
                    HeroStatChip(
                        label = if (isOverBudget) "Over" else "Remaining",
                        value = formatMoneyValue(if (isOverBudget) actualSpending - budgetLimit else remaining),
                        valueColor = if (isOverBudget) MaterialTheme.colorScheme.error else null
                    )
                    HeroStatChip(label = "Daily avg", value = formatMoneyValue(dailyAvg))
                }
            }
        }
    }
}

@Composable
private fun HeroStatChip(
    label: String,
    value: String,
    valueColor: Color? = null,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.15f))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor ?: MaterialTheme.colorScheme.onPrimary,
            maxLines = 1
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.75f)
        )
    }
}

// ─── Spending Trend Chart ─────────────────────────────────────────────────────
@Composable
private fun SpendingTrendCard(
    trendData: List<TrendPoint>,
    dailyBudget: Double,
    dailyLimitLine: Double,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    val greenColor  = Color(0xFF388E3C)
    val orangeColor = Color(0xFFE65100)
    val redColor    = Color(0xFFB71C1C)
    val gridColor   = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f)
    val limitColor  = Color(0xFFFF8F00)
    val onSurface   = MaterialTheme.colorScheme.onSurface

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.chart),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Spending Trend",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // How-to-read caption
            Text(
                text = "Each bar = spending per period. The dashed line = daily limit to stay on budget. " +
                    "🟢 Within limit  🟠 Slightly over  🔴 Over limit",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 10.sp,
                lineHeight = 14.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (trendData.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                ) {
                    Text(
                        text = "No transactions in this budget period yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                val maxVal = trendData.maxOf { it.totalOut }.coerceAtLeast(dailyLimitLine).coerceAtLeast(1.0)
                val showN  = if (trendData.size <= 7) 1 else ceil(trendData.size / 7.0).toInt()

                Box {
                    // Chart area
                    Column {
                        // Y-axis max label
                        Text(
                            text = formatMoneyValue(maxVal),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 9.sp
                        )
                        Canvas(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
                                .run { if (!isPremium) alpha(1f) else this }
                        ) {
                            val w = size.width
                            val h = size.height
                            val barCount = trendData.size
                            val gap = 4.dp.toPx()
                            val barW = ((w - gap * (barCount + 1)) / barCount).coerceAtLeast(4f)

                            // Draw daily limit dashed line using dailyLimitLine (fixed: budgetLimit / totalDays)
                            if (dailyLimitLine > 0) {
                                val limitY = h - (dailyLimitLine / maxVal).toFloat() * h
                                drawLine(
                                    color = limitColor,
                                    start = Offset(0f, limitY),
                                    end = Offset(w, limitY),
                                    strokeWidth = 1.5.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 6f))
                                )
                            }

                            trendData.forEachIndexed { i, point ->
                                val barH = (point.totalOut / maxVal).toFloat() * h
                                val left = gap + i * (barW + gap)
                                val top  = h - barH
                                // Use dailyLimitLine (not dailyBudget) so coloring is accurate
                                // even when budget is over and remaining=0
                                val barColor = when {
                                    dailyLimitLine <= 0                        -> greenColor
                                    point.totalOut > dailyLimitLine * 1.5     -> redColor
                                    point.totalOut > dailyLimitLine            -> orangeColor
                                    else                                       -> greenColor
                                }
                                drawRoundRect(
                                    color = barColor,
                                    topLeft = Offset(left, top),
                                    size = Size(barW, barH.coerceAtLeast(2f)),
                                    cornerRadius = CornerRadius(2.dp.toPx())
                                )
                            }
                        }

                        // X-axis labels
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            trendData.forEachIndexed { i, point ->
                                val show = i % showN == 0
                                Text(
                                    text = if (show) point.label else "",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontSize = 8.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.weight(1f),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Premium gate overlay (covers lower half)
                    if (!isPremium) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .align(Alignment.BottomCenter)
                                .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0f),
                                            MaterialTheme.colorScheme.surface.copy(alpha = 0.97f)
                                        )
                                    )
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer)
                                    .padding(horizontal = 14.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Unlock full trend history → Go Premium",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Legend
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendDot(color = greenColor, label = "Within limit")
                    LegendDot(color = orangeColor, label = "Slightly over")
                    LegendDot(color = redColor, label = "Over limit")
                    LegendDot(color = limitColor, label = "Daily limit", isDash = true)
                }
            }
        }
    }
}

@Composable
private fun LegendDot(
    color: Color,
    label: String,
    isDash: Boolean = false,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (isDash) {
            Canvas(modifier = Modifier.size(16.dp, 2.dp)) {
                drawLine(
                    color = color,
                    start = Offset(0f, 0f),
                    end = Offset(size.width, 0f),
                    strokeWidth = 3f,
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 3f))
                )
            }
        } else {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(color)
            )
        }
        Spacer(modifier = Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// ─── Smart Insights Card ──────────────────────────────────────────────────────
@Composable
private fun SmartInsightsCard(
    insights: List<InsightItem>,
    isPremium: Boolean,
    modifier: Modifier = Modifier
) {
    if (insights.isEmpty()) return

    val freeInsights    = insights.filter { !it.requiresPremium }
    val premiumInsights = insights.filter { it.requiresPremium }

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(R.drawable.info),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Smart Insights",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Free insights always shown
            freeInsights.forEach { insight ->
                InsightRow(insight = insight)
                Spacer(modifier = Modifier.height(8.dp))
            }

            if (isPremium) {
                // All premium insights shown normally
                premiumInsights.forEach { insight ->
                    InsightRow(insight = insight)
                    Spacer(modifier = Modifier.height(8.dp))
                }
            } else if (premiumInsights.isNotEmpty()) {
                // Show first 2 blurred
                premiumInsights.take(2).forEach { insight ->
                    Box {
                        Box(modifier = Modifier.alpha(0.12f)) {
                            InsightRow(insight = insight)
                        }
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .padding(start = 12.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Premium Insight",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // CTA row
                val remaining = premiumInsights.size
                OutlinedButton(
                    onClick = { /* navigate to subscription */ },
                    shape = RoundedCornerShape(20.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.lock),
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "$remaining more insight${if (remaining != 1) "s" else ""} available — Upgrade to unlock →",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightRow(
    insight: InsightItem,
    modifier: Modifier = Modifier
) {
    val accentColor = when (insight.isPositive) {
        true  -> Color(0xFF388E3C)
        false -> Color(0xFFE65100)
        null  -> Color(0xFFFFA000)
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
    ) {
        Box(
            modifier = Modifier
                .width(4.dp)
                .height(IntrinsicSize.Max)
                .background(accentColor)
                .padding(vertical = 12.dp)
        )
        Text(
            text = insight.text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp)
        )
    }
}

// ─── Date header ──────────────────────────────────────────────────────────────
@Composable
private fun BudgetDateHeader(date: LocalDate, modifier: Modifier = Modifier) {
    val formatter = remember { DateTimeFormatter.ofPattern("EEE d MMM") }
    Text(
        text = date.format(formatter),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontWeight = FontWeight.SemiBold,
        modifier = modifier.padding(horizontal = 16.dp, vertical = 6.dp)
    )
}

// ─── Transaction row ──────────────────────────────────────────────────────────
@Composable
private fun BudgetTransactionRow(
    transaction: Transaction,
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { DateTimeFormatter.ofPattern("HH:mm") }
    // Use amount sign as source of truth — more reliable than type string matching
    val isOutflow = transaction.transactionAmount < 0

    val amountColor = if (isOutflow)
        MaterialTheme.colorScheme.error
    else
        Color(0xFF388E3C)

    val prefix = if (isOutflow) "-" else "+"
    val displayName = (transaction.nickName?.trim()?.ifBlank { null }
        ?: transaction.entity.trim().ifBlank { null }
        ?: transaction.transactionType)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        // Avatar circle
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer)
        ) {
            Text(
                text = displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
                fontWeight = FontWeight.Bold
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = displayName,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = transaction.transactionType,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = "· ${transaction.time.format(timeFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = "$prefix${formatMoneyValue(abs(transaction.transactionAmount))}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = amountColor
        )
    }
}

// ─── Manual Transaction row ───────────────────────────────────────────────────
@Composable
private fun BudgetManualTransactionRow(
    tx: com.records.pesa.db.models.ManualTransaction,
    onEdit: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("HH:mm") }
    val amountColor = if (tx.isOutflow) MaterialTheme.colorScheme.error else Color(0xFF388E3C)
    val prefix = if (tx.isOutflow) "-" else "+"
    val avatarColor = txAvatarColor(tx.memberName)

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                Text(
                    text = tx.memberName.take(1).uppercase(),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
            // Small "edit" badge marks as manual
            Box(
                modifier = Modifier.size(14.dp).clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .align(Alignment.BottomEnd)
                    .clickable(onClick = onEdit),
                contentAlignment = Alignment.Center
            ) {
                Icon(painter = painterResource(R.drawable.edit), contentDescription = null, modifier = Modifier.size(9.dp), tint = Color.White)
            }
        }
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(tx.memberName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tx.transactionTypeName, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                tx.time?.let { Text("· ${it.format(timeFormatter)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant) }
            }
            if (tx.description.isNotBlank()) {
                Text(tx.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
        Text("$prefix${formatMoneyValue(tx.amount)}", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = amountColor)
    }
}

// ─── Edit Dialog ──────────────────────────────────────────────────────────────
@Composable
private fun BudgetEditDialog(
    uiState: BudgetInfoScreenUiState,
    onNameChange: (String) -> Unit,
    onLimitChange: (String) -> Unit,
    onDateChange: (String) -> Unit,
    onThresholdChange: (Int) -> Unit,
    onToggleMember: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    val showDatePicker: () -> Unit = {
        val today = LocalDate.now()
        DatePickerDialog(
            context,
            { _, y, m, d ->
                onDateChange(LocalDate.of(y, m + 1, d).toString())
            },
            today.year, today.monthValue - 1, today.dayOfMonth
        ).show()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Edit Budget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedTextField(
                    value = uiState.budgetName,
                    onValueChange = onNameChange,
                    label = { Text("Budget name") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.budgetLimit,
                    onValueChange = onLimitChange,
                    label = { Text("Budget limit (KES)") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Next
                    ),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = uiState.budgetLimitDate,
                    onValueChange = onDateChange,
                    label = { Text("Limit date") },
                    readOnly = true,
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    trailingIcon = {
                        IconButton(onClick = showDatePicker) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = "Pick date",
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker() }
                )

                // Alert threshold — PREMIUM feature
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Alert threshold",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f)
                    )
                    if (!uiState.isPremium) {
                        Surface(
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            shape = RoundedCornerShape(4.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = null,
                                    modifier = Modifier.size(10.dp),
                                    tint = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "Premium",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                )
                            }
                        }
                    }
                }
                Text(
                    text = "Alert me when ${uiState.alertThreshold}% of budget is used",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isPremium) MaterialTheme.colorScheme.onSurface
                            else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Slider(
                    value = uiState.alertThreshold.toFloat(),
                    onValueChange = { if (uiState.isPremium) onThresholdChange(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8, // 10,20,30,40,50,60,70,80,90,100
                    enabled = uiState.isPremium,
                    modifier = Modifier.fillMaxWidth()
                )

                if (uiState.loadingStatus == LoadingStatus.LOADING) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(28.dp))
                    }
                }

                // Member management
                if (uiState.allCategoryMembers.isNotEmpty()) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "Tracked members",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        "Select members whose spending counts toward this budget. Leave all unchecked to track the whole category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                    uiState.allCategoryMembers.forEach { memberName ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onToggleMember(memberName) }
                                .padding(vertical = 4.dp)
                        ) {
                            Checkbox(
                                checked = memberName in uiState.editSelectedMembers,
                                onCheckedChange = { onToggleMember(memberName) }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(memberName, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onSave,
                enabled = uiState.loadingStatus != LoadingStatus.LOADING,
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    painter = painterResource(R.drawable.check),
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ─── Delete Confirmation Dialog ───────────────────────────────────────────────
@Composable
private fun BudgetDeleteDialog(
    budgetName: String,
    isDeleting: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Delete Budget",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.error
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete \"$budgetName\"? This cannot be undone.",
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isDeleting) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(28.dp),
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isDeleting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(16.dp)
    )
}

// ─── Preview ──────────────────────────────────────────────────────────────────
@Preview(showBackground = true)
@Composable
private fun BudgetInfoScreenPreview() {
    MaterialTheme {
        BudgetInfoScreen(
            uiState = BudgetInfoScreenUiState(
                budgetName = "Groceries Budget",
                actualSpending = 3200.0,
                percentUsed = 64,
                remaining = 1800.0,
                dailyAvg = 320.0,
                daysLeft = 10,
                isPremium = false
            ),
            onEditClick = {},
            onDeleteClick = {},
            navigateToBudgetAllTransactions = {},
            navigateToPreviousScreen = {}
        )
    }
}

@Composable
private fun BudgetMembersCard(
    budgetMembers: List<com.records.pesa.db.models.BudgetMember>,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            Color(0xFF1565C0).copy(alpha = 0.08f),
                            Color(0xFF1565C0).copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(16.dp)
        ) {
            Column {
                Text(
                    text = "Tracked Members",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (budgetMembers.isEmpty()) "All category members are tracked"
                           else "Spending for these members is counted toward this budget",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (budgetMembers.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    budgetMembers.forEach { member ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            val avatarColor = txAvatarColor(member.memberName)
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(avatarColor)
                            ) {
                                Text(
                                    text = member.memberName.take(1).uppercase(),
                                    color = Color.White,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = member.memberName,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }
            }
        }
    }
}
