package com.records.pesa.ui.screens.dashboard.category

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.models.ManualCategoryMember
import com.records.pesa.db.models.ManualTransaction
import com.records.pesa.db.models.ManualTransactionType
import com.records.pesa.models.CategoryBudget
import com.records.pesa.models.CategoryKeyword
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.components.SubscriptionDialog
import com.records.pesa.ui.screens.dashboard.budget.BudgetStatus
import com.records.pesa.ui.screens.dashboard.budget.BudgetWithProgress
import com.records.pesa.ui.screens.transactions.DateRangePickerDialog
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlin.math.min

// ─── Navigation destination ───────────────────────────────────────────────────
object CategoryDetailsScreenDestination : com.records.pesa.nav.AppNavigation {
    override val title = "Category details screen"
    override val route = "category-details-screen"
    const val categoryId = "categoryId"
    val routeWithArgs = "$route/{$categoryId}"
}

// ─── Member colour palette ────────────────────────────────────────────────────
private val memberColors: List<Color> = listOf(
    Color(0xFF006A65), Color(0xFF7B5EA7), Color(0xFF48607B), Color(0xFFB5542D),
    Color(0xFF2D6A8A), Color(0xFF8A3D4A), Color(0xFF5A7A2B), Color(0xFF4A6361),
    Color(0xFF7B6E28), Color(0xFF6B3D7A), Color(0xFF2D6A4A), Color(0xFF8A5D2D),
)

private fun memberColor(index: Int): Color = memberColors[index % memberColors.size]

// ─── Top-level Composable (wires ViewModel) ───────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoryDetailsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    navigateToTransactionsScreen: (categoryId: String) -> Unit,
    navigateToAllTransactionsScreen: (categoryId: String) -> Unit,
    navigateToCategoryBudgetListScreen: (categoryId: String, categoryName: String) -> Unit,
    navigateToBudgetCreationScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToHomeScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: CategoryDetailsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = { viewModel.getCategory() }
    )

    var showEditCategoryNameDialog by rememberSaveable { mutableStateOf(false) }
    var showEditMemberNameDialog   by rememberSaveable { mutableStateOf(false) }
    var categoryName               by rememberSaveable { mutableStateOf("") }
    var memberName                 by rememberSaveable { mutableStateOf("") }
    var categoryId                 by rememberSaveable { mutableIntStateOf(0) }
    var keywordId                  by rememberSaveable { mutableIntStateOf(0) }
    var showRemoveMemberDialog     by rememberSaveable { mutableStateOf(false) }
    var showRemoveCategoryDialog   by rememberSaveable { mutableStateOf(false) }
    var showSubscriptionDialog     by rememberSaveable { mutableStateOf(false) }

    if (showEditCategoryNameDialog) {
        EditNameDialog(
            title = "Category name", label = "Category", name = categoryName,
            onNameChange = { categoryName = it; viewModel.editCategoryName(it) },
            onConfirm = { viewModel.updateCategoryName(); showEditCategoryNameDialog = false },
            onDismiss = { showEditCategoryNameDialog = false }
        )
    }
    if (showEditMemberNameDialog) {
        EditNameDialog(
            title = "Member name", label = "New name", name = memberName,
            onNameChange = { memberName = it; viewModel.editMemberName(it) },
            onConfirm = { viewModel.updateMemberName(); showEditMemberNameDialog = false },
            onDismiss = { showEditMemberNameDialog = false }
        )
    }
    if (showRemoveMemberDialog) {
        DeleteDialog(
            name = memberName, categoryDeletion = false,
            onConfirm = { viewModel.removeCategoryMember(categoryId, keywordId); showRemoveMemberDialog = false },
            onDismiss = { showRemoveMemberDialog = false }
        )
    }
    if (showRemoveCategoryDialog) {
        DeleteDialog(
            name = categoryName, categoryDeletion = true,
            onConfirm = { viewModel.removeCategory(categoryId); showRemoveCategoryDialog = false },
            onDismiss = { showRemoveCategoryDialog = false }
        )
    }
    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = { showSubscriptionDialog = false; navigateToSubscriptionScreen() }
        )
    }

    if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
        viewModel.getCategory()
        viewModel.resetLoadingStatus()
    } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed. Try again.", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }
    if (uiState.deletionStatus == DeletionStatus.SUCCESS) {
        Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetLoadingStatus()
    }

    if (uiState.inlineBudgetSaved) {
        Toast.makeText(context, "Budget created!", Toast.LENGTH_SHORT).show()
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        CategoryDetailsScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            category = uiState.category,
            isPremium = uiState.isPremium,
            selectedPeriod = uiState.selectedPeriod,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            showCustomPicker = uiState.showCustomPicker,
            totalIn = uiState.totalIn,
            totalOut = uiState.totalOut,
            txCount = uiState.txCount,
            typeBreakdown = uiState.typeBreakdown,
            trendData = uiState.trendData,
            memberStats = uiState.memberStats,
            insights = uiState.insights,
            onSelectPeriod = { viewModel.selectPeriod(it) },
            onSetCustomStart = { viewModel.setCustomStartDate(it) },
            onSetCustomEnd = { viewModel.setCustomEndDate(it) },
            onToggleCustomPicker = { viewModel.toggleCustomPicker() },
            onShowSubscriptionDialog = { showSubscriptionDialog = true },
            onEditCategoryName = { categoryName = it; showEditCategoryNameDialog = true },
            onEditMemberName = {
                memberName = it.nickName ?: it.keyWord
                viewModel.updateCategoryKeyword(it)
                showEditMemberNameDialog = true
            },
            onRemoveMember = { memName, catId, keyId ->
                memberName = memName; categoryId = catId; keywordId = keyId
                showRemoveMemberDialog = true
            },
            onRemoveCategory = { catId, cateName -> categoryId = catId; categoryName = cateName; showRemoveCategoryDialog = true },
            navigateToCategoryBudgetListScreen = navigateToCategoryBudgetListScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToMembersAdditionScreen = navigateToMembersAdditionScreen,
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToAllTransactionsScreen = navigateToAllTransactionsScreen,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            showInlineBudgetForm = uiState.showInlineBudgetForm,
            inlineBudgetName = uiState.inlineBudgetName,
            inlineBudgetLimit = uiState.inlineBudgetLimit,
            inlineBudgetStartDate = uiState.inlineBudgetStartDate,
            inlineBudgetEndDate = uiState.inlineBudgetEndDate,
            inlineBudgetSaving = uiState.inlineBudgetSaving,
            onToggleInlineBudgetForm = { viewModel.toggleInlineBudgetForm() },
            onInlineBudgetNameChange = { viewModel.updateInlineBudgetName(it) },
            onInlineBudgetLimitChange = { viewModel.updateInlineBudgetLimit(it) },
            onInlineBudgetStartDateChange = { viewModel.updateInlineBudgetStartDate(it) },
            onInlineBudgetEndDateChange = { viewModel.updateInlineBudgetEndDate(it) },
            onCreateInlineBudget = { viewModel.createInlineBudget() },
            budgetProgressMap = uiState.budgetProgressMap,
            manualMembers = uiState.manualMembers,
            manualTransactions = uiState.manualTransactions,
            onAddManualMember = { viewModel.addManualMember(it) },
            onDeleteManualMember = { id, name -> viewModel.deleteManualMember(id, name) },
            onEditManualMember = { id, oldName, newName -> viewModel.editManualMemberName(id, oldName, newName) },
            onAddManualTransaction = { memberName, isOutflow, amount, description, date, time ->
                viewModel.addManualTransaction(memberName, isOutflow, amount, description, date, time)
            },
            onDeleteManualTransaction = { viewModel.deleteManualTransaction(it) }
        )
    }
}

