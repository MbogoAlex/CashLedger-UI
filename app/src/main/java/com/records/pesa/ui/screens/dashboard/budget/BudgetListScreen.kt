package com.records.pesa.ui.screens.dashboard.budget

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.BudgetDt
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.budgets
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime
import kotlin.math.absoluteValue

object BudgetListScreenDestination: AppNavigation {
    override val title: String = "Budget list screen"
    override val route: String = "budget-list-screen"
    val categoryId: String = "categoryId"
    val categoryName: String = "categoryName"
    val routeWithArgs = "$route/{$categoryId}/{$categoryName}"

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
        if(showBackArrow) {
            navigateToPreviousScreen()
        } else {
            navigateToHomeScreen()
        }
    })

    val viewModel: BudgetListScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getBudgets()
        }
    )

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
        if(lifecycleState.name.lowercase() == "started") {
            viewModel.getBudgets()
        }
    }

    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        BudgetListScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            budgets = uiState.budgetList,
            searchQuery = uiState.searchQuery,
            categoryId = uiState.categoryId,
            categoryName = uiState.categoryName,
            onChangeSearchQuery = {
                viewModel.updateSearchQuery(it)
            },
            onClearSearch = {
                viewModel.clearSearch()
            },
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
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    budgets: List<BudgetDt>,
    searchQuery: String,
    categoryId: String?,
    categoryName: String?,
    onChangeSearchQuery: (value: String) -> Unit,
    onClearSearch: () -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    navigateToBudgetCreationScreen: () -> Unit,
    navigateToBudgetCreationScreenWithCategoryId: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    showBackArrow: Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            if(showBackArrow) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
                }
                Spacer(modifier = Modifier.weight(1f))
            }
            TextField(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                value = searchQuery,
                placeholder = {
                    Text(text = "Budget / Category")
                },
                trailingIcon = {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.inverseOnSurface)
                            .padding(5.dp)
                            .clickable {
                                onClearSearch()
                            }

                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            modifier = Modifier
                                .size(16.dp)
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
                modifier = Modifier
                    .fillMaxWidth()
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if(categoryName != null) {
            Text(
                text = "$categoryName budgets",
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "User budgets",
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        if(loadingStatus == LoadingStatus.SUCCESS) {
            if(budgets.isEmpty()) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .fillMaxSize()
                ) {
                    Text(text = "Create budgets for your transactions. A budget must belong to a category. Create a category first then create a budget for it")
                }
            } else {
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(budgets) {
                        BudgetListItem(
                            budgetDt = it,
                            navigateToBudgetInfoScreen = navigateToBudgetInfoScreen
                        )
                    }
                }
            }
        }

        if(loadingStatus == LoadingStatus.LOADING) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .fillMaxSize()
            ) {
                PullRefreshIndicator(
                    refreshing = loadingStatus == LoadingStatus.LOADING,
                    state = pullRefreshState!!
                )
            }
            Spacer(modifier = Modifier.weight(1f))
        }
//        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                if(categoryId == null) navigateToBudgetCreationScreen() else navigateToBudgetCreationScreenWithCategoryId(categoryId)
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "Add a budget")
        }
    }
}

@Composable
fun BudgetListItem(
    budgetDt: BudgetDt,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val difference = budgetDt.expenditure - budgetDt.budgetLimit
    var expanded by remember {
        mutableStateOf(false)
    }
    val progress = budgetDt.expenditure / budgetDt.budgetLimit
    val percentLeft = "%.2f".format((1 - progress) * 100)
    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(
                bottom = 10.dp
            )
    ) {
        Row(
            modifier = Modifier
                .padding(10.dp)
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize(
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioNoBouncy,
                            stiffness = Spring.StiffnessMedium
                        )
                    )
            ) {
                Text(
                    text = budgetDt.name ?: "N/A",
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                if(budgetDt.active) {
                    Text(
                        text = "ACTIVE",
                        fontWeight = FontWeight.Bold
                    )
                } else {
                    Text(
                        text = "INACTIVE",
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Spent: ${formatMoneyValue(budgetDt.expenditure)} / ${formatMoneyValue(budgetDt.budgetLimit)}",
                    color = MaterialTheme.colorScheme.surfaceTint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                LinearProgressIndicator(
                    progress = {
                        if(progress.isNaN()) 0f else progress.toFloat()
                    },
                    modifier = Modifier
                        .height(20.dp)
                        .fillMaxWidth(),
                )
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "$percentLeft % left",
                    color = MaterialTheme.colorScheme.surfaceTint,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(5.dp))
                Row(
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Difference:",
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    if(difference <= 0) {
                        Text(
                            text = formatMoneyValue(difference.absoluteValue),
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.surfaceTint
                        )
                    } else {
                        Text(
                            text = "- ${formatMoneyValue(difference)}",
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(
                    text = "Created on ${formatIsoDateTime(LocalDateTime.parse(budgetDt.createdAt))}",
                    fontWeight = FontWeight.Bold
                )
                if(budgetDt.limitReached) {
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "Limit Reached")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "Reached limit on ${budgetDt.limitReachedAt}")
                    Spacer(modifier = Modifier.height(5.dp))
                    Text(text = "Overspent by ${formatMoneyValue(difference)}")
                }
                Spacer(modifier = Modifier.height(5.dp))
                Text(text = "Category: ${budgetDt.category.name}")

                Spacer(modifier = Modifier.height(5.dp))
                Button(
                    onClick = {
                        navigateToBudgetInfoScreen(budgetDt.id.toString())
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    Text(text = "Explore")
                }

                
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetListScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        BudgetListScreen(
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            budgets = budgets,
            searchQuery = "",
            categoryId = null,
            categoryName = null,
            onChangeSearchQuery = {},
            onClearSearch = { /*TODO*/ },
            navigateToBudgetInfoScreen = {},
            navigateToBudgetCreationScreen = {},
            navigateToBudgetCreationScreenWithCategoryId = {},
            navigateToPreviousScreen = {},
            showBackArrow = true
        )
    }
}