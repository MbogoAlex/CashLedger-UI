package com.records.pesa.ui.screens

import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Surface
import androidx.compose.runtime.remember
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.ui.window.Dialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.extensions.isNotNull
import co.yml.charts.common.model.Point
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionCategoryCell
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.composables.TimePeriodSelector
import com.records.pesa.composables.TransactionTypeBreakdownCard
import com.records.pesa.composables.UssdQuickActionButton
import com.records.pesa.composables.MoneyFlowLineChart
import com.records.pesa.composables.HeroBalanceCard
import com.records.pesa.composables.QuickStatsGrid
import com.records.pesa.composables.VisualChartSection
import com.records.pesa.composables.RecentTransactionsSection
import com.records.pesa.composables.CompactTransactionBreakdown
import com.records.pesa.composables.TopPeopleSection
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.functions.dialUssd
import com.records.pesa.models.TimePeriod
import com.records.pesa.models.TransactionTypeSummary
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.GroupedTransactionData
import com.records.pesa.models.transaction.MonthlyTransaction
import com.records.pesa.models.transaction.SortedTransactionItem
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.groupedTransactions
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.auth.PasswordInputField
import com.records.pesa.ui.screens.dashboard.budget.BudgetStatus
import com.records.pesa.ui.screens.dashboard.budget.BudgetWithProgress
import com.records.pesa.ui.screens.transactions.Chart6
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
import com.records.pesa.ui.screens.components.SubscriptionDialog
import androidx.compose.foundation.horizontalScroll
import androidx.compose.animation.animateColorAsState
import com.records.pesa.ui.screens.components.txAvatarColor
import java.time.Month

