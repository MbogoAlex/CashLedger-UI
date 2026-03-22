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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Card
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
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
import com.records.pesa.ui.screens.transactions.Chart6
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.delay
import java.time.LocalDate
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
            .safeDrawingPadding()
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
            }
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
            .padding(
                top = screenHeight(x = 16.0),
                bottom = screenHeight(x = 20.0)
            )
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
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )

        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        
        // USSD Quick Action Button
        val context = LocalContext.current
        UssdQuickActionButton(
            onClick = {
                context.dialUssd("*334#")
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )
        
        Spacer(modifier = Modifier.height(screenHeight(x = 12.0)))
        
        // Time Period Selector - Clean dropdown
        TimePeriodSelector(
            selectedPeriod = selectedTimePeriod,
            availableYears = availableYears,
            onPeriodSelected = onPeriodSelected,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )
        
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        
        // Transaction Type Breakdown
        if (moneyInCategories.isNotEmpty() || moneyOutCategories.isNotEmpty()) {
            CompactTransactionBreakdown(
                moneyInCategories = moneyInCategories,
                moneyOutCategories = moneyOutCategories,
                periodLabel = selectedTimePeriod.getDisplayName(),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = screenWidth(x = 16.0))
            )
            
            Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        }
        
        // Top Senders/Receivers
        TopPeopleSection(
            transactions = periodTransactions,
            periodLabel = selectedTimePeriod.getDisplayName(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )
        
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        
        // Recent Transactions Section
        RecentTransactionsSection(
            transactions = transactions,
            onSeeAllClick = navigateToTransactionsScreen,
            onTransactionClick = navigateToTransactionDetailsScreen,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )
        
        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
        
        // Categories Section
        CategoriesSection(
            categories = transactionCategories,
            premium = premium,
            onSeeAllClick = navigateToCategoriesScreen,
            onAddClick = navigateToCategoryAdditionScreen,
            onCategoryClick = navigateToCategoryDetailsScreen,
            onShowSubscriptionDialog = onShowSubscriptionDialog,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = screenWidth(x = 16.0))
        )

    }


}

/**
 * Categories Section - Modern card-based categories display
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
    Column(
        modifier = modifier.fillMaxWidth()
    ) {
        // Section header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            if (categories.isEmpty()) {
                TextButton(
                    onClick = onAddClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_add),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Add",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            } else {
                TextButton(
                    onClick = onSeeAllClick,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = "See All",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        
        // Categories content
        if (categories.isEmpty()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "📊",
                        fontSize = 40.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "No Categories Yet",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "Create categories to analyze money flow and track spending patterns",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        } else {
            // Show top 2 categories
            categories.take(2).forEachIndexed { index, category ->
                TransactionCategoryCell(
                    transactionCategory = category,
                    navigateToCategoryDetailsScreen = {
                        if (index != 0 && !premium) {
                            onShowSubscriptionDialog()
                        } else {
                            onCategoryClick(category.id.toString())
                        }
                    },
                    modifier = Modifier
                        .clickable {
                            if (index != 0 && !premium) {
                                onShowSubscriptionDialog()
                            } else {
                                onCategoryClick(category.id.toString())
                            }
                        }
                        .padding(bottom = 8.dp)
                )
            }
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
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Go premium?",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .padding(10.dp)
                ) {
                    Text(
                        text = "Ksh100.0 premium monthly fee",
                        fontSize = screenFontSize(x = 14.0).sp,
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "1. See transactions and export reports of more than one months",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "2. Backup your transactions",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "3. Manage more than one category",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(
                        text = "4. Use in dark mode",
                        fontSize = screenFontSize(x = 14.0).sp
                    )

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Subscribe",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
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