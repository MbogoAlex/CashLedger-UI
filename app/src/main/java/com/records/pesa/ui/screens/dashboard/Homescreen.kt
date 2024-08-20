package com.records.pesa.ui.screens.dashboard

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.widget.Space
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Divider
import androidx.compose.material3.DrawerState
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.HomeScreenTab
import com.records.pesa.reusables.HomeScreenTabItem
import com.records.pesa.ui.screens.DashboardScreenComposable
import com.records.pesa.ui.screens.dashboard.budget.BudgetListScreenComposable
import com.records.pesa.ui.screens.dashboard.category.CategoriesScreenComposable
import com.records.pesa.ui.screens.profile.AccountInformationScreenComposable
import com.records.pesa.ui.screens.transactionTypes.TransactionTypesScreenComposable
import com.records.pesa.ui.screens.transactions.TransactionsScreenComposable
import com.records.pesa.ui.theme.CashLedgerTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object HomeScreenDestination: AppNavigation {
    override val title: String = "Home screen"
    override val route: String = "home-screen"

}
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
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyIn: Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToTransactionsScreenWithTransactionType: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = (LocalContext.current as? Activity)
    BackHandler(onBack = {activity?.finish()})

    val viewModel: HomeScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val tabs = listOf(
        HomeScreenTabItem(
            name = "Home",
            icon = R.drawable.home,
            tab = HomeScreenTab.HOME
        ),
        HomeScreenTabItem(
            name = "All transactions",
            icon = R.drawable.transactions,
            tab = HomeScreenTab.ALL_TRANSACTIONS
        ),
        HomeScreenTabItem(
            name = "Transaction types",
            icon = R.drawable.types,
            tab = HomeScreenTab.TRANSACTION_TYPES
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
        HomeScreenTabItem(
            name = "Account information",
            icon = R.drawable.account_info,
            tab = HomeScreenTab.ACCOUNT_INFO
        ),
    )

    var showSubscribeDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var currentTab by rememberSaveable {
        mutableStateOf(HomeScreenTab.HOME)
    }

    if(showSubscribeDialog) {
        SubscriptionDialog(
            onDismiss = {
                showSubscribeDialog = false
                onSwitchTheme()
            },
            onConfirm = {
                showSubscribeDialog = false
                onSwitchTheme()
                navigateToSubscriptionScreen()
            }
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        HomeScreen(
            scope = scope,
            drawerState = drawerState,
            firstName = uiState.userDetails.firstName,
            lastName = uiState.userDetails.lastName,
            phoneNumber = uiState.userDetails.phoneNumber,
            darkTheme = uiState.userDetails.darkThemeSet,
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs,
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToCategoriesScreen = navigateToCategoriesScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen,
            navigateToBudgetCreationScreenWithCategoryId = navigateToBudgetCreationScreenWithCategoryId,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToHomeScreen = {
                currentTab = HomeScreenTab.HOME
            },
            navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs,
            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
            onSwitchTheme = {
                onSwitchTheme()
                scope.launch {
                    delay(1000)
                    drawerState.close()
                }
                if(uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0179189199") {
                    Toast.makeText(context, "Theme switched", Toast.LENGTH_SHORT).show()
                } else {
                    showSubscribeDialog = true
                }
            },
            onReviewApp = {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=com.records.pesa")
                )
                context.startActivity(intent)
            },
            navigateToSubscriptionScreen = navigateToSubscriptionScreen,
            navigateToAccountInfoScreen = {
                currentTab = HomeScreenTab.ACCOUNT_INFO
            },
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
            navigateToTransactionsScreenWithTransactionType = navigateToTransactionsScreenWithTransactionType
        )
    }
}