// ─── Main Screen ──────────────────────────────────────────────────────────────
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoryDetailsScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    category: TransactionCategory,
    isPremium: Boolean,
    selectedPeriod: TimePeriod,
    startDate: LocalDate,
    endDate: LocalDate,
    showCustomPicker: Boolean,
    totalIn: Double,
    totalOut: Double,
    txCount: Int,
    typeBreakdown: List<Pair<String, Int>>,
    trendData: List<TrendPoint>,
    memberStats: List<MemberStat>,
    insights: List<InsightItem>,
    onSelectPeriod: (TimePeriod) -> Unit,
    onSetCustomStart: (LocalDate) -> Unit,
    onSetCustomEnd: (LocalDate) -> Unit,
    onToggleCustomPicker: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    onEditCategoryName: (name: String) -> Unit,
    onEditMemberName: (categoryKeyword: CategoryKeyword) -> Unit,
    onRemoveMember: (memberName: String, categoryId: Int, keywordId: Int) -> Unit,
    onRemoveCategory: (categoryId: Int, categoryName: String) -> Unit,
    navigateToCategoryBudgetListScreen: (categoryId: String, categoryName: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    navigateToTransactionsScreen: (categoryId: String) -> Unit,
    navigateToAllTransactionsScreen: (categoryId: String) -> Unit = {},
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit = {},
    showInlineBudgetForm: Boolean = false,
    inlineBudgetName: String = "",
    inlineBudgetLimit: String = "",
    inlineBudgetStartDate: LocalDate = LocalDate.now().withDayOfMonth(1),
    inlineBudgetEndDate: LocalDate? = null,
    inlineBudgetSaving: Boolean = false,
    onToggleInlineBudgetForm: () -> Unit = {},
    onInlineBudgetNameChange: (String) -> Unit = {},
    onInlineBudgetLimitChange: (String) -> Unit = {},
    onInlineBudgetStartDateChange: (LocalDate) -> Unit = {},
    onInlineBudgetEndDateChange: (LocalDate) -> Unit = {},
    onCreateInlineBudget: () -> Unit = {},
    budgetProgressMap: Map<Int, BudgetWithProgress> = emptyMap(),
    manualMembers: List<ManualCategoryMember> = emptyList(),
    manualTransactions: List<ManualTransaction> = emptyList(),
    onAddManualMember: (String) -> Unit = {},
    onDeleteManualMember: (id: Int, name: String) -> Unit = { _, _ -> },
    onEditManualMember: (id: Int, oldName: String, newName: String) -> Unit = { _, _, _ -> },
    onAddManualTransaction: (memberName: String, isOutflow: Boolean, amount: Double, description: String, date: LocalDate, time: java.time.LocalTime?) -> Unit = { _, _, _, _, _, _ -> },
    onDeleteManualTransaction: (Int) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val categoryColor = txAvatarColor(category.name)
    val primaryColor = MaterialTheme.colorScheme.primary
    val net = totalIn - totalOut
    var showMembers by rememberSaveable { mutableStateOf(true) }
    var showBudgets  by rememberSaveable { mutableStateOf(false) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }

    Column(modifier = modifier.fillMaxSize()) {

        // ── App bar ──────────────────────────────────────────────────────────
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
                    modifier = Modifier.size(22.dp).graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(
                modifier = Modifier.size(34.dp).clip(CircleShape).background(categoryColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = category.name.take(2).uppercase(),
                    color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                )
            }
            Spacer(Modifier.width(8.dp))
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold, fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
            )
            IconButton(onClick = { onEditCategoryName(category.name) }) {
                Icon(
                    painter = painterResource(R.drawable.edit),
                    contentDescription = "Edit name",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = { onRemoveCategory(category.id, category.name) }) {
                Icon(
                    painter = painterResource(R.drawable.remove),
                    contentDescription = "Delete category",
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        Box(modifier = Modifier.fillMaxSize()) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item { Spacer(Modifier.height(4.dp)) }

                // ── Non-premium upgrade banner ────────────────────────────────
                if (!isPremium) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(
                                    Brush.horizontalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.15f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                                        )
                                    )
                                )
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Upgrade for full experience",
                                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "All-time history · advanced insights · projections",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Button(
                                onClick = onShowSubscriptionDialog,
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.tertiary
                                ),
                                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    "Upgrade",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }                        }
                    }
                }

                // ── Period picker ─────────────────────────────────────────────
                item {
                    CatPeriodPicker(
                        isPremium = isPremium,
                        selectedPeriod = selectedPeriod,
                        startDate = startDate,
                        endDate = endDate,
                        txCount = txCount,
                        totalIn = totalIn,
                        totalOut = totalOut,
                        onPeriodSelected = onSelectPeriod,
                        onOpenCustomPicker = onToggleCustomPicker,
                        onShowSubscriptionDialog = onShowSubscriptionDialog
                    )
                }

                // ── Custom date picker ────────────────────────────────────────
                if (showCustomPicker) {
                    item {
                        DateRangePickerDialog(
                            premium = isPremium,
                            startDate = startDate,
                            endDate = endDate,
                            defaultStartDate = null,
                            defaultEndDate = null,
                            onChangeStartDate = onSetCustomStart,
                            onChangeLastDate = onSetCustomEnd,
                            onDismiss = onToggleCustomPicker,
                            onConfirm = onToggleCustomPicker,
                            onShowSubscriptionDialog = onShowSubscriptionDialog,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // ── Hero stats card ───────────────────────────────────────────
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            categoryColor.copy(alpha = 0.25f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                CatStatPill(
                                    label = "$txCount transaction${if (txCount != 1) "s" else ""}",
                                    color = categoryColor, modifier = Modifier.weight(1f)
                                )
                                CatStatPill(
                                label = "${category.keywords.size + manualMembers.size} member${if ((category.keywords.size + manualMembers.size) != 1) "s" else ""}",
                                    color = MaterialTheme.colorScheme.secondary, modifier = Modifier.weight(1f)
                                )
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                MoneyStatBox(
                                    label = "Money In", amount = totalIn,
                                    color = MaterialTheme.colorScheme.tertiary, modifier = Modifier.weight(1f)
                                )
                                MoneyStatBox(
                                    label = "Money Out", amount = totalOut,
                                    color = MaterialTheme.colorScheme.error, modifier = Modifier.weight(1f)
                                )
                                MoneyStatBox(
                                    label = "Net", amount = net,
                                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            // In vs Out bar
                            val total = totalIn + totalOut
                            if (total > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "In")
                                        LegendDot(color = MaterialTheme.colorScheme.error, label = "Out")
                                    }
                                    Box(
                                        modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.error.copy(alpha = 0.3f))
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth((totalIn / total).toFloat().coerceIn(0f, 1f))
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(MaterialTheme.colorScheme.tertiary)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Activity bar chart ────────────────────────────────────────
                if (trendData.isNotEmpty() && (totalIn > 0 || totalOut > 0)) {
                    item {
                        // Determine aggregation level from point count vs date span
                        val daySpan = startDate.until(endDate, java.time.temporal.ChronoUnit.DAYS).toInt()
                        val granularity = when {
                            daySpan > 90 -> "Monthly"
                            daySpan > 31 -> "Weekly"
                            else         -> "Daily"
                        }
                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column {
                                        Text(
                                            "Activity",
                                            fontWeight = FontWeight.Bold, fontSize = 14.sp
                                        )
                                        Text(
                                            "$granularity · ${dateFormatter.format(startDate)} – ${dateFormatter.format(endDate)}",
                                            fontSize = 10.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    }
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        LegendDot(color = MaterialTheme.colorScheme.tertiary, label = "In")
                                        LegendDot(color = MaterialTheme.colorScheme.error, label = "Out")
                                    }
                                }
                                TrendBarChart(
                                    trendData = trendData,
                                    modifier = Modifier.fillMaxWidth().height(160.dp)
                                )
                            }
                        }
                    }
                }

                // ── Member distribution donut ─────────────────────────────────
                val membersWithActivity = memberStats.filter { it.totalOut + it.totalIn > 0 }
                if (membersWithActivity.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Text(
                                    "Member Distribution",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                MemberDonutChart(
                                    memberStats = membersWithActivity,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }

                // ── Transaction type breakdown ────────────────────────────────
                if (typeBreakdown.isNotEmpty()) {
                    item {
                        ElevatedCard(
                            shape = RoundedCornerShape(20.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    "Transaction Types",
                                    fontWeight = FontWeight.Bold, fontSize = 14.sp
                                )
                                typeBreakdown.take(5).forEachIndexed { idx, (type, count) ->
                                    val fraction = if (txCount > 0) count.toFloat() / txCount else 0f
                                    Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                type, fontSize = 11.sp, fontWeight = FontWeight.Medium,
                                                modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                "$count (${(fraction * 100).toInt()}%)",
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Box(
                                            modifier = Modifier.fillMaxWidth().height(5.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(memberColor(idx).copy(alpha = 0.15f))
                                        ) {
                                            Box(
                                                modifier = Modifier.fillMaxWidth(fraction).fillMaxSize()
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(memberColor(idx))
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Insights ──────────────────────────────────────────────────
                if (insights.isNotEmpty()) {
                    item {
                        InsightsCard(
                            insights = insights,
                            isPremium = isPremium,
                            onShowSubscriptionDialog = onShowSubscriptionDialog
                        )
                    }
                }

                // ── Members section ───────────────────────────────────────────
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { showMembers = !showMembers }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.secondaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.contact),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer
                                        )
                                    }
                                    Column {
                                        Text(
                                            "Members", fontWeight = FontWeight.Bold, fontSize = 14.sp
                                        )
                                        Text(
                                            "${category.keywords.size + manualMembers.size} member${if ((category.keywords.size + manualMembers.size) != 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = if (showMembers) "Collapse" else "Expand",
                                    modifier = Modifier.size(16.dp)
                                        .graphicsLayer { rotationZ = if (showMembers) 90f else 0f },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = showMembers,
                                enter = expandVertically(), exit = shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    if (category.keywords.isEmpty()) {
                                        Text(
                                            "No members yet",
                                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                                            textAlign = TextAlign.Center,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            fontSize = 13.sp
                                        )
                                    } else {
                                        category.keywords.forEach { member ->
                                            val stat = memberStats.firstOrNull {
                                                it.keyword.equals(member.keyWord, ignoreCase = true)
                                            }
                                            MemberRow(
                                                member = member,
                                                stat = stat,
                                                categoryId = category.id,
                                                onEditMemberName = onEditMemberName,
                                                onRemoveMember = onRemoveMember
                                            )
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }

                                    // Manual (non-M-PESA) members
                                    var manualMemberToDelete by remember { mutableStateOf<ManualCategoryMember?>(null) }
                                    var manualMemberToEdit by remember { mutableStateOf<ManualCategoryMember?>(null) }
                                    var editManualMemberNameText by remember { mutableStateOf("") }

                                    manualMemberToDelete?.let { m ->
                                        AlertDialog(
                                            onDismissRequest = { manualMemberToDelete = null },
                                            title = { Text("Remove Member") },
                                            text = {
                                                Text(
                                                    "Remove \"${m.name}\"? This will also delete all manual transactions you recorded for them in this category. This cannot be undone."
                                                )
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        onDeleteManualMember(m.id, m.name)
                                                        manualMemberToDelete = null
                                                    },
                                                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                                                ) { Text("Remove") }
                                            },
                                            dismissButton = { TextButton(onClick = { manualMemberToDelete = null }) { Text("Cancel") } }
                                        )
                                    }

                                    manualMemberToEdit?.let { m ->
                                        AlertDialog(
                                            onDismissRequest = { manualMemberToEdit = null },
                                            title = { Text("Edit Member Name") },
                                            text = {
                                                OutlinedTextField(
                                                    value = editManualMemberNameText,
                                                    onValueChange = { editManualMemberNameText = it },
                                                    label = { Text("New name") },
                                                    placeholder = { Text(m.name) },
                                                    singleLine = true,
                                                    keyboardOptions = KeyboardOptions(
                                                        capitalization = KeyboardCapitalization.Words,
                                                        imeAction = ImeAction.Done
                                                    )
                                                )
                                            },
                                            confirmButton = {
                                                Button(
                                                    onClick = {
                                                        val newName = editManualMemberNameText.trim()
                                                        if (newName.isNotBlank()) {
                                                            onEditManualMember(m.id, m.name, newName)
                                                        }
                                                        manualMemberToEdit = null
                                                    },
                                                    enabled = editManualMemberNameText.isNotBlank()
                                                ) { Text("Save") }
                                            },
                                            dismissButton = { TextButton(onClick = { manualMemberToEdit = null }) { Text("Cancel") } }
                                        )
                                    }

                                    if (manualMembers.isNotEmpty()) {
                                        HorizontalDivider(
                                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.edit),
                                                contentDescription = null,
                                                modifier = Modifier.size(12.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                "Non-M-PESA members",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        manualMembers.forEach { member ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .padding(horizontal = 16.dp, vertical = 4.dp),
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Box(
                                                    modifier = Modifier.size(32.dp).clip(CircleShape)
                                                        .background(txAvatarColor(member.name)),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        member.name.take(1).uppercase(),
                                                        color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp
                                                    )
                                                }
                                                Spacer(Modifier.width(10.dp))
                                                Text(
                                                    member.name,
                                                    fontSize = 13.sp,
                                                    modifier = Modifier.weight(1f)
                                                )
                                                IconButton(
                                                    onClick = {
                                                        editManualMemberNameText = member.name
                                                        manualMemberToEdit = member
                                                    },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.edit),
                                                        contentDescription = "Edit ${member.name}",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { manualMemberToDelete = member },
                                                    modifier = Modifier.size(32.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.remove),
                                                        contentDescription = "Remove ${member.name}",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(14.dp)
                                                    )
                                                }
                                            }
                                            HorizontalDivider(
                                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f),
                                                modifier = Modifier.padding(horizontal = 16.dp)
                                            )
                                        }
                                    }

                                    // Add members button
                                    TextButton(
                                        onClick = { navigateToMembersAdditionScreen(category.id.toString()) },
                                        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_add),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(Modifier.width(6.dp))
                                        Text("Add Members")
                                    }
                                }
                            }
                        }
                    }
                }

                // ── Budgets section ───────────────────────────────────────────
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth()
                                    .clickable { showBudgets = !showBudgets }
                                    .padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.size(30.dp).clip(CircleShape)
                                            .background(MaterialTheme.colorScheme.primaryContainer),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.wallet),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp),
                                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                    Column {
                                        Text("Budgets", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                        Text(
                                            "${category.budgets.size} budget${if (category.budgets.size != 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = if (showBudgets) "Collapse" else "Expand",
                                    modifier = Modifier.size(16.dp)
                                        .graphicsLayer { rotationZ = if (showBudgets) 90f else 0f },
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            AnimatedVisibility(
                                visible = showBudgets,
                                enter = expandVertically(), exit = shrinkVertically()
                            ) {
                                Column {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    if (category.budgets.isEmpty()) {
                                        ElevatedCard(
                                            shape = RoundedCornerShape(16.dp),
                                            colors = CardDefaults.elevatedCardColors(
                                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                                            ),
                                            modifier = Modifier.fillMaxWidth().padding(16.dp)
                                        ) {
                                            Column(
                                                modifier = Modifier.fillMaxWidth().padding(20.dp),
                                                horizontalAlignment = Alignment.CenterHorizontally,
                                                verticalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    painter = painterResource(R.drawable.budget_2),
                                                    contentDescription = null,
                                                    modifier = Modifier.size(36.dp),
                                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                                )
                                                Spacer(Modifier.height(4.dp))
                                                Text(
                                                    "No active budget",
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    "Track your spending by creating a budget for this category.",
                                                    style = MaterialTheme.typography.bodySmall,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    textAlign = TextAlign.Center
                                                )
                                                Spacer(Modifier.height(8.dp))
                                                Button(
                                                    onClick = onToggleInlineBudgetForm,
                                                    shape = RoundedCornerShape(12.dp)
                                                ) {
                                                    Icon(
                                                        painter = painterResource(R.drawable.ic_add),
                                                        contentDescription = null,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                    Spacer(Modifier.width(4.dp))
                                                    Text("Create Budget", fontWeight = FontWeight.SemiBold)
                                                }
                                            }
                                        }
                                    } else {
                                        Column(
                                            modifier = Modifier.padding(16.dp),
                                            verticalArrangement = Arrangement.spacedBy(16.dp)
                                        ) {
                                            category.budgets.forEach { budget ->
                                                BudgetRow(
                                                    budget = budget,
                                                    progress = budgetProgressMap[budget.id],
                                                    onClick = { navigateToBudgetInfoScreen(budget.id.toString()) }
                                                )
                                            }
                                        }
                                    }
                                    Row(
                                        modifier = Modifier.fillMaxWidth()
                                            .padding(horizontal = 8.dp, vertical = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        TextButton(
                                            onClick = { navigateToCategoryBudgetListScreen(category.id.toString(), category.name) },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.list),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text("Manage Budgets", fontSize = 12.sp)
                                        }
                                        TextButton(
                                            onClick = onToggleInlineBudgetForm,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(
                                                painter = painterResource(R.drawable.ic_add),
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(Modifier.width(4.dp))
                                            Text(
                                                if (showInlineBudgetForm) "Cancel" else "New Budget",
                                                fontSize = 12.sp
                                            )
                                        }
                                    }
                                    // Inline budget creation form
                                    AnimatedVisibility(
                                        visible = showInlineBudgetForm,
                                        enter = expandVertically(),
                                        exit = shrinkVertically()
                                    ) {
                                        val context = LocalContext.current
                                        val today = LocalDate.now()
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 16.dp, vertical = 8.dp),
                                            verticalArrangement = Arrangement.spacedBy(10.dp)
                                        ) {
                                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                            Text(
                                                "New Budget for ${category.name}",
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                            OutlinedTextField(
                                                value = inlineBudgetName,
                                                onValueChange = onInlineBudgetNameChange,
                                                label = { Text("Budget name") },
                                                placeholder = {
                                                    val monthYear = today.format(java.time.format.DateTimeFormatter.ofPattern("MMMM yyyy"))
                                                    Text("e.g. ${category.name} – $monthYear", fontSize = 12.sp)
                                                },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    imeAction = ImeAction.Next,
                                                    keyboardType = KeyboardType.Text
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            OutlinedTextField(
                                                value = inlineBudgetLimit,
                                                onValueChange = { v ->
                                                    onInlineBudgetLimitChange(v.filter { it.isDigit() || it == '.' })
                                                },
                                                label = { Text("Spending limit (KES)") },
                                                placeholder = { Text("0.00", fontSize = 12.sp) },
                                                singleLine = true,
                                                shape = RoundedCornerShape(12.dp),
                                                keyboardOptions = KeyboardOptions.Default.copy(
                                                    imeAction = ImeAction.Done,
                                                    keyboardType = KeyboardType.Decimal
                                                ),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            // Start date picker
                                            OutlinedTextField(
                                                value = inlineBudgetStartDate.toString(),
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("Start date") },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        DatePickerDialog(
                                                            context,
                                                            { _, year, month, day ->
                                                                onInlineBudgetStartDateChange(LocalDate.of(year, month + 1, day))
                                                            },
                                                            inlineBudgetStartDate.year,
                                                            inlineBudgetStartDate.monthValue - 1,
                                                            inlineBudgetStartDate.dayOfMonth
                                                        ).show()
                                                    }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.calendar),
                                                            contentDescription = "Pick start date",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            // End date picker
                                            OutlinedTextField(
                                                value = inlineBudgetEndDate?.toString() ?: "",
                                                onValueChange = {},
                                                readOnly = true,
                                                label = { Text("End date") },
                                                placeholder = { Text("Tap to pick a date", fontSize = 12.sp) },
                                                trailingIcon = {
                                                    IconButton(onClick = {
                                                        DatePickerDialog(
                                                            context,
                                                            { _, year, month, day ->
                                                                onInlineBudgetEndDateChange(LocalDate.of(year, month + 1, day))
                                                            },
                                                            today.year, today.monthValue - 1, today.dayOfMonth
                                                        ).show()
                                                    }) {
                                                        Icon(
                                                            painter = painterResource(R.drawable.calendar),
                                                            contentDescription = "Pick end date",
                                                            modifier = Modifier.size(18.dp)
                                                        )
                                                    }
                                                },
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            val canSave = inlineBudgetName.isNotBlank() &&
                                                (inlineBudgetLimit.toDoubleOrNull() ?: 0.0) > 0.0 &&
                                                inlineBudgetEndDate != null &&
                                                inlineBudgetEndDate.isAfter(inlineBudgetStartDate)
                                            Button(
                                                onClick = onCreateInlineBudget,
                                                enabled = canSave && !inlineBudgetSaving,
                                                shape = RoundedCornerShape(12.dp),
                                                modifier = Modifier.fillMaxWidth()
                                            ) {
                                                if (inlineBudgetSaving) {
                                                    CircularProgressIndicator(
                                                        modifier = Modifier.size(18.dp),
                                                        strokeWidth = 2.dp,
                                                        color = MaterialTheme.colorScheme.onPrimary
                                                    )
                                                } else {
                                                    Text("Create Budget")
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // ── View all transactions button ───────────────────────────────
                item {
                    Button(
                        onClick = { navigateToAllTransactionsScreen(category.id.toString()) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.list),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("View All Transactions", fontWeight = FontWeight.SemiBold)
                    }
                }

                item {
                    Spacer(Modifier.height(16.dp))
                }
                item {
                    ManualTransactionsSection(
                        transactions = manualTransactions,
                        mpesaMembers = memberStats.map { it.keyword },
                        manualMembers = manualMembers,
                        onAddTransaction = onAddManualTransaction,
                        onDeleteTransaction = onDeleteManualTransaction,
                        onNavigateToAddMember = { navigateToMembersAdditionScreen(category.id.toString()) }
                    )
                }

                item { Spacer(Modifier.height(32.dp)) }
            }

            // Pull-to-refresh indicator
            if (pullRefreshState != null) {
                PullRefreshIndicator(
                    refreshing = loadingStatus == LoadingStatus.LOADING,
                    state = pullRefreshState,
                    modifier = Modifier.align(Alignment.TopCenter)
                )
            }
        }
    }
}

// ─── Period picker card ───────────────────────────────────────────────────────
@Composable
private fun CatPeriodPicker(
    isPremium: Boolean,
    selectedPeriod: TimePeriod,
    startDate: LocalDate,
    endDate: LocalDate,
    txCount: Int,
    totalIn: Double,
    totalOut: Double,
    onPeriodSelected: (TimePeriod) -> Unit,
    onOpenCustomPicker: () -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val net = totalIn - totalOut
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM") }
    val periodOptions = remember {
        listOf(
            TimePeriod.TODAY, TimePeriod.YESTERDAY,
            TimePeriod.THIS_WEEK, TimePeriod.LAST_WEEK,
            TimePeriod.THIS_MONTH, TimePeriod.LAST_MONTH,
            TimePeriod.THIS_YEAR
        )
    }
    var showPeriodMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = modifier.fillMaxWidth(),
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
                // Period selector chip row
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
                        DropdownMenu(
                            expanded = showPeriodMenu,
                            onDismissRequest = { showPeriodMenu = false }
                        ) {
                            periodOptions.forEach { period ->
                                val requiresPremium = !isPremium && period == TimePeriod.THIS_YEAR
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
                                                color = if (period == selectedPeriod) primaryColor
                                                        else MaterialTheme.colorScheme.onSurface
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
                                        showPeriodMenu = false
                                        if (requiresPremium) onShowSubscriptionDialog()
                                        else onPeriodSelected(period)
                                    }
                                )
                            }
                            HorizontalDivider(modifier = Modifier.padding(horizontal = 8.dp))
                            DropdownMenuItem(
                                text = {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.calendar),
                                            contentDescription = null,
                                            modifier = Modifier.size(14.dp), tint = primaryColor
                                        )
                                        Text("Custom", fontSize = 14.sp, fontWeight = FontWeight.Medium, color = primaryColor)
                                    }
                                },
                                onClick = { showPeriodMenu = false; onOpenCustomPicker() }
                            )
                        }
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "${dateFormatter.format(startDate)} – ${dateFormatter.format(endDate)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "$txCount txn${if (txCount != 1) "s" else ""}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(Modifier.height(14.dp))
                Text(
                    text = "Net Flow",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium, letterSpacing = 0.5.sp
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "${if (net >= 0) "+" else "-"}KES ${String.format("%,.0f", abs(net))}",
                    fontSize = 28.sp, fontWeight = FontWeight.Bold,
                    color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Insights card ────────────────────────────────────────────────────────────
@Composable
private fun InsightsCard(
    insights: List<InsightItem>,
    isPremium: Boolean,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    ElevatedCard(
        shape = RoundedCornerShape(20.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Insights", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                if (!isPremium) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.1f)
                    ) {
                        Text(
                            "Some blurred · Upgrade",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.tertiary,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
            insights.forEach { insight ->
                val isLocked = insight.requiresPremium && !isPremium
                Box {
                    InsightRow(
                        insight = insight,
                        modifier = if (isLocked) Modifier.blur(6.dp) else Modifier
                    )
                    if (isLocked) {
                        Box(
                            modifier = Modifier.matchParentSize()
                                .clip(RoundedCornerShape(10.dp))
                                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.6f))
                                .clickable { onShowSubscriptionDialog() },
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.lock),
                                    contentDescription = "Premium",
                                    modifier = Modifier.size(12.dp),
                                    tint = MaterialTheme.colorScheme.tertiary
                                )
                                Text(
                                    "Premium", fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.tertiary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightRow(insight: InsightItem, modifier: Modifier = Modifier) {
    val accentColor = when (insight.isPositive) {
        true -> MaterialTheme.colorScheme.tertiary
        false -> MaterialTheme.colorScheme.error
        null -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val bgColor = when (insight.isPositive) {
        true -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.08f)
        false -> MaterialTheme.colorScheme.error.copy(alpha = 0.08f)
        null -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(bgColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier.size(6.dp).clip(CircleShape).background(accentColor)
                .align(Alignment.CenterVertically)
        )
        Text(
            text = insight.text,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurface,
            lineHeight = 18.sp,
            modifier = Modifier.weight(1f)
        )
    }
}

// ─── Member donut chart ───────────────────────────────────────────────────────
@Composable
private fun MemberDonutChart(
    memberStats: List<MemberStat>,
    modifier: Modifier = Modifier
) {
    val surface   = MaterialTheme.colorScheme.surface
    val onSurface = MaterialTheme.colorScheme.onSurface
    val totalOut  = memberStats.sumOf { it.totalOut }.coerceAtLeast(0.01)

    var drawTarget by remember { mutableStateOf(0f) }
    LaunchedEffect(memberStats) { drawTarget = 1f }
    val drawProgress by animateFloatAsState(
        targetValue = drawTarget,
        animationSpec = tween(900, easing = FastOutSlowInEasing),
        label = "memberDonut"
    )

    val segments = memberStats.take(8).mapIndexed { idx, stat ->
        Triple(stat, memberColor(idx), (stat.totalOut / totalOut).toFloat())
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(150.dp)) {
            Canvas(modifier = Modifier.size(150.dp)) {
                val canvasSize  = min(size.width, size.height)
                val strokeWidth = canvasSize * 0.18f
                val arcRadius   = (canvasSize / 2f) - strokeWidth / 2f
                val arcTopLeft  = Offset((size.width - arcRadius * 2) / 2f, (size.height - arcRadius * 2) / 2f)
                val arcSize     = Size(arcRadius * 2, arcRadius * 2)
                var startAngle  = -90f
                val gap         = if (segments.size > 1) 2f else 0f

                segments.forEach { (_, color, fraction) ->
                    val sweep = (fraction * 360f - gap).coerceAtLeast(0f) * drawProgress
                    drawArc(
                        color = color, startAngle = startAngle, sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                        topLeft = arcTopLeft, size = arcSize
                    )
                    startAngle += fraction * 360f
                }
                drawCircle(color = surface, radius = arcRadius - strokeWidth / 2f + 2f)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("OUT", fontSize = 10.sp, fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.error, letterSpacing = 1.sp)
                Text(
                    "KES ${String.format("%,.0f", totalOut)}",
                    fontSize = 12.sp, fontWeight = FontWeight.Bold, color = onSurface
                )
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            segments.take(6).forEach { (stat, color, fraction) ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            stat.displayName.take(14),
                            fontSize = 11.sp, fontWeight = FontWeight.Medium,
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "KES ${String.format("%,.0f", stat.totalOut)}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        "${(fraction * 100).toInt()}%",
                        fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = color
                    )
                }
            }
        }
    }
}

// ─── Trend bar chart ─────────────────────────────────────────────────────────
@Composable
private fun TrendBarChart(
    trendData: List<TrendPoint>,
    modifier: Modifier = Modifier
) {
    val inColor    = MaterialTheme.colorScheme.tertiary
    val outColor   = MaterialTheme.colorScheme.error
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant
    val maxVal     = trendData.maxOfOrNull { maxOf(it.totalIn, it.totalOut) }?.coerceAtLeast(1.0) ?: 1.0

    // Thin out labels if there are many points — show at most ~7
    val totalPoints = trendData.size.coerceAtLeast(1)
    val labelEveryN = ((totalPoints / 7.0).toInt()).coerceAtLeast(1)

    Column(modifier = modifier) {
        // ── Bars ──
        Canvas(modifier = Modifier.fillMaxWidth().weight(1f)) {
            val barGroupW = size.width / totalPoints
            val barW      = (barGroupW * 0.35f).coerceAtLeast(2.dp.toPx())
            val gap       = 2.dp.toPx()

            trendData.forEachIndexed { idx, point ->
                val groupLeft = idx * barGroupW
                val inH  = (point.totalIn  / maxVal * size.height).toFloat()
                val outH = (point.totalOut / maxVal * size.height).toFloat()

                if (outH > 0f) drawRoundRect(
                    color = outColor,
                    topLeft = Offset(groupLeft + gap, size.height - outH),
                    size = Size(barW, outH),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
                if (inH > 0f) drawRoundRect(
                    color = inColor,
                    topLeft = Offset(groupLeft + gap + barW + gap, size.height - inH),
                    size = Size(barW, inH),
                    cornerRadius = CornerRadius(2.dp.toPx())
                )
            }
        }

        // ── X-axis labels ──
        Spacer(Modifier.height(4.dp))
        Row(modifier = Modifier.fillMaxWidth()) {
            trendData.forEachIndexed { idx, point ->
                val showLabel = point.label.isNotEmpty() && idx % labelEveryN == 0
                Text(
                    text = if (showLabel) point.label else "",
                    fontSize = 8.sp,
                    color = labelColor.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Clip,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// ─── Member row ───────────────────────────────────────────────────────────────
@Composable
private fun MemberRow(
    member: CategoryKeyword,
    stat: MemberStat?,
    categoryId: Int,
    onEditMemberName: (CategoryKeyword) -> Unit,
    onRemoveMember: (memberName: String, categoryId: Int, keywordId: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val displayName = member.nickName?.takeIf { it.isNotBlank() } ?: member.keyWord
    val avatarColor = txAvatarColor(displayName)

    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(avatarColor),
            contentAlignment = Alignment.Center
        ) {
            Text(
                displayName.take(1).uppercase(),
                color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(displayName, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
            if (stat != null && stat.txCount > 0) {
                Text(
                    "${stat.txCount} txn${if (stat.txCount != 1) "s" else ""}  ·  " +
                    "KES ${String.format("%,.0f", stat.totalOut + stat.totalIn)}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                Text(
                    "No activity this period",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
            }
        }
        // Stat badges
        if (stat != null) {
            Column(horizontalAlignment = Alignment.End) {
                if (stat.totalOut > 0) {
                    Text(
                        "-${String.format("%,.0f", stat.totalOut)}",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                if (stat.totalIn > 0) {
                    Text(
                        "+${String.format("%,.0f", stat.totalIn)}",
                        fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        IconButton(
            onClick = { onEditMemberName(member) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.edit),
                contentDescription = "Edit member",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(
            onClick = { onRemoveMember(displayName, categoryId, member.id) },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                painter = painterResource(R.drawable.remove),
                contentDescription = "Remove member",
                modifier = Modifier.size(15.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

// ─── Small reusable composables ───────────────────────────────────────────────
@Composable
private fun CatStatPill(label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = color.copy(alpha = 0.12f),
        modifier = modifier
    ) {
        Text(
            text = label, fontSize = 11.sp, fontWeight = FontWeight.SemiBold,
            color = color, textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
        )
    }
}

@Composable
private fun MoneyStatBox(label: String, amount: Double, color: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.clip(RoundedCornerShape(12.dp)).background(color.copy(alpha = 0.08f)).padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = "KES ${String.format("%,.0f", abs(amount))}",
            fontWeight = FontWeight.ExtraBold, fontSize = 13.sp, color = color,
            textAlign = TextAlign.Center, maxLines = 1, overflow = TextOverflow.Ellipsis
        )
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun BudgetRow(
    budget: CategoryBudget,
    progress: BudgetWithProgress?,
    onClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val status = progress?.status ?: BudgetStatus.ON_TRACK
    val actualSpending = progress?.actualSpending ?: 0.0
    val percentUsed = progress?.percentUsed ?: 0
    val remaining = progress?.remaining ?: budget.budgetLimit
    val daysLeft = progress?.daysLeft ?: 0
    val fraction = (percentUsed / 100f).coerceIn(0f, 1f)

    val gradientBrush = when (status) {
        BudgetStatus.ON_TRACK -> Brush.linearGradient(
            listOf(colorScheme.primary, colorScheme.primary.copy(alpha = 0.85f))
        )
        BudgetStatus.WARNING -> Brush.linearGradient(
            listOf(Color(0xFFF57C00), Color(0xFFE65100))
        )
        BudgetStatus.EXCEEDED -> Brush.linearGradient(
            listOf(colorScheme.error, colorScheme.error.copy(alpha = 0.85f))
        )
        BudgetStatus.EXPIRED -> Brush.linearGradient(
            listOf(colorScheme.onSurfaceVariant.copy(alpha = 0.6f), colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
        )
    }

    val statusLabel = when (status) {
        BudgetStatus.ON_TRACK -> "Active"
        BudgetStatus.WARNING  -> "Warning"
        BudgetStatus.EXCEEDED -> "Exceeded!"
        BudgetStatus.EXPIRED  -> "Expired"
    }

    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "budgetProgress"
    )

    Card(
        shape = RoundedCornerShape(16.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradientBrush)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row: name + status badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.budget_2),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color.White
                        )
                        Text(
                            text = budget.name ?: "Budget",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(50))
                            .background(Color.White.copy(alpha = 0.2f))
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(Modifier.height(8.dp))

                // Spending + days left row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Text(
                        text = "KES ${String.format("%,.0f", actualSpending)} spent",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (status == BudgetStatus.EXPIRED) "Expired" else "${daysLeft}d left",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }
                Text(
                    text = "of KES ${String.format("%,.0f", budget.budgetLimit)} limit",
                    style = MaterialTheme.typography.labelSmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(Modifier.height(8.dp))

                // Progress bar
                LinearProgressIndicator(
                    progress = { animatedFraction },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color.White,
                    trackColor = Color.White.copy(alpha = 0.3f),
                )

                Spacer(Modifier.height(4.dp))

                // Percent used + remaining row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${percentUsed}% used",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                    Text(
                        text = "KES ${String.format("%,.0f", remaining)} left",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.75f)
                    )
                }

                // Projection insight
                if (progress != null && daysLeft > 0 && actualSpending > 0) {
                    val dbBudget = progress.budget
                    val totalDays = java.time.temporal.ChronoUnit.DAYS.between(dbBudget.startDate, dbBudget.limitDate).toInt().coerceAtLeast(1)
                    val daysElapsed = (totalDays - daysLeft).coerceAtLeast(1)
                    val projectedTotal = actualSpending / daysElapsed * totalDays

                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "⚡ At this rate: KES ${String.format("%,.0f", projectedTotal)} projected by ${budget.limitDate.take(10)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color.White.copy(alpha = 0.8f),
                        fontStyle = FontStyle.Italic
                    )
                }

                Spacer(Modifier.height(10.dp))

                // View Details button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onClick) {
                        Text(
                            text = "View Details",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            style = MaterialTheme.typography.labelSmall
                        )
                        Spacer(Modifier.width(2.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.White
                        )
                    }
                }
            }
        }
    }
}

// ─── Dialogs ──────────────────────────────────────────────────────────────────
@Composable
fun EditNameDialog(
    title: String, label: String, name: String,
    onNameChange: (String) -> Unit, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit $title", fontWeight = FontWeight.Bold) },
        text = {
            OutlinedTextField(
                value = name, onValueChange = onNameChange, label = { Text(label) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                )
            )
        },
        confirmButton = { Button(onClick = onConfirm) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
fun DeleteDialog(
    name: String, categoryDeletion: Boolean, onConfirm: () -> Unit, onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (categoryDeletion) "Delete category?" else "Remove member?", fontWeight = FontWeight.Bold) },
        text = {
            Text(
                if (categoryDeletion)
                    "Delete category \"$name\"? This will also remove all associated members and budgets."
                else
                    "Remove member \"$name\" from this category?"
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
            ) { Text(if (categoryDeletion) "Delete" else "Remove") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true)
@Composable
private fun CategoryDetailsScreenPreview() {
    MaterialTheme {
        CategoryDetailsScreen(
            pullRefreshState = null,
            loadingStatus = LoadingStatus.INITIAL,
            category = com.records.pesa.reusables.transactionCategory,
            isPremium = false,
            selectedPeriod = TimePeriod.THIS_MONTH,
            startDate = LocalDate.now().withDayOfMonth(1),
            endDate = LocalDate.now(),
            showCustomPicker = false,
            totalIn = 12500.0,
            totalOut = 8300.0,
            txCount = 14,
            typeBreakdown = listOf("Send Money" to 5, "Buy Goods" to 4, "Pay Bill" to 3, "Withdraw" to 2),
            trendData = emptyList(),
            memberStats = emptyList(),
            insights = emptyList(),
            onSelectPeriod = {},
            onSetCustomStart = {},
            onSetCustomEnd = {},
            onToggleCustomPicker = {},
            onShowSubscriptionDialog = {},
            onEditCategoryName = {},
            onEditMemberName = {},
            onRemoveMember = { _, _, _ -> },
            onRemoveCategory = { _, _ -> },
            navigateToCategoryBudgetListScreen = { _, _ -> },
            navigateToPreviousScreen = {},
            navigateToMembersAdditionScreen = {},
            navigateToTransactionsScreen = {},
            navigateToAllTransactionsScreen = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ManualTransactionsSection(
    transactions: List<ManualTransaction>,
    mpesaMembers: List<String>,
    manualMembers: List<ManualCategoryMember>,
    onAddTransaction: (memberName: String, isOutflow: Boolean, amount: Double, description: String, date: LocalDate, time: java.time.LocalTime?) -> Unit,
    onDeleteTransaction: (Int) -> Unit,
    onNavigateToAddMember: () -> Unit = {}
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var txToDelete by remember { mutableStateOf<ManualTransaction?>(null) }
    val dateFormatter = remember { DateTimeFormatter.ofPattern("d MMM yyyy") }

    txToDelete?.let { tx ->
        AlertDialog(
            onDismissRequest = { txToDelete = null },
            title = { Text("Delete Transaction") },
            text = { Text("Delete this ${if (tx.isOutflow) "expense" else "income"} of KES ${"%,.2f".format(tx.amount)}?") },
            confirmButton = { TextButton(onClick = { onDeleteTransaction(tx.id); txToDelete = null }) { Text("Delete") } },
            dismissButton = { TextButton(onClick = { txToDelete = null }) { Text("Cancel") } }
        )
    }

    if (showAddDialog) {
        AddManualTransactionDialog(
            mpesaMembers = mpesaMembers,
            manualMembers = manualMembers,
            onNavigateToAddMember = onNavigateToAddMember,
            onConfirm = { memberName, isOutflow, amount, description, date, time ->
                onAddTransaction(memberName, isOutflow, amount, description, date, time)
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }

    val totalOutflow = transactions.filter { it.isOutflow }.sumOf { it.amount }

    ElevatedCard(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().background(
                Brush.linearGradient(listOf(
                    Color(0xFF1565C0).copy(alpha = 0.08f),
                    Color(0xFF0288D1).copy(alpha = 0.04f)
                ))
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.wallet),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "Manual Transactions",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = "Add transaction",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 8.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                )

                if (transactions.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            "No manual transactions yet. Tap + to add cash, bank or any non-M-PESA expense.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                } else {
                    transactions.forEach { tx ->
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = tx.memberName,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = tx.transactionTypeName,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = buildString {
                                        append(tx.date.format(dateFormatter))
                                        tx.time?.let { append("  ${it.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm"))}") }
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "KES ${"%,.2f".format(tx.amount)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (tx.isOutflow) MaterialTheme.colorScheme.error else Color(0xFF2E7D32)
                            )
                            IconButton(
                                onClick = { txToDelete = tx },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.remove),
                                    contentDescription = "Delete",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (tx != transactions.last()) {
                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                        }
                    }

                    if (totalOutflow > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(top = 8.dp),
                            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.End
                        ) {
                            Text(
                                text = "Total spent: KES ${"%,.2f".format(totalOutflow)}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddManualTransactionDialog(
    mpesaMembers: List<String>,
    manualMembers: List<ManualCategoryMember>,
    onNavigateToAddMember: () -> Unit = {},
    onConfirm: (memberName: String, isOutflow: Boolean, amount: Double, description: String, date: LocalDate, time: java.time.LocalTime?) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allMembers = (mpesaMembers + manualMembers.map { it.name }).distinct().sorted()

    var selectedMember by remember { mutableStateOf(allMembers.firstOrNull() ?: "") }
    var memberExpanded by remember { mutableStateOf(false) }
    var isOutflow by remember { mutableStateOf(true) }
    var amountText by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedDate by remember { mutableStateOf(LocalDate.now()) }
    var selectedTime by remember { mutableStateOf<java.time.LocalTime?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Manual Transaction") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {

                // Member picker
                ExposedDropdownMenuBox(expanded = memberExpanded, onExpandedChange = { memberExpanded = it }) {
                    OutlinedTextField(
                        value = selectedMember,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Member") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = memberExpanded) },
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = memberExpanded, onDismissRequest = { memberExpanded = false }) {
                        allMembers.forEach { m ->
                            DropdownMenuItem(text = { Text(m) }, onClick = { selectedMember = m; memberExpanded = false })
                        }
                        DropdownMenuItem(
                            text = {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Text(
                                        "Member not here? Add one",
                                        color = MaterialTheme.colorScheme.primary,
                                        fontSize = 13.sp
                                    )
                                }
                            },
                            onClick = { memberExpanded = false; onNavigateToAddMember() }
                        )
                    }
                }

                // Expense / Income toggle
                Text("Transaction type", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = isOutflow,
                        onClick = { isOutflow = true },
                        label = { Text("Expense") }
                    )
                    FilterChip(
                        selected = !isOutflow,
                        onClick = { isOutflow = false },
                        label = { Text("Income") }
                    )
                }

                // Amount
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it.filter { c -> c.isDigit() || c == '.' } },
                    label = { Text("Amount (KES)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                // Date
                val dateFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy")
                OutlinedTextField(
                    value = selectedDate.format(dateFormatter),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Date") },
                    trailingIcon = {
                        IconButton(onClick = {
                            DatePickerDialog(
                                context,
                                { _, y, m, d -> selectedDate = LocalDate.of(y, m + 1, d) },
                                selectedDate.year, selectedDate.monthValue - 1, selectedDate.dayOfMonth
                            ).show()
                        }) {
                            Icon(painter = painterResource(R.drawable.calendar), contentDescription = "Pick date", modifier = Modifier.size(20.dp))
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Time (optional)
                val timeFormatter = remember { java.time.format.DateTimeFormatter.ofPattern("hh:mm a") }
                OutlinedTextField(
                    value = selectedTime?.format(timeFormatter) ?: "",
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Time (optional)") },
                    placeholder = { Text("Tap to set time") },
                    trailingIcon = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (selectedTime != null) {
                                IconButton(onClick = { selectedTime = null }, modifier = Modifier.size(32.dp)) {
                                    Icon(painter = painterResource(R.drawable.baseline_clear_24), contentDescription = "Clear time", modifier = Modifier.size(16.dp))
                                }
                            }
                            IconButton(onClick = {
                                val now = selectedTime ?: java.time.LocalTime.now()
                                android.app.TimePickerDialog(
                                    context,
                                    { _, hour, minute -> selectedTime = java.time.LocalTime.of(hour, minute) },
                                    now.hour, now.minute, false
                                ).show()
                            }) {
                                Icon(painter = painterResource(R.drawable.calendar), contentDescription = "Pick time", modifier = Modifier.size(20.dp))
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: return@Button
                    if (selectedMember.isNotBlank() && amount > 0) {
                        onConfirm(selectedMember, isOutflow, amount, description, selectedDate, selectedTime)
                    }
                },
                enabled = selectedMember.isNotBlank() && (amountText.toDoubleOrNull() ?: 0.0) > 0
            ) { Text("Add") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

