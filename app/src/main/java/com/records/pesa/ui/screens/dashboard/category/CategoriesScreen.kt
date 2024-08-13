package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionCategoryCell
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.ui.theme.CashLedgerTheme
object CategoriesScreenDestination: AppNavigation {
    override val title: String = "Categories screen"
    override val route: String = "categories-screen"

}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreenComposable(
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToHomeScreen: () -> Unit,
    showBackArrow: Boolean,
    navigateToSubscriptionScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = {
        if(showBackArrow) {
            navigateToPreviousScreen()
        } else {
            navigateToHomeScreen()
        }
    })

    val viewModel: CategoriesScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getUserCategories()
        }
    )

    var showSubscriptionDialog by rememberSaveable {
        mutableStateOf(false)
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

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
        if(lifecycleState.name.lowercase() == "started") {
            viewModel.getUserCategories()
        }
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        CategoriesScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            premium = uiState.userDetails.paymentStatus,
            searchQuery = uiState.name,
            onChangeSearchQuery = {
                viewModel.updateName(it)
            },
            onClearSearch = {
                viewModel.getUserCategories()
            },
            categories = uiState.categories,
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            onShowSubscriptionDialog = {
                showSubscriptionDialog = true
            }
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    premium: Boolean,
    searchQuery: String,
    categories: List<TransactionCategory>,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    showBackArrow: Boolean = false,
    onShowSubscriptionDialog: () -> Unit,
    onClearSearch: () -> Unit,
    onChangeSearchQuery: (value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {

        if(premium) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if(showBackArrow) {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "Previous screen")
                    }
                }
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    painter = painterResource(id = R.drawable.filter),
                    contentDescription = "Filter categories",
                    modifier = Modifier
//                    .padding(20.dp)
                        .size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            TextField(
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                value = searchQuery,
                placeholder = {
                    Text(text = "Category")
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
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Categories",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {
                if(categories.isNotEmpty() && !premium) {
                    onShowSubscriptionDialog()
                } else {
                    navigateToCategoryAdditionScreen()
                }
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = "Add")
                    Spacer(modifier = Modifier.width(3.dp))
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add category")
                }

            }
        }

        Spacer(modifier = Modifier.height(20.dp))
        if(loadingStatus == LoadingStatus.SUCCESS) {
            LazyColumn {
                items(categories.size) {index ->
                    TransactionCategoryCell(
                        transactionCategory = categories[index],
                        navigateToCategoryDetailsScreen = {
                            if(index != 0 && !premium) {
                                onShowSubscriptionDialog()
                            } else {
                                navigateToCategoryDetailsScreen(categories[index].id.toString())
                            }
                        }
                    )
                }
            }
        }
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
                    Text(text = "1. See transactions and export reports of more than three months")
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

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryScreenPreview() {
    CashLedgerTheme {
        CategoriesScreen(
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            categories = transactionCategories,
            searchQuery = "",
            onClearSearch = {},
            onChangeSearchQuery = {},
            navigateToCategoryDetailsScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToPreviousScreen = {},
            premium = false,
            onShowSubscriptionDialog = {}
        )
    }
}