@Composable
fun HomeScreen(
    scope: CoroutineScope?,
    drawerState: DrawerState?,
    firstName: String?,
    lastName: String?,
    phoneNumber: String,
    darkTheme: Boolean,
    currentTab: HomeScreenTab,
    onTabChange: (HomeScreenTab) -> Unit,
    tabs: List<HomeScreenTabItem>,
    navigateToTransactionsScreen: () -> Unit,
    navigateToCategoriesScreen: () -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    navigateToHomeScreen: () -> Unit,
    navigateToEntityTransactionsScreen: (userId: String, transactionType: String, entity: String, startDate: String, endDate: String, times: String, moneyIn: Boolean) -> Unit,
    onSwitchTheme: () -> Unit,
    onReviewApp: () -> Unit,
    navigateToSubscriptionScreen: () -> Unit,
    navigateToAccountInfoScreen: () -> Unit,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToTransactionsScreenWithTransactionType: (transactionType: String?, moneyDirection: String, startDate: String, endDate: String) -> Unit,
    modifier: Modifier = Modifier
) {
    BoxWithConstraints {
        when(maxWidth) {
            in 0.dp..320.dp -> {
                ModalNavigationDrawer(
                    drawerState = drawerState!!,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(
                                            horizontal = 16.dp
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.account_info),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(30.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    (if(firstName.isNullOrEmpty() && lastName.isNullOrEmpty()) phoneNumber else if(firstName.isNotNull() && lastName.isNotNull()) "$firstName $lastName" else if (firstName.isNullOrEmpty() && lastName.isNotNull()) lastName else if (lastName.isNotNull() && firstName.isNullOrEmpty()) lastName else phoneNumber)?.let {
                                        Text(
                                            text = it,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    ThemeSwitcher(
                                        darkTheme = darkTheme,
                                        size = 25.dp,
                                        padding = 5.dp,
                                        onClick = onSwitchTheme,
                                        modifier = Modifier
                                            .padding(
                                                end = 8.dp
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(15.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(15.dp))
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    for(tab in tabs) {
                                        NavigationDrawerItem(
                                            label = {
                                                Row {
                                                    Icon(
                                                        painter = painterResource(id = tab.icon),
                                                        contentDescription = tab.name
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(
                                                        text = tab.name,
                                                        fontSize = 14.sp
                                                    )
                                                }
                                            },
                                            selected = currentTab == tab.tab,
                                            onClick = {
                                                onTabChange(tab.tab)
                                                scope!!.launch {
                                                    drawerState.close()
                                                }
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                onReviewApp()
                                            }
                                            .padding(
                                                horizontal = 16.dp
                                            )
                                            .fillMaxWidth()
                                    ) {
                                        Icon(
                                            tint = Color.Yellow,
                                            painter = painterResource(id = R.drawable.star),
                                            contentDescription = "Review app",
                                            modifier = Modifier
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            color = MaterialTheme.colorScheme.surfaceTint,
                                            text = "Review app",
                                            fontSize = 14.sp,
                                            modifier = Modifier
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp
                                )
                        ) {
                            IconButton(onClick = {
                                scope!!.launch {
                                    if(drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    tint = Color.Gray,
                                    painter = painterResource(id = R.drawable.menu),
                                    contentDescription = "Menu"
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = navigateToAccountInfoScreen) {
                                Icon(
                                    painter = painterResource(id = R.drawable.account_info),
                                    contentDescription = "Account info"
                                )
                            }
                        }
                        when(currentTab) {
                            HomeScreenTab.HOME -> {
                                DashboardScreenComposable(
                                    navigateToTransactionsScreen = navigateToTransactionsScreen,
                                    navigateToCategoriesScreen = navigateToCategoriesScreen,
                                    navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
                                    navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
                                    navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                                    navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }
                            HomeScreenTab.ALL_TRANSACTIONS -> {
                                TransactionsScreenComposable(
                                    navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                                    navigateToPreviousScreen = navigateToPreviousScreen,
                                    navigateToHomeScreen = navigateToHomeScreen,
                                    navigateToSubscriptionScreen = {},
                                    navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                                    showBackArrow = false,
                                    navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
                                )
                            }
                            HomeScreenTab.TRANSACTION_TYPES -> {
                                TransactionTypesScreenComposable(
                                    navigateToTransactionsScreen = navigateToTransactionsScreenWithTransactionType
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
                                    showBackArrow = false,
                                    modifier = Modifier
                                )
                            }
                            HomeScreenTab.ACCOUNT_INFO -> {
                                AccountInformationScreenComposable(
                                    navigateToHomeScreen = navigateToHomeScreen,
                                    navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
                                )
                            }
                        }
                    }
                }
            }
            else -> {
                ModalNavigationDrawer(
                    drawerState = drawerState!!,
                    drawerContent = {
                        ModalDrawerSheet {
                            Column(
                                modifier = Modifier
                                    .padding(10.dp)
                            ) {
                                Spacer(modifier = Modifier.height(10.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .padding(
                                            horizontal = 16.dp
                                        )
                                ) {
                                    Icon(
                                        painter = painterResource(id = R.drawable.account_info),
                                        contentDescription = null,
                                        modifier = Modifier
                                            .size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    (if(firstName.isNullOrEmpty() && lastName.isNullOrEmpty()) phoneNumber else if(firstName.isNotNull() && lastName.isNotNull()) "$firstName $lastName" else if (firstName.isNullOrEmpty() && lastName.isNotNull()) lastName else if (lastName.isNotNull() && firstName.isNullOrEmpty()) lastName else phoneNumber)?.let {
                                        Text(
                                            text = it,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                    Spacer(modifier = Modifier.weight(1f))
                                    ThemeSwitcher(
                                        darkTheme = darkTheme,
                                        size = 30.dp,
                                        padding = 5.dp,
                                        onClick = onSwitchTheme,
                                        modifier = Modifier
                                            .padding(
                                                end = 8.dp
                                            )
                                    )
                                }
                                Spacer(modifier = Modifier.height(15.dp))
                                Divider()
                                Spacer(modifier = Modifier.height(15.dp))
                                Column(
                                    modifier = Modifier
                                        .verticalScroll(rememberScrollState())
                                ) {
                                    for(tab in tabs) {
                                        NavigationDrawerItem(
                                            label = {
                                                Row {
                                                    Icon(
                                                        painter = painterResource(id = tab.icon),
                                                        contentDescription = tab.name
                                                    )
                                                    Spacer(modifier = Modifier.width(5.dp))
                                                    Text(text = tab.name)
                                                }
                                            },
                                            selected = currentTab == tab.tab,
                                            onClick = {
                                                onTabChange(tab.tab)
                                                scope!!.launch {
                                                    drawerState.close()
                                                }
                                            }
                                        )
                                    }
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Row(
                                        modifier = Modifier
                                            .clickable {
                                                onReviewApp()
                                            }
                                            .padding(
                                                horizontal = 16.dp
                                            )
                                            .fillMaxWidth()
                                    ) {
                                        Icon(
                                            tint = Color.Yellow,
                                            painter = painterResource(id = R.drawable.star),
                                            contentDescription = "Review app",
                                            modifier = Modifier
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        )
                                        Spacer(modifier = Modifier.width(5.dp))
                                        Text(
                                            color = MaterialTheme.colorScheme.surfaceTint,
                                            text = "Review app",
                                            modifier = Modifier
                                                .padding(
                                                    vertical = 8.dp
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(
                                    horizontal = 16.dp
                                )
                        ) {
                            IconButton(onClick = {
                                scope!!.launch {
                                    if(drawerState.isClosed) drawerState.open() else drawerState.close()
                                }
                            }) {
                                Icon(
                                    tint = Color.Gray,
                                    painter = painterResource(id = R.drawable.menu),
                                    contentDescription = "Menu"
                                )
                            }
                            Spacer(modifier = Modifier.weight(1f))
                            IconButton(onClick = navigateToAccountInfoScreen) {
                                Icon(
                                    painter = painterResource(id = R.drawable.account_info),
                                    contentDescription = "Account info"
                                )
                            }
                        }
                        when(currentTab) {
                            HomeScreenTab.HOME -> {
                                DashboardScreenComposable(
                                    navigateToTransactionsScreen = navigateToTransactionsScreen,
                                    navigateToCategoriesScreen = navigateToCategoriesScreen,
                                    navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
                                    navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
                                    navigateToSubscriptionScreen = navigateToSubscriptionScreen,
                                    navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                                    modifier = Modifier
                                        .weight(1f)
                                )
                            }
                            HomeScreenTab.ALL_TRANSACTIONS -> {
                                TransactionsScreenComposable(
                                    navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen,
                                    navigateToPreviousScreen = navigateToPreviousScreen,
                                    navigateToHomeScreen = navigateToHomeScreen,
                                    navigateToSubscriptionScreen = {},
                                    navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
                                    showBackArrow = false,
                                    navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
                                )
                            }
                            HomeScreenTab.TRANSACTION_TYPES -> {
                                TransactionTypesScreenComposable(
                                    navigateToTransactionsScreen = navigateToTransactionsScreenWithTransactionType
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
                                    showBackArrow = false,
                                    modifier = Modifier
                                )
                            }
                            HomeScreenTab.ACCOUNT_INFO -> {
                                AccountInformationScreenComposable(
                                    navigateToHomeScreen = navigateToHomeScreen,
                                    navigateToLoginScreenWithArgs = navigateToLoginScreenWithArgs
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
private fun BottomNavBar(
    tabItems: List<HomeScreenTabItem>,
    currentTab: HomeScreenTab,
    onTabSelected: (HomeScreenTab) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar {
        for(item in tabItems) {
            NavigationBarItem(
                label = {
                    Text(text = item.name)
                },
                selected = item.tab == currentTab,
                onClick = { onTabSelected(item.tab) },
                icon = {
                    Icon(
                        painter = painterResource(id = item.icon),
                        contentDescription = item.name
                    )
                }
            )
        }
    }
}

@Composable
fun ThemeSwitcher(
    darkTheme: Boolean = false,
    size: Dp = 150.dp,
    iconSize: Dp = size / 3,
    padding: Dp = 10.dp,
    borderWidth: Dp = 1.dp,
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
fun SubscriptionDialog(
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(text = "Go premium?")
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
                        fontWeight = FontWeight.Bold,
                        textDecoration = TextDecoration.Underline
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Premium version allows you to: ",
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(text = "1. See transactions and export reports of more than one months")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "2. Manage more than one category")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "3. Manage more than one Budget")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "4. Use in dark mode")

                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = "Dismiss")
            }
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(text = "Subscribe")
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
            scope = null,
            drawerState = null,
            firstName = null,
            lastName = null,
            phoneNumber = "",
            currentTab = currentTab,
            onTabChange = {
                currentTab = it
            },
            tabs = tabs,
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
            navigateToEntityTransactionsScreen = {userId, transactionType, entity, startDate, endDate, times, moneyIn ->  },
            darkTheme = false,
            onSwitchTheme = {},
            onReviewApp = {},
            navigateToSubscriptionScreen = {},
            navigateToAccountInfoScreen = {},
            navigateToTransactionDetailsScreen = {},
            navigateToTransactionsScreenWithTransactionType = {transactionType, moneyDirection, startDate, endDate ->  }
        )
    }
}