object DashboardScreenDestination: AppNavigation {
    override val title: String = "Dashboard screen"
    override val route: String = "dashboard-screen"

}
@Composable
fun DashboardScreenComposable(
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    budgets: List<BudgetWithProgress> = emptyList(),
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit = {},
    navigateToAllBudgets: () -> Unit = {},
    navigateToBudgetCreationScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: DashboardScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val appVersion = 143.0

    if (uiState.appVersion.isNotNull() && appVersion != uiState.appVersion) {
        LaunchedEffect(Unit) {
            Toast.makeText(context, "New version available", Toast.LENGTH_LONG).show()
            delay(5000)
            val intent = Intent(
                Intent.ACTION_VIEW,
                Uri.parse("https://play.google.com/store/apps/details?id=com.records.pesa")
            )
            context.startActivity(intent)
        }
    }

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
    }


    var showPasswordDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showIncorrectPasswordText by rememberSaveable {
        mutableStateOf(false)
    }

    if(showPasswordDialog) {
        PasswordDialog(
            password = uiState.userPassword,
            showIncorrectPasswordText = showIncorrectPasswordText,
            onEnterPassword = viewModel::updatePassword,
            onConfirm = {
                if(uiState.userPassword == uiState.userDetails!!.password) {
                    viewModel.changeBalanceVisibility()
                    showPasswordDialog = false
                    showIncorrectPasswordText = false
                    viewModel.updatePassword("")
                } else {
                    showIncorrectPasswordText = true
                }
            },
            onDismiss = {
                showPasswordDialog = false
                showIncorrectPasswordText = false
                viewModel.updatePassword("")
            },
            navigateToUpdatePasswordScreen = {
                showIncorrectPasswordText = false
                viewModel.updatePassword("")
                navigateToUpdatePasswordScreen()
            }
        )
    }

    if(showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscriptionDialog = false
            },
            onConfirm = {
                showSubscriptionDialog = false
                navigateToSubscriptionScreen()
            }
        )
    }

    if(uiState.showSubscriptionExpiredDialog) {
        SubscriptionExpiredDialog(
            onDismiss = viewModel::dismissSubscriptionExpiredDialog,
            onSubscribe = {
                viewModel.dismissSubscriptionExpiredDialog()
                navigateToSubscriptionScreen()
            }
        )
    }

    if(uiState.showSubscriptionActivatedDialog) {
        SubscriptionActivatedDialog(
            onDismiss = viewModel::dismissSubscriptionActivatedDialog
        )
    }

    Box(
        modifier = modifier
//             .safeDrawingPadding()
    ) {
        DashboardScreen(
            premium = uiState.preferences?.paid == true || uiState.userDetails?.phoneNumber == "0888888888",
            totalInToday = formatMoneyValue(uiState.todayTotalIn),
            totalOutToday = formatMoneyValue(uiState.todayTotalOut),
            monthlyTotalIn = formatMoneyValue(uiState.monthlyInTotal),
            monthlyTotalOut = formatMoneyValue(uiState.monthlyOutTotal),
            firstTransactionDate = uiState.firstTransactionDate,
            moneyInPointsData = uiState.moneyInPointsData,
            moneyOutPointsData = uiState.moneyOutPointsData,
            maxAmount = uiState.maxAmount,
            groupedTransactions = uiState.groupedTransactions,
            monthlyTransactions = uiState.monthlyTransactions,
            sortedTransactionItems = uiState.sortedTransactionItems,
            transactions = uiState.transactions,
            selectableYears = uiState.selectableYears,
            selectableMonths = uiState.selectableMonths,
            selectedYear = uiState.year,
            selectedMonth = uiState.month,
            onSelectYear = {
                viewModel.updateYear(it)
            },
            onSelectMonth = {
                viewModel.updateMonth(it)
            },
            transactionCategories = uiState.categories,
            currentBalance = formatMoneyValue(uiState.currentBalance),
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = true
            },
            onToggleBalanceVisibility = {
                if(uiState.preferences.showBalance) {
                    viewModel.changeBalanceVisibility()
                } else {
                    showPasswordDialog = true
                }
            },
            showBalance = uiState.preferences.showBalance,
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
            // Time Period Selector parameters
            selectedTimePeriod = uiState.selectedTimePeriod,
            availableYears = uiState.availableYears,
            periodTotalIn = formatMoneyValue(uiState.periodTotalIn),
            periodTotalOut = formatMoneyValue(uiState.periodTotalOut),
            transactionTypeBreakdown = uiState.transactionTypeBreakdown,
            moneyInCategories = uiState.moneyInCategories,
            moneyOutCategories = uiState.moneyOutCategories,
            periodTransactions = uiState.periodTransactions,
            onPeriodSelected = { period ->
                viewModel.updateSelectedPeriod(period)
            },
            budgets = budgets,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToAllBudgets = navigateToAllBudgets,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen
        )
    }
}

