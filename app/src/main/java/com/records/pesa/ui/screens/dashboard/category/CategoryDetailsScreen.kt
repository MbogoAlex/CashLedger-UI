package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.models.CategoryKeyword
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime
object CategoryDetailsScreenDestination: AppNavigation {
    override val title: String = "Category details screen"
    override val route: String = "category-details-screen"
    val categoryId: String = "categoryId"
    val routeWithArgs: String = "$route/{$categoryId}"

}
@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryDetailsScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    navigateToTransactionsScreen: (categoryId: String) -> Unit,
    navigateToCategoryBudgetListScreen: (categoryId: String, categoryName: String) -> Unit,
    navigateToBudgetCreationScreen: (categoryId: String) -> Unit,
    navigateToHomeScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: CategoryDetailsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val lifecycleOwner = LocalLifecycleOwner.current
    val lifecycleState by lifecycleOwner.lifecycle.currentStateFlow.collectAsState()

    LaunchedEffect(lifecycleState) {
        Log.i("CURRENT_LIFECYCLE", lifecycleState.name)
        if(lifecycleState.name.lowercase() == "started") {
            viewModel.getCategory()
        }
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getCategory()
        }
    )

    var showEditCategoryNameDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showEditMemberNameDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var categoryName by rememberSaveable {
        mutableStateOf("")
    }

    var memberName by rememberSaveable {
        mutableStateOf("")
    }

    var categoryId by rememberSaveable {
        mutableIntStateOf(0)
    }

    var keywordId by rememberSaveable {
        mutableIntStateOf(0)
    }

    var showRemoveMemberDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showRemoveCategoryDialog by rememberSaveable {
        mutableStateOf(false)
    }


    if(showEditCategoryNameDialog) {
        EditNameDialog(
            title = "Category name",
            label = "Category",
            name = categoryName,
            onNameChange = {
                categoryName = it
                viewModel.editCategoryName(it)
            },
            onConfirm = {
                viewModel.updateCategoryName()
                showEditCategoryNameDialog = !showEditCategoryNameDialog
            },
            onDismiss = {showEditCategoryNameDialog = !showEditCategoryNameDialog}
        )
    }

    if(showEditMemberNameDialog) {
        EditNameDialog(
            title = "Member name",
            label = "Member",
            name = memberName,
            onNameChange = {
                memberName = it
                viewModel.editMemberName(it)
            },
            onConfirm = {
                viewModel.updateMemberName()
                showEditMemberNameDialog = !showEditMemberNameDialog
            },
            onDismiss = {showEditMemberNameDialog = !showEditMemberNameDialog}
        )
    }

    if(showRemoveMemberDialog) {
        DeleteDialog(
            name = memberName,
            categoryDeletion = false,
            onConfirm = {
                viewModel.removeCategoryMember(categoryId, keywordId)
                showRemoveMemberDialog = !showRemoveMemberDialog
                if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
                    navigateToHomeScreen()
                }
            },
            onDismiss = {
                showRemoveMemberDialog = !showRemoveMemberDialog
            }
        )
    }

    if(showRemoveCategoryDialog) {
        DeleteDialog(
            name = categoryName,
            categoryDeletion = true,
            onConfirm = {
                viewModel.removeCategory(categoryId)
                showRemoveCategoryDialog = !showRemoveCategoryDialog
            },
            onDismiss = {
                showRemoveCategoryDialog = !showRemoveCategoryDialog
            }
        )
    }

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Category updated", Toast.LENGTH_SHORT).show()
        viewModel.getCategory()
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed. Check connection or try later", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    if(uiState.deletionStatus == DeletionStatus.SUCCESS) {
        Toast.makeText(context, "Category deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetLoadingStatus()
    } else if(uiState.deletionStatus == DeletionStatus.FAIL) {
        Toast.makeText(context, "Failed to delete category. Check your connection or try later", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        CategoryDetailsScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            category = uiState.category,
            onEditCategoryName = {
                categoryName = it
                showEditCategoryNameDialog = !showEditCategoryNameDialog
            },
            onEditMemberName = {
                memberName = it.nickName ?: ""
                viewModel.updateCategoryKeyword(it)
                showEditMemberNameDialog = !showEditMemberNameDialog
            },
            onRemoveMember = {memName, catId, keyId ->
                memberName = memName
                categoryId = catId
                keywordId = keyId
                showRemoveMemberDialog = !showRemoveMemberDialog
            },
            onRemoveCategory = { catId, cateName ->
                categoryId = catId
                categoryName = cateName
                showRemoveCategoryDialog = !showRemoveCategoryDialog
            },
            navigateToCategoryBudgetListScreen = navigateToCategoryBudgetListScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToMembersAdditionScreen = navigateToMembersAdditionScreen,
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToBudgetCreationScreen = navigateToBudgetCreationScreen
        )
    }
}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoryDetailsScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    category: TransactionCategory,
    navigateToCategoryBudgetListScreen: (categoryId: String, categoryName: String) -> Unit,
    onEditCategoryName: (name: String) -> Unit,
    onEditMemberName: (categoryKeyword: CategoryKeyword) -> Unit,
    onRemoveMember: (memberName: String, categoryId: Int, keywordId: Int) -> Unit,
    onRemoveCategory: (categoryId: Int, categoryName: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToMembersAdditionScreen: (categoryId: String) -> Unit,
    navigateToTransactionsScreen: (categoryId: String) -> Unit,
    navigateToBudgetCreationScreen: (categoryId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(screenWidth(x = 16.0))
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = navigateToPreviousScreen
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack, 
                    contentDescription = "Previous screen",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            IconButton(
                colors = IconButtonDefaults.iconButtonColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                ),
                onClick = {onRemoveCategory(category.id, category.name)}
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.error,
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete category",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = category.name,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 18.0).sp
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onEditCategoryName(category.name)
            }) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit category name",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Text(
            text = "Created on ${formatIsoDateTime(LocalDateTime.parse(category.createdAt))}",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budgets (${if (category.budgets.isNotEmpty()) category.budgets.size else 0})",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 18.0).sp
            )

            Spacer(modifier = Modifier.weight(1f))
            if(category.budgets.isNotEmpty()) {
                TextButton(
                    enabled = category.budgets.isNotEmpty(),
                    onClick = {
                        navigateToCategoryBudgetListScreen(category.id.toString(), category.name)
                    }
                ) {
                    Text(
                        text = "Explore",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            } else {
                TextButton(
                    enabled = category.keywords.isNotEmpty(),
                    onClick = {
                        navigateToBudgetCreationScreen(category.id.toString())
                    }
                ) {
                    Text(
                        text = "Create",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }

        }
        Divider()
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Members",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 18.0).sp
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = { navigateToMembersAdditionScreen(category.id.toString()) }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Add",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add members")
                }
            }
        }
        LazyColumn(
            modifier = Modifier
                .weight(1f)
        ) {
            Log.d("CATEGORY_KEYWORDS", category.keywords.toString())
            items(category.keywords) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if(it.nickName != null) if(it.nickName.length > 20) "${it.nickName.substring(0, 20)}..." else it.nickName else if(it.keyWord.length > 20) "${it.keyWord.substring(0, 20)}..." else it.keyWord,
//                        fontWeight = FontWeight.Bold,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    IconButton(onClick = {
                        onEditMemberName(it)
                    }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit member",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                    IconButton(onClick = {
                        onRemoveMember(it.nickName ?: it.keyWord, category.id, it.id)
                    }) {
                        Icon(
                            tint = MaterialTheme.colorScheme.error,
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Remove member",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
            }
        }
        if(loadingStatus == LoadingStatus.LOADING) {
            Box(
                contentAlignment = Alignment.TopCenter,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxSize()
            ) {
                PullRefreshIndicator(
                    refreshing = loadingStatus == LoadingStatus.LOADING,
                    state = pullRefreshState!!
                )
            }
        }
        OutlinedButton(
            onClick = {
                navigateToTransactionsScreen(category.id.toString())
            },
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Transactions",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Icon(imageVector = Icons.Default.KeyboardArrowRight, contentDescription = "See transactions")
            }
        }

    }

}

@Composable
fun EditNameDialog(
    label: String,
    title: String,
    name: String,
    onNameChange: (name: String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Edit $title",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            OutlinedTextField(
                value = name,
                label = {
                    Text(
                        text = label,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                },
                onValueChange = onNameChange,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Text
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
fun DeleteDialog(
    name: String,
    categoryDeletion: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = if(categoryDeletion) "Remove category" else "Remove member",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Text(
                text = "Remove $name? This action cannot be undone",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        onDismissRequest = onDismiss,
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        modifier = Modifier
    )
}

@OptIn(ExperimentalMaterialApi::class)
@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryDetailsScreenPreview() {
    CashLedgerTheme {
        CategoryDetailsScreen(
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            category = transactionCategory,
            onEditCategoryName = {},
            onEditMemberName = {},
            onRemoveMember = {memName, categoryId, keywordId ->  },
            onRemoveCategory = {categoryId, categoryName ->  },
            navigateToPreviousScreen = {},
            navigateToCategoryBudgetListScreen = {categoryId, categoryName ->  },
            navigateToMembersAdditionScreen = {},
            navigateToTransactionsScreen = {},
            navigateToBudgetCreationScreen = {}
        )
    }
}