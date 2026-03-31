package com.records.pesa.ui.screens.dashboard

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
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
import androidx.compose.ui.draw.shadow
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import co.yml.charts.common.extensions.isNotNull
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.dialUssd
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatIsoDateTime2
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.backup.BackupScreenComposable
import com.records.pesa.ui.screens.contact.ContactFormScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetWithProgress
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenComposable
import com.records.pesa.ui.screens.profile.AccountInformationScreenComposable
import com.records.pesa.ui.screens.transactionTypes.TransactionTypesScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionsHubScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionsScreenComposable
import com.records.pesa.ui.screens.transactions.sorted.SortedTransactionsScreenComposable
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import com.records.pesa.ui.screens.components.PermissionExplanationDialog
import com.records.pesa.ui.screens.components.SubscriptionDialog
import kotlinx.coroutines.launch

object HomeScreenDestination: AppNavigation {
    override val title: String = "Home screen"
    override val route: String = "home-screen"
    val screen: String = "screen"
    val routeWithArgs: String = "$route/{$screen}"
}
@OptIn(ExperimentalPermissionsApi::class, ExperimentalPermissionsApi::class)
@Composable
fun HomeScreenComposable(
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    onSwitchTheme: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToTransactionsScreenWithTransactionType: (comment: String, transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    BackHandler(onBack = { activity?.finish() })

    val viewModel: HomeScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()

    val notificationPermissionState = rememberPermissionState(permission = Manifest.permission.POST_NOTIFICATIONS)
    val notificationRequestHandler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            Toast.makeText(context, "Notification permission granted!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(context, "Notification permission denied!", Toast.LENGTH_SHORT).show()
        }
    }

    val callPhoneRequestHandler = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) context.dialUssd("*334#") }

    var showSubscribeDialog by rememberSaveable { mutableStateOf(false) }
    var showFreeTrialDialog by rememberSaveable { mutableStateOf(false) }
    var showBackupDialog by rememberSaveable { mutableStateOf(false) }
    var showCallPermDialog by rememberSaveable { mutableStateOf(false) }
    var showNotificationPermDialog by rememberSaveable { mutableStateOf(false) }

    var currentTab by rememberSaveable { mutableStateOf(HomeScreenTab.HOME) }

    LaunchedEffect(Unit) {
        if (uiState.screen == "backup-screen") {
            currentTab = HomeScreenTab.BACK_UP
            viewModel.resetNavigationScreen()
        }

        // Show permissions sequentially: notifications first, then CALL_PHONE.
        if (!notificationPermissionState.status.isGranted) {
            showNotificationPermDialog = true
        } else {
            // Notification already granted — check CALL_PHONE next
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                != PackageManager.PERMISSION_GRANTED) {
                showCallPermDialog = true
            }
        }
    }

    if (showNotificationPermDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.star,
            title = "Enable Notifications",
            explanation = "Cash Ledger needs notification permission to alert you the moment an M-PESA transaction is detected — even when the app is in the background.\n\nWithout this, transaction alerts will be silently dropped.",
            confirmLabel = "Allow",
            dismissLabel = "Not now",
            onConfirm = {
                showNotificationPermDialog = false
                notificationRequestHandler.launch(Manifest.permission.POST_NOTIFICATIONS)
                // Chain to CALL_PHONE next
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                    showCallPermDialog = true
                }
            },
            onDismiss = {
                showNotificationPermDialog = false
                // Chain to CALL_PHONE next
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                    != PackageManager.PERMISSION_GRANTED) {
                    showCallPermDialog = true
                }
            }
        )
    }

    if (showCallPermDialog) {
        PermissionExplanationDialog(
            icon = R.drawable.refresh,
            title = "Allow Making Calls",
            explanation = "Cash Ledger uses this permission to automatically open the M-PESA USSD menu (*334#) when you tap \"Make a Transaction\" — without you having to press the call button manually.",
            confirmLabel = "Allow",
            dismissLabel = "Not now",
            onConfirm = {
                showCallPermDialog = false
                callPhoneRequestHandler.launch(Manifest.permission.CALL_PHONE)
            },
            onDismiss = { showCallPermDialog = false }
        )
    }

    if (showFreeTrialDialog) {
        FreeTrialDialog(
            days = uiState.freeTrialDays,
            onDismiss = { showFreeTrialDialog = false }
        )
    }

    if (showSubscribeDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscribeDialog = false; onSwitchTheme() },
            onConfirm = {
                showSubscribeDialog = false
                onSwitchTheme()
                navigateToSubscriptionScreen()
            }
        )
    }

    if (showBackupDialog) {
        BackupDialog(
            lastBackup = uiState.userDetails.lastBackup?.let { formatIsoDateTime2(it) } ?: "Never",
            onBackup = {
                showBackupDialog = false
                viewModel.backUpWorker()
                if (!notificationPermissionState.status.isGranted) {
                    notificationRequestHandler.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
                Toast.makeText(context, "Backup initiated - Check notification bar for progress", Toast.LENGTH_LONG).show()
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    Box(
        modifier = Modifier
            .then(
                // Use statusBarsPadding for real device, fallback for preview
                try {
                    Modifier.statusBarsPadding()
                } catch (e: Exception) {
                    Modifier.padding(top = 24.dp)
                }
            )
//            .imePadding()
//            .safeDrawingPadding()
    ) {
        HomeScreen(
            context = context,
            lastBackup = uiState.userDetails.lastBackup?.let { formatIsoDateTime2(it) } ?: "Never",
            freeTrialDays = uiState.freeTrialDays,
            scope = scope,
            firstName = uiState.userDetails.firstName,
            lastName = uiState.userDetails.lastName,
            phoneNumber = uiState.userDetails.phoneNumber,
            darkTheme = uiState.preferences.darkMode,
            currentTab = currentTab,
            onTabChange = { currentTab = it },
            tabs = listOf(
                HomeScreenTabItem("Home", R.drawable.home, HomeScreenTab.HOME),
                HomeScreenTabItem("Transactions", R.drawable.transactions, HomeScreenTab.ALL_TRANSACTIONS),
                HomeScreenTabItem("Categories", R.drawable.categories, HomeScreenTab.CATEGORIES),
                HomeScreenTabItem("Profile", R.drawable.account_info, HomeScreenTab.ACCOUNT_INFO),
            ),
            onTransact = {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE)
                    == PackageManager.PERMISSION_GRANTED) {
                    context.dialUssd("*334#")
                } else {
                    callPhoneRequestHandler.launch(Manifest.permission.CALL_PHONE)
                }
            },
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
            navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToHomeScreen = { currentTab = HomeScreenTab.HOME },
            navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs,
            navigateToLoginScreen = navigateToLoginScreen,
            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
            onSwitchTheme = onSwitchTheme,
            onBackup = { showBackupDialog = true },
            onReviewApp = {
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=com.records.pesa"))
                context.startActivity(intent)
            },
            onShowFreeTrialDetails = { showFreeTrialDialog = true },
            navigateToSubscriptionScreen = navigateToSubscriptionScreen,
            navigateToAccountInfoScreen = { currentTab = HomeScreenTab.ACCOUNT_INFO },
            navigateToBackupScreen = { currentTab = HomeScreenTab.BACK_UP },
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
            navigateToTransactionsScreenWithTransactionType = {transactionType, moneyDirection, startDate, endDate ->
                navigateToTransactionsScreenWithTransactionType("comment", transactionType, moneyDirection, startDate, endDate)
            },
            navigateToUpdatePasswordScreen = navigateToUpdatePasswordScreen,
            budgets = uiState.budgets
        )
    }
}


@Composable
fun HomeScreen(
    context: Context = LocalContext.current,
    lastBackup: String,
    freeTrialDays: Int,
    scope: CoroutineScope?,
    firstName: String?,
    lastName: String?,
    phoneNumber: String,
    darkTheme: Boolean,
    currentTab: HomeScreenTab,
    onTabChange: (HomeScreenTab) -> Unit,
    tabs: List<HomeScreenTabItem>,
    onTransact: () -> Unit,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToLoginScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    navigateToBackupScreen: () -> Unit,
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyDirection: String) -> Unit,
    onSwitchTheme: () -> Unit,
    onBackup: () -> Unit,
    onReviewApp: () -> Unit,
    onShowFreeTrialDetails: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToAccountInfoScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToTransactionsScreenWithTransactionType: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    navigateToUpdatePasswordScreen: () -> Unit,
    budgets: List<BudgetWithProgress> = emptyList(),
    modifier: Modifier = Modifier
) {
    // Intercept back press on non-HOME tabs and return to HOME instead of exiting
    BackHandler(enabled = currentTab != HomeScreenTab.HOME) {
        onTabChange(HomeScreenTab.HOME)
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Track dates from hub to pass to TransactionTypesScreen
        var txTypesStartDate by remember { mutableStateOf("") }
        var txTypesEndDate   by remember { mutableStateOf("") }

        // Content area – fills full screen, scrolls behind the floating bar
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 96.dp)
        ) {
            when(currentTab) {
                HomeScreenTab.HOME -> {
                    DashboardScreenComposable(
                        navigateToTransactionsScreen = navigateToTransactionsScreen,
                        navigateToCategoriesScreen = navigateToCategoriesScreen,
                        navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
                        navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                        navigateToUpdatePasswordScreen = navigateToUpdatePasswordScreen,
                        budgets = budgets,
                        navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
                        navigateToAllBudgets = { onTabChange(HomeScreenTab.BUDGETS) },
                        navigateToBudgetCreationScreen = navigateToBudgetCreationScreen
                    )
                }
                HomeScreenTab.ALL_TRANSACTIONS -> {
                    TransactionsHubScreenComposable(
                        navigateToAllTransactions = navigateToTransactionsScreen,
                        navigateToSortedTransactions = { onTabChange(HomeScreenTab.SORTED_TRANSACTIONS) },
                        navigateToTransactionTypes = { startDate, endDate ->
                            txTypesStartDate = startDate
                            txTypesEndDate   = endDate
                            onTabChange(HomeScreenTab.TRANSACTION_TYPES)
                        },
                        navigateToTransactionDetails = navigateToTransactionDetailsScreen,
                        navigateToEntityTransactions = navigateToEntityTransactionsScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen
                    )
                }
                HomeScreenTab.SORTED_TRANSACTIONS -> {
                    SortedTransactionsScreenComposable(
                        navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        navigateToHomeScreen = navigateToHomeScreen
                    )
                }
                HomeScreenTab.TRANSACTION_TYPES -> {
                    TransactionTypesScreenComposable(
                        navigateToTransactionsScreen = navigateToTransactionsScreenWithTransactionType,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToPreviousScreen = { onTabChange(HomeScreenTab.ALL_TRANSACTIONS) },
                        initialStartDate = txTypesStartDate.ifEmpty { null },
                        initialEndDate   = txTypesEndDate.ifEmpty { null }
                    )
                }
                HomeScreenTab.TRANSACT -> {
                    // Navigate to transactions screen where user can add new transaction
                    TransactionsScreenComposable(
                        navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                        navigateToPreviousScreen = navigateToPreviousScreen,
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                        showBackArrow = false,
                        navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
                    )
                }
                HomeScreenTab.CATEGORIES -> {
                    CategoriesScreenComposable(
                        navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
                        navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
                        navigateToPreviousScreen = navigateToPreviousScreen,
                        navigateToHomeScreen = navigateToHomeScreen,
                        showBackArrow = false,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen
                    )
                }
                HomeScreenTab.BUDGETS -> {
                    BudgetListScreenComposable(
                        navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
                        navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
                        navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
                        navigateToPreviousScreen = navigateToPreviousScreen,
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        showBackArrow = false,
                        modifier = Modifier
                    )
                }
                HomeScreenTab.ACCOUNT_INFO -> {
                    AccountInformationScreenComposable(
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs,
                        navigateToLoginScreen = navigateToLoginScreen,
                        navigateToBackupScreen = navigateToBackupScreen,
                        navigateToSubscriptionPaymentScreen = navigateToSubscriptionScreen,
                        onSwitchTheme = onSwitchTheme,
                        darkMode = darkTheme,
                    )
                }
                HomeScreenTab.BACK_UP -> {
                    BackupScreenComposable(
                        refreshScreen = true,
                        navigateToHomeScreen = navigateToHomeScreen,
                        navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                        navigateToPreviousScreen = navigateToPreviousScreen
                    )
                }
                HomeScreenTab.CONTACT_US -> {
                    ContactFormScreenComposable(
                        navigateToHomeScreen = navigateToHomeScreen
                    )
                }
            }
        }
        
        // Floating Bottom Navigation Bar – overlays content, pinned to bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(horizontal = 12.dp)
                .padding(bottom = 12.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            Card(
                shape = RoundedCornerShape(32.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(72.dp)
                            .padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left 2 tabs
                        tabs.take(2).forEach { item ->
                            BottomNavBarItem(
                                item = item,
                                isSelected = currentTab == item.tab,
                                onClick = { onTabChange(item.tab) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        // Centre Transact FAB
                        Box(
                            modifier = Modifier.weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .shadow(elevation = 6.dp, shape = CircleShape)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() }
                                    ) { onTransact() },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_transact),
                                    contentDescription = "Transact",
                                    modifier = Modifier.size(24.dp),
                                    tint = MaterialTheme.colorScheme.onPrimary
                                )
                            }
                        }
                        // Right 2 tabs
                        tabs.drop(2).forEach { item ->
                            BottomNavBarItem(
                                item = item,
                                isSelected = currentTab == item.tab,
                                onClick = { onTabChange(item.tab) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
            }
        }
    }
}