@Composable
fun DashboardScreen(
    premium: Boolean,
    totalInToday: String,
    totalOutToday: String,
    monthlyTotalIn: String,
    monthlyTotalOut: String,
    firstTransactionDate: String,
    moneyInPointsData: List<Point>,
    moneyOutPointsData: List<Point>,
    maxAmount: Float = 0.0f,
    groupedTransactions: List<GroupedTransactionData>,
    sortedTransactionItems: List<SortedTransactionItem>,
    monthlyTransactions: List<MonthlyTransaction>,
    transactions: List<TransactionItem>,
    transactionCategories: List<TransactionCategory>,
    currentBalance: String,
    selectedYear: String,
    selectedMonth: String,
    selectableMonths: List<String>,
    selectableYears: List<String>,
    onSelectMonth: (month: String) -> Unit,
    onSelectYear: (month: String) -> Unit,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    onToggleBalanceVisibility: () -> Unit,
    showBalance: Boolean,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    // Time Period Selector parameters
    selectedTimePeriod: TimePeriod = TimePeriod.TODAY,
    availableYears: List<Int> = emptyList(),
    periodTotalIn: String = "Ksh 0.00",
    periodTotalOut: String = "Ksh 0.00",
    transactionTypeBreakdown: List<TransactionTypeSummary> = emptyList(),
    moneyInCategories: List<TransactionTypeSummary> = emptyList(),
    moneyOutCategories: List<TransactionTypeSummary> = emptyList(),
    periodTransactions: List<TransactionItem> = emptyList(),
    onPeriodSelected: (TimePeriod) -> Unit = {},
    budgets: List<BudgetWithProgress> = emptyList(),
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit = {},
    navigateToAllBudgets: () -> Unit = {},
    navigateToBudgetCreationScreen: () -> Unit = {},
    modifier: Modifier = Modifier
) {

    var selectYearExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    var selectMonthExpanded by rememberSaveable {
        mutableStateOf(false)
    }

    val oneMonthBack = LocalDate.now().minusMonths(1).month
    
    // Animated entrance
    LaunchedEffect(Unit) {
        // Trigger any lifecycle animations here
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 40.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Hero Balance Card with integrated stats
        HeroBalanceCard(
            balance = currentBalance,
            showBalance = showBalance,
            onToggleVisibility = onToggleBalanceVisibility,
            firstTransactionDate = firstTransactionDate,
            periodLabel = selectedTimePeriod.getDisplayName(),
            moneyIn = periodTotalIn,
            moneyOut = periodTotalOut,
            selectedTimePeriod = selectedTimePeriod,
            availableYears = availableYears,
            onPeriodSelected = onPeriodSelected,
            modifier = Modifier.fillMaxWidth()
        )

        // USSD Quick Action Button
        val context = LocalContext.current
        UssdQuickActionButton(
            onClick = {
                context.dialUssd("*334#")
            },
            modifier = Modifier.fillMaxWidth()
        )

        // Recent Transactions Section
        RecentTransactionsSection(
            transactions = transactions,
            onSeeAllClick = navigateToTransactionsScreen,
            onTransactionClick = navigateToTransactionDetailsScreen,
            modifier = Modifier.fillMaxWidth()
        )

        // Categories Section
        CategoriesSection(
            categories = transactionCategories,
            premium = premium,
            onSeeAllClick = navigateToCategoriesScreen,
            onAddClick = navigateToCategoryAdditionScreen,
            onCategoryClick = navigateToCategoryDetailsScreen,
            onShowSubscriptionDialog = onShowSubscriptionDialog,
            modifier = Modifier.fillMaxWidth()
        )

        // Budget Health Widget
        BudgetHealthWidget(
            budgets = budgets,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToAllBudgets = navigateToAllBudgets,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
            modifier = Modifier.fillMaxWidth()
        )
    }


}

/**
 * Categories Section — header inside ElevatedCard (BudgetHealthWidget pattern)
 */
@Composable
fun CategoriesSection(
    categories: List<TransactionCategory>,
    premium: Boolean,
    onSeeAllClick: () -> Unit,
    onAddClick: () -> Unit,
    onCategoryClick: (String) -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val quickPicks = listOf(
        "Groceries", "Transport", "Rent", "Entertainment",
        "Food & Drink", "Shopping", "Savings", "Utilities",
        "School Fees", "Healthcare", "Airtime & Data", "Family"
    )
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    if (categories.isEmpty()) {
        ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(
                        primary.copy(alpha = 0.12f),
                        tertiary.copy(alpha = 0.06f),
                        primary.copy(alpha = 0.04f)
                    ))
                )
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Header row inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.categories),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        TextButton(onClick = onAddClick, contentPadding = PaddingValues(0.dp)) {
                            Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(14.dp), tint = primary)
                            Spacer(Modifier.width(4.dp))
                            Text("New", style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.SemiBold)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Icon(
                        painter = painterResource(R.drawable.chart),
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = primary
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = "See Where Your Money Really Goes",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = "Categories group your M-PESA transactions so you can track spending on rent, food, transport and more — all in one place. You can also set budgets per category.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(16.dp))
                    // Quick picks
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(13.dp),
                            tint = primary
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = "Quick picks",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        quickPicks.forEach { suggestion ->
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = txAvatarColor(suggestion).copy(alpha = 0.15f),
                                modifier = Modifier.clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) { onAddClick() }
                            ) {
                                Text(
                                    text = suggestion,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Medium,
                                    color = txAvatarColor(suggestion),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Button(
                        onClick = onAddClick,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(painter = painterResource(R.drawable.ic_add), contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Create a Category", fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }
    } else {
        ElevatedCard(modifier = modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
            Box(
                modifier = Modifier.fillMaxWidth().background(
                    Brush.linearGradient(listOf(
                        primary.copy(alpha = 0.12f),
                        tertiary.copy(alpha = 0.06f),
                        primary.copy(alpha = 0.04f)
                    ))
                )
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    // Header row inside card
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                painter = painterResource(R.drawable.categories),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = primary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text("Categories", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }
                        TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(0.dp)) {
                            Text("See All", style = MaterialTheme.typography.labelSmall, color = primary, fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(2.dp))
                            Icon(painter = painterResource(R.drawable.ic_arrow_right), contentDescription = null, modifier = Modifier.size(14.dp), tint = primary)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    categories.take(3).forEachIndexed { index, category ->
                        val isLocked = index != 0 && !premium
                        val totalIn = remember(category) {
                            category.transactions.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
                        }
                        val totalOut = remember(category) {
                            category.transactions.filter { it.transactionAmount < 0 }.sumOf { kotlin.math.abs(it.transactionAmount) }
                        }
                        val initials = category.name.trim().split(" ")
                            .mapNotNull { it.firstOrNull()?.uppercaseChar()?.toString() }
                            .take(2).joinToString("").ifEmpty { category.name.take(2).uppercase() }
                        val avatarColor = txAvatarColor(category.name)
                        val budget = category.budgets.firstOrNull()

                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 4.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ) {
                                    if (isLocked) onShowSubscriptionDialog()
                                    else onCategoryClick(category.id.toString())
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            // Avatar
                            Box(contentAlignment = Alignment.Center) {
                                Box(modifier = Modifier.size(46.dp).clip(CircleShape).background(avatarColor.copy(alpha = 0.15f)))
                                Box(modifier = Modifier.size(40.dp).clip(CircleShape).background(avatarColor), contentAlignment = Alignment.Center) {
                                    Text(initials, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                            // Name + pills
                            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Text(
                                        text = category.name,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (isLocked) {
                                        Icon(painter = painterResource(R.drawable.lock), contentDescription = "Premium", modifier = Modifier.size(13.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    Box(
                                        modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                            .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text("${category.transactions.size} txn", fontSize = 10.sp, fontWeight = FontWeight.Medium, color = primary)
                                    }
                                    if (budget != null) {
                                        Box(
                                            modifier = Modifier.clip(RoundedCornerShape(4.dp))
                                                .background(
                                                    if (budget.limitReached) MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                                    else MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                                                )
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                "Budget: Ksh ${String.format("%,.0f", budget.budgetLimit)}",
                                                fontSize = 10.sp, fontWeight = FontWeight.Medium,
                                                color = if (budget.limitReached) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.tertiary
                                            )
                                        }
                                    }
                                }
                            }
                            // Money totals
                            Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                Text("+Ksh ${String.format("%,.0f", totalIn)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                Text("-Ksh ${String.format("%,.0f", totalOut)}", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }
        }
    }
}



@Composable
fun BudgetHealthWidget(
    budgets: List<BudgetWithProgress>,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToAllBudgets: () -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val primary = MaterialTheme.colorScheme.primary
    val tertiary = MaterialTheme.colorScheme.tertiary

    ElevatedCard(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primary.copy(alpha = 0.12f),
                            tertiary.copy(alpha = 0.06f),
                            primary.copy(alpha = 0.04f)
                        )
                    )
                )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                // Header row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.budget_2),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = primary
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Budget Health",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    TextButton(onClick = navigateToAllBudgets) {
                        Text(
                            text = "View All",
                            style = MaterialTheme.typography.labelSmall,
                            color = primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = primary
                        )
                    }
                }

                if (budgets.isEmpty()) {
                    // Empty state — entice user to create a budget
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Stay on top of your spending",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Set budgets per category and get instant alerts when you're close to the limit — before it's too late.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = navigateToBudgetCreationScreen,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.ic_add),
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Create Your First Budget",
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    val active = budgets.count { it.status != BudgetStatus.EXPIRED }
                    val exceeded = budgets.count { it.status == BudgetStatus.EXCEEDED }
                    val onTrack = budgets.count { it.status == BudgetStatus.ON_TRACK }
                    Text(
                        text = "$active active · $exceeded exceeded · $onTrack on track",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val top3 = budgets
                        .sortedWith(
                            compareByDescending<BudgetWithProgress> { it.status == BudgetStatus.EXCEEDED }
                                .thenByDescending { it.percentUsed }
                        )
                        .take(3)

                    top3.forEachIndexed { index, item ->
                        if (index > 0) {
                            HorizontalDivider(
                                modifier = Modifier.padding(vertical = 6.dp),
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                        BudgetHealthRow(
                            item = item,
                            onClick = { navigateToBudgetInfoScreen(item.budget.id.toString()) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BudgetHealthRow(
    item: BudgetWithProgress,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statusColor = when (item.status) {
        BudgetStatus.ON_TRACK -> MaterialTheme.colorScheme.primary
        BudgetStatus.WARNING  -> Color(0xFFF57C00)
        BudgetStatus.EXCEEDED -> MaterialTheme.colorScheme.error
        BudgetStatus.EXPIRED  -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(statusColor)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = item.budget.name,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (item.isOverBudget)
                        "Exceeded ${formatMoneyValue(item.actualSpending - item.budget.budgetLimit)}"
                    else
                        "${item.daysLeft}d left",
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
            Spacer(modifier = Modifier.height(3.dp))
            LinearProgressIndicator(
                progress = { (item.percentUsed / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = statusColor,
                trackColor = statusColor.copy(alpha = 0.15f),
                strokeCap = StrokeCap.Round
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = "${item.percentUsed}% · ${formatMoneyValue(item.actualSpending)} of ${formatMoneyValue(item.budget.budgetLimit)}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}



@Composable
fun HeaderSection(
    totalInToday: String,
    totalOutToday: String,
    currentBalance: String,
    firstTransactionDate: String,
    showBalance: Boolean,
    onToggleBalanceVisibility: () -> Unit,
    modifier: Modifier = Modifier
) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "M-PESA transactions since $firstTransactionDate",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .padding(screenWidth(x = 10.0))
                )
            }

        }
        Spacer(
            modifier = Modifier.height(screenHeight(x = 16.0))
        )
        Text(
            text = "Account balance",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (showBalance) currentBalance else "*".repeat(currentBalance.length),
                fontSize = screenFontSize(x = 20.0).sp,
                fontWeight = FontWeight.Bold
            )
            IconButton(
                onClick = onToggleBalanceVisibility
            ) {
                Icon(
                    painter = if (showBalance) painterResource(id = R.drawable.visibility_off) else painterResource(
                        id = R.drawable.visibility_on
                    ),
                    contentDescription = null,
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
        Text(
            text = "${formatLocalDate(LocalDate.now())} (today)",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = screenWidth(x = 16.0)
                )
        ) {
            Card {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(screenWidth(x = 16.0))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_downward),
                            contentDescription = null,
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                        Text(
                            text = totalInToday,
                            fontSize = screenFontSize(x = 14.0).sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surfaceTint
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Card {
                Box {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .padding(screenWidth(x = 16.0))
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.arrow_upward),
                            contentDescription = null,
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                        Text(
                            text = totalOutToday,
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 14.0).sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
    }

}

@Composable
fun SubscriptionExpiredDialog(
    onDismiss: () -> Unit,
    onSubscribe: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Icon circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.errorContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.subscription_expired),
                        contentDescription = "Subscription expired",
                        tint = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Subscription Expired",
                    fontSize = screenFontSize(x = 18.0).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Your MLedger subscription has expired. Renew your subscription to continue enjoying premium features.",
                    fontSize = screenFontSize(x = 14.0).sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                // Subscribe button
                Button(
                    onClick = onSubscribe,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.star),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Renew Subscription",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Later button
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Maybe Later",
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun SubscriptionActivatedDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            shape = RoundedCornerShape(24.dp),
            modifier = modifier.fillMaxWidth()
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(24.dp)
            ) {
                // Icon circle
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        painter = painterResource(R.drawable.subscription_activated),
                        contentDescription = "Subscription activated",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(40.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "You're Premium! 🎉",
                    fontSize = screenFontSize(x = 18.0).sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "Welcome to MLedger Premium! You now have full access to all features including backup, advanced reports, and more.",
                    fontSize = screenFontSize(x = 14.0).sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        painter = painterResource(R.drawable.check),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Get Started",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun PasswordDialog(
    password: String,
    showIncorrectPasswordText: Boolean,
    onEnterPassword: (value: String) -> Unit,
    onConfirm: () -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    onDismiss: () -> Unit
) {
    var passwordVisibility by rememberSaveable {
        mutableStateOf(false)
    }
    AlertDialog(
        title = {
            Text(
                text = "Enter password to show balance",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 16.0).sp
            )
        },
        text = {
            Column {
                PasswordInputField(
                    heading = "Password",
                    value = password,
                    trailingIcon = R.drawable.visibility_on,
                    onValueChange = onEnterPassword,
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done,
                        keyboardType = KeyboardType.Password
                    ),
                    visibility = passwordVisibility,
                    onChangeVisibility = { passwordVisibility = !passwordVisibility }
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                if(showIncorrectPasswordText) {
                    Text(
                        text = "Incorrect password",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                TextButton(
                    onClick = navigateToUpdatePasswordScreen,
                    modifier = Modifier
                        .align(Alignment.Start)
                ) {
                    Text(
                        text = "Forgot password?",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier
            ) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Done",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}


@Preview(showBackground = true, showSystemUi = true)
@Composable
fun DashboardScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        DashboardScreen(
            navigateToTransactionsScreen = {},
            firstTransactionDate = LocalDate.now().toString(),
            transactions = transactions,
            currentBalance = "Ksh 5,350",
            maxAmount = groupedTransactions.maxOf { maxOf(it.moneyIn, it.moneyOut) },
            groupedTransactions = groupedTransactions,
            monthlyTransactions = emptyList(),
            moneyInPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyIn)},
            moneyOutPointsData = groupedTransactions.mapIndexed { index, transaction ->
                Point(index.toFloat(), transaction.moneyOut)
            },
            selectableMonths = emptyList(),
            selectedYear = "2024",
            selectableYears = emptyList(),
            selectedMonth = "AUGUST",
            onSelectMonth = {},
            onSelectYear = {},
            transactionCategories = transactionCategories,
            navigateToCategoriesScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToCategoryDetailsScreen = {},
            onShowSubscriptionDialog = {},
            premium = false,
            totalInToday = "",
            totalOutToday = "",
            monthlyTotalIn = "",
            monthlyTotalOut = "",
            showBalance = true,
            onToggleBalanceVisibility = {},
            navigateToTransactionDetailsScreen = {},
            sortedTransactionItems = emptyList(),
            selectedTimePeriod = TimePeriod.TODAY,
            availableYears = listOf(2026, 2025, 2024),
            periodTotalIn = "Ksh 0.00",
            periodTotalOut = "Ksh 0.00",
            transactionTypeBreakdown = emptyList(),
            onPeriodSelected = {}
        )
    }
}