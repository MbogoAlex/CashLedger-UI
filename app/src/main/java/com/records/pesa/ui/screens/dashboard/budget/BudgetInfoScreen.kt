package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.os.Build
import android.util.Log
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatIsoDateTime
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.BudgetDt
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.ExecutionStatus
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.budget
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import kotlin.math.absoluteValue

object BudgetInfoScreenDestination: AppNavigation {
    override val title: String = "Budget info screen"
    override val route: String = "budget-info-screen"
    val budgetId: String = "budgetId"
    val routeWithArgs: String = "$route/{$budgetId}"

}
@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BudgetInfoScreenComposable(
    navigateToTransactionsScreen: (categoryId: Int, budgetId: Int, startDate: String, endDate: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: BudgetInfoScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {}
    )

    var showEditBudgetNameDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showEditBudgetLimitDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var showEditLimitDateDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var budgetName by rememberSaveable {
        mutableStateOf("")
    }

    var budgetLimit by rememberSaveable {
        mutableStateOf("")
    }

    var endDate by rememberSaveable {
        mutableStateOf(uiState.budget.limitDate)
    }

    var showRemoveBudgetDialog by rememberSaveable {
        mutableStateOf(false)
    }

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Budget updated", Toast.LENGTH_SHORT).show()
        showEditBudgetNameDialog = false
        showEditBudgetLimitDialog = false
        showEditLimitDateDialog = false
        showRemoveBudgetDialog = false
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed. Check connection or try later", Toast.LENGTH_SHORT).show()
        showEditBudgetNameDialog = false
        showEditBudgetLimitDialog = false
        showEditLimitDateDialog = false
        showRemoveBudgetDialog = false
        viewModel.resetLoadingStatus()
    }

    if(uiState.executionStatus == ExecutionStatus.SUCCESS) {
        Toast.makeText(context, "Budget deleted", Toast.LENGTH_SHORT).show()
        navigateToPreviousScreen()
        viewModel.resetLoadingStatus()
    }

    if(showEditBudgetNameDialog) {
        EditNameDialog(
            title = uiState.budget.name!!,
            label = "Budget name",
            name = budgetName,
            onNameChange = {
                budgetName = it
                viewModel.updateBudgetName(it)
            },
            onConfirm = {},
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    viewModel.updateBudgetName("")
                    showEditBudgetNameDialog = !showEditBudgetNameDialog
                }
            },
            loadingStatus = uiState.loadingStatus
        )
    }

    if(showEditBudgetLimitDialog) {
        EditAmountDialog(
            title = uiState.budget.name!!,
            label = "Budget limit",
            amount = budgetLimit,
            onAmountChange = {value ->
                val filteredAmount = value.filter { it.isDigit() }
                budgetLimit = filteredAmount
                viewModel.updateBudgetLimit(budgetLimit)
            },
            onConfirm = {},
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    viewModel.updateBudgetLimit("")
                    showEditBudgetLimitDialog = !showEditBudgetLimitDialog
                }

            },
            loadingStatus = uiState.loadingStatus
        )
    }

    if(showEditLimitDateDialog) {
        EditLimitDateDialog(
            startDate = uiState.budget.createdAt.substring(0, 10),
            endDate = endDate,
            title = uiState.budget.name!!,
            onLimitDateChange = {
                endDate = it.toString()
                viewModel.updateLimitDate(endDate)
            },
            onConfirm = {},
            onDismiss = {
                if(uiState.loadingStatus != LoadingStatus.LOADING) {
                    viewModel.updateLimitDate("")
                    showEditLimitDateDialog = !showEditLimitDateDialog
                }

            },
            loadingStatus = uiState.loadingStatus
        )
    }

    if(showRemoveBudgetDialog) {
        DeleteDialog(
            name = uiState.budget.name!!,
            onConfirm = {},
            onDismiss = {
                if(uiState.executionStatus != ExecutionStatus.LOADING) {
                    showRemoveBudgetDialog = !showRemoveBudgetDialog
                }
            },
            executionStatus = uiState.executionStatus
        )
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BudgetInfoScreen(
            budgetDt = uiState.budget,
            onDeleteBudget = {
                showRemoveBudgetDialog = !showRemoveBudgetDialog
            },
            onEditBudgetName = {
                budgetName = it
                showEditBudgetNameDialog = !showEditBudgetNameDialog
            },
            onEditBudgetLimit = {
                budgetLimit = it
                showEditBudgetLimitDialog = !showEditBudgetLimitDialog
            },
            onEditLimitDate = {
                endDate = it
                showEditLimitDateDialog = !showEditLimitDateDialog
            },
            navigateToTransactionsScreen = navigateToTransactionsScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus
        )
    }

}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun BudgetInfoScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    budgetDt: BudgetDt,
    onDeleteBudget: () -> Unit,
    onEditBudgetName: (name: String) -> Unit,
    onEditBudgetLimit: (amount: String) -> Unit,
    onEditLimitDate: (date: String) -> Unit,
    navigateToTransactionsScreen: (categoryId: Int, budgetId: Int, startDate: String, endDate: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Log.d("BUDGET", budgetDt.toString())
    val difference = budgetDt.expenditure - budgetDt.budgetLimit
    var expanded by remember {
        mutableStateOf(false)
    }
    val progress = budgetDt.expenditure / budgetDt.budgetLimit
    val percentLeft = "%.2f".format((1 - progress) * 100)

    val days = ChronoUnit.DAYS.between(LocalDate.parse(budgetDt.limitDate), LocalDateTime.parse(budgetDt.createdAt))

    Column(
        modifier = Modifier
            .padding(
                horizontal = 16.dp,
                vertical = 8.dp
            )
            .fillMaxSize()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
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
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                ),
                onClick = onDeleteBudget
            ) {
                Icon(
                    tint = MaterialTheme.colorScheme.error,
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete this budget",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
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
        }

        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = budgetDt.name!!,
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onEditBudgetName(budgetDt.name)
            }) {
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit budget name",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }
        if(budgetDt.active) {
            Text(
                text = "ACTIVE",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
        } else {
            Text(
                text = "INACTIVE",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Text(
            text = "Spent: ${formatMoneyValue(budgetDt.expenditure)} / ${formatMoneyValue(budgetDt.budgetLimit)}",
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.surfaceTint,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
        LinearProgressIndicator(
            progress = {
                if(progress.isNaN()) 0f else progress.toFloat()
            },
            modifier = Modifier
                .height(screenHeight(x = 20.0))
                .fillMaxWidth(),
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "$percentLeft % left",
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.surfaceTint,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            TextButton(onClick = {
                onEditBudgetLimit(budgetDt.budgetLimit.toString())
            }) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Edit budget limit",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
                    Icon(
                        tint = MaterialTheme.colorScheme.surfaceTint,
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit budget",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Row(
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Remaining:",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.width(screenWidth(x = 3.0)))
            if(difference <= 0) {
                Text(
                    text = formatMoneyValue(difference.absoluteValue),
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.surfaceTint
                )
            } else {
                Text(
                    text = "- ${formatMoneyValue(difference)}",
                    fontWeight = FontWeight.Bold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = Color.Red
                )
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Text(
            text = "Created on ${formatIsoDateTime(LocalDateTime.parse(budgetDt.createdAt))}",
            fontSize = screenFontSize(x = 14.0).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Budget period ends on ${budgetDt.limitDate}",
                fontSize = screenFontSize(x = 14.0).sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            IconButton(onClick = {
                onEditLimitDate(budgetDt.limitDate)
            }) {
                Icon(
                    tint = MaterialTheme.colorScheme.surfaceTint,
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Edit budget limit date",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
        Text(
            text = "Period: ${days.absoluteValue} days",
            fontSize = screenFontSize(x = 14.0).sp,
            fontWeight = FontWeight.Bold
        )
        if(budgetDt.limitReached) {
            Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
            Text(
                text = "Limit Reached",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
            Text(
                text = "Reached limit at ${formatIsoDateTime(LocalDateTime.parse(budgetDt.limitReachedAt))}",
                fontSize = screenFontSize(x = 14.0).sp
            )
            Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
            Text(
                text = "Overspent by ${formatMoneyValue(difference)}",
                fontSize = screenFontSize(x = 14.0).sp
            )
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 5.0)))
        Text(
            text = "Category: ${budgetDt.category.name}",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.weight(1f))
        OutlinedButton(
            onClick = {
                navigateToTransactionsScreen(budgetDt.category.id, budgetDt.id, budgetDt.createdAt.substring(0, 10), budgetDt.limitDate)
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
                Icon(
                    imageVector = Icons.Default.KeyboardArrowRight,
                    contentDescription = "See transactions",
                    modifier = Modifier
                        .size(screenWidth(x = 24.0))
                )
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
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Edit budget name for $title",
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
            Button(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(
                        text = "Loading",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Confirm",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
        },
        dismissButton = {
            TextButton(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onDismiss
            ) {
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
fun EditAmountDialog(
    label: String,
    title: String,
    amount: String,
    onAmountChange: (name: String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Edit budget limit for $title",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            OutlinedTextField(
                value = amount,
                label = {
                    Text(
                        text = label,
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                },
                onValueChange = onAmountChange,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal
                ),
                modifier = Modifier
                    .fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(
                        text = "Loading",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Confirm",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onDismiss
            ) {
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
fun EditLimitDateDialog(
    startDate: String,
    endDate: String,
    title: String,
    onLimitDateChange: (date: LocalDate) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {

    val context = LocalContext.current
    val dStartDate = LocalDate.parse(startDate)

    val defaultStartMillis = dStartDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker() {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                onLimitDateChange(selectedDate)
            },

            dStartDate.year,
            dStartDate.monthValue - 1,
            dStartDate.dayOfMonth
        )
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }

        datePicker.show()
    }

    AlertDialog(
        title = {
            Text(
                text = "Edit budget limit date for $title",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Row(
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                TextButton(onClick = {showDatePicker()}) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = endDate)
                        Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
                        Icon(
                            painter = painterResource(id = R.drawable.calendar),
                            contentDescription = "Change limit date",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onConfirm
            ) {
                if(loadingStatus == LoadingStatus.LOADING) {
                    Text(
                        text = "Loading",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Confirm",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }
            }
        },
        dismissButton = {
            TextButton(
                enabled = loadingStatus != LoadingStatus.LOADING,
                onClick = onDismiss
            ) {
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
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    executionStatus: ExecutionStatus,
    modifier: Modifier = Modifier
) {
    AlertDialog(
        title = {
            Text(
                text = "Remove budget",
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
            Button(
                enabled = executionStatus != ExecutionStatus.LOADING,
                onClick = onConfirm
            ) {
                if(executionStatus == ExecutionStatus.LOADING) {
                    Text(
                        text = "Loading...",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                } else {
                    Text(
                        text = "Confirm",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                }

            }
        },
        dismissButton = {
            TextButton(
                enabled = executionStatus != ExecutionStatus.LOADING,
                onClick = onDismiss
            ) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetInfoScreenPreview() {
    CashLedgerTheme {
        BudgetInfoScreen(
            pullRefreshState = null,
            loadingStatus = LoadingStatus.INITIAL,
            budgetDt = budget,
            onDeleteBudget = {},
            onEditBudgetName = {},
            onEditBudgetLimit = {},
            onEditLimitDate = {},
            navigateToTransactionsScreen = {categoryId, budgetId, startDate, endDate ->},
            navigateToPreviousScreen = {}
        )
    }
}