@Composable
private fun BottomNavBarItem(
    item: HomeScreenTabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { onClick() }
            .padding(vertical = 6.dp)
    ) {
        Icon(
            painter = painterResource(id = item.icon),
            contentDescription = item.name,
            modifier = Modifier.size(22.dp),
            tint = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = item.name,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (isSelected)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
            maxLines = 1
        )
        Spacer(modifier = Modifier.height(4.dp))
        // Active indicator dot
        Box(
            modifier = Modifier
                .size(4.dp)
                .clip(CircleShape)
                .background(
                    if (isSelected) MaterialTheme.colorScheme.primary
                    else Color.Transparent
                )
        )
    }
}

@Composable
fun ThemeSwitcher(
    darkTheme: Boolean = false,
    size: Dp = screenWidth(x = 150.0),
    iconSize: Dp = size / 3,
    padding: Dp = screenWidth(x = 10.0),
    borderWidth: Dp = screenWidth(x = 1.0),
    parentShape: Shape = CircleShape,
    toggleShape: Shape = CircleShape,
    animationSpec: AnimationSpec<Dp> = tween(durationMillis = 300),
    onClick: () -> Unit,
    modifier: Modifier =Modifier
) {
    val offset by animateDpAsState(
        targetValue = if (darkTheme) 0.dp else size,
        animationSpec = animationSpec
    )

    Box(modifier = modifier
        .width(size * 2)
        .height(size)
        .clip(shape = parentShape)
        .clickable { onClick() }
        .background(MaterialTheme.colorScheme.secondaryContainer)
    ) {
        Box(
            modifier = Modifier
                .size(size)
                .offset(x = offset)
                .padding(all = padding)
                .clip(shape = toggleShape)
                .background(MaterialTheme.colorScheme.primary)
        ) {}
        Row(
            modifier = Modifier
                .border(
                    border = BorderStroke(
                        width = borderWidth,
                        color = MaterialTheme.colorScheme.primary
                    ),
                    shape = parentShape
                )
        ) {
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tint = if (darkTheme) MaterialTheme.colorScheme.secondaryContainer
                    else MaterialTheme.colorScheme.primary,
                    painter = painterResource(id = R.drawable.nightlight),
                    contentDescription = "Theme icon",
                    modifier = Modifier.size(iconSize),
                )
            }
            Box(
                modifier = Modifier.size(size),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    tint = if (darkTheme) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondaryContainer,
                    painter = painterResource(id = R.drawable.lightmode),
                    contentDescription = "Theme icon",
                    modifier = Modifier.size(iconSize),
                )
            }
        }
    }
}

@Composable
fun FreeTrialDialog(
    days: Int,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        title = {
            Text(
                text = "Free trial:",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Column {
                Text(
                    text = "We hope you’re enjoying the app! After your free trial, you can support us with a monthly fee of KES 100, paid through M-PESA. There’s no automatic deduction—it’s entirely up to you. Your support helps us maintain the app's services. Thank you!",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Free trial days remaining: $days",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }

        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        },
    )
}

@Composable
fun BackupDialog(
    lastBackup: String,
    onBackup: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        title = {
            Text(
                text = "Backup",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Backup your transactions to avoid losing them",
                    fontSize = screenFontSize(x = 14.0).sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
                Text(
                    text = "Last backup: $lastBackup",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onBackup) {
                Text(
                    text = "Backup",
                    fontSize = screenFontSize(x = 14.0).sp,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Dismiss",
                    fontSize = screenFontSize(x = 14.0).sp,
                )
            }
        }
    )
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun HomeScreenPreview() {
    val tabs = listOf(
        HomeScreenTabItem(
            name = "Home",
            icon = R.drawable.baseline_home_24,
            tab = HomeScreenTab.HOME
        ),
        HomeScreenTabItem(
            name = "All transactions",
            icon = R.drawable.transactions,
            tab = HomeScreenTab.ALL_TRANSACTIONS
        ),
        HomeScreenTabItem(
            name = "Categories",
            icon = R.drawable.categories,
            tab = HomeScreenTab.CATEGORIES
        ),
        HomeScreenTabItem(
            name = "Budgets",
            icon = R.drawable.budget_pic,
            tab = HomeScreenTab.BUDGETS
        ),
    )

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.HOME)
    }
    CashLedgerTheme {
        HomeScreen(
            lastBackup = "",
            freeTrialDays = 0,
            scope = null,
            firstName = null,
            lastName = null,
            phoneNumber = "",
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs,
            onTransact = {},
            navigateToTransactionsScreen = {},
            navigateToCategoriesScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToCategoryDetailsScreen = {},
            navigateToBudgetInfoScreen = {},
            navigateToBudgetCreationScreen = {},
            navigateToBudgetCreationScreenWithCategoryId = {},
            navigateToPreviousScreen = {},
            navigateToHomeScreen = {},
            navigateToLoginScreenWithArgs = {phoneNumber, password ->  },
            navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyDirection ->  },
            darkTheme = false,
            onSwitchTheme = {},
            onReviewApp = {},
            onBackup = {},
            onShowFreeTrialDetails = {},
            navigateToSubscriptionScreen = {},
            navigateToAccountInfoScreen = {},
            navigateToTransactionDetailsScreen = {},
            navigateToBackupScreen = {},
            navigateToLoginScreen = {},
            navigateToTransactionsScreenWithTransactionType = {transactionType, moneyDirection, startDate, endDate ->  },
            navigateToUpdatePasswordScreen = {}
        )
    }
}