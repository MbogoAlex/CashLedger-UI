package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.content.Context
import android.os.Build
import android.widget.Space
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material3.Button
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactionCategory
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.ZoneId

object BudgetCreationScreenDestination: AppNavigation {
    override val title: String = "Budget creation screen"
    override val route: String = "budget-creation-screen"
    val categoryId: String = "category-id"
    val routeWithArgs = "$route/{$categoryId}"

}
@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreenComposable(
    navigateToPreviousScreen: () -> Unit,
    navigateToBudgetInfoScreen: (budgetId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: BudgetCreationScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Budget created", Toast.LENGTH_SHORT).show()
        navigateToBudgetInfoScreen(uiState.budgetId)
        viewModel.resetLoadingStatus()
    } else if(uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed. Check your connection or try later", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        BudgetCreationScreen(
            categories = uiState.categories,
            selectedCategory = uiState.selectedCategory,
            onSelectCategory = {
                viewModel.updateCategory(it)
                viewModel.checkIfFieldsAreFilled()
            },
            budgetName = uiState.budgetName,
            onBudgetNameChange = {
                viewModel.updateBudgetName(it)
                viewModel.checkIfFieldsAreFilled()
            },
            budgetLimit = uiState.budgetLimit,
            onBudgetLimitChange = { value ->
                val filteredValue = value.filter { it.isDigit() }
                viewModel.updateBudgetLimit(filteredValue)
                viewModel.checkIfFieldsAreFilled()
            },
            budgetEndDate = uiState.limitDate,
            onBudgetEndDateChange = {
                viewModel.updateLimitDate(it)
                viewModel.checkIfFieldsAreFilled()
            },
            saveButtonEnabled = uiState.saveButtonEnabled,
            loadingStatus = uiState.loadingStatus,
            onCreateBudget = {
                viewModel.createBudget()
            },
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreen(
    categories: List<TransactionCategory>,
    selectedCategory: TransactionCategory,
    onSelectCategory: (category: TransactionCategory) -> Unit,
    budgetName: String,
    onBudgetNameChange: (name: String) -> Unit,
    budgetLimit: String,
    onBudgetLimitChange: (limit: String) -> Unit,
    budgetEndDate: LocalDate?,
    onBudgetEndDateChange: (date: LocalDate) -> Unit,
    saveButtonEnabled: Boolean,
    loadingStatus: LoadingStatus,
    onCreateBudget: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val startDate = LocalDate.now()

    var dropDownMenuExpanded by rememberSaveable {
        mutableStateOf(false)
    }
    val defaultStartMillis = startDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker() {
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                onBudgetEndDateChange(selectedDate)
            },

            startDate.year,
            startDate.monthValue - 1,
            startDate.dayOfMonth
        )
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }

        datePicker.show()
    }

    Column(
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 8.0)
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
            Text(
                text = "Set budget",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 18.0).sp
            )
        }
        Text(
            text = "Select category",
            fontSize = screenFontSize(x = 14.0).sp,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Card(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .clickable {
                            dropDownMenuExpanded = !dropDownMenuExpanded
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .padding(screenWidth(x = 10.0))
                    ) {
                        Text(
                            text = if(selectedCategory.name.length > 20) "${selectedCategory.name.substring(0, 20)}..." else selectedCategory.name.ifEmpty { "Select category" },
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Spacer(modifier = Modifier.weight(1f))
                        Icon(
                            imageVector = if(dropDownMenuExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                            contentDescription =
                            "Select category",
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }
                }
                DropdownMenu(expanded = dropDownMenuExpanded, onDismissRequest = {
                    dropDownMenuExpanded = !dropDownMenuExpanded
                }
                ) {
                    for(category in categories) {
                        DropdownMenuItem(onClick = {
                            onSelectCategory(category)
                            dropDownMenuExpanded = !dropDownMenuExpanded
                        }
                        ) {
                            Text(
                                text = if(category.name.length > 20) "${category.name.substring(0, 20)}..." else category.name,
                                fontSize = screenFontSize(x = 14.0).sp
                            )
                        }
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        OutlinedTextField(
            label = {
                Text(
                    text = "Budget name",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            },
            value = budgetName,
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            onValueChange = onBudgetNameChange,
            modifier = Modifier
                .fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Text(
            text = "Budget starts on ${formatLocalDate(LocalDate.now())} (Today)",
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                label = {
                    Text(
                        text = "Budget limit",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                },
                value = budgetLimit,
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal
                ),
                onValueChange = onBudgetLimitChange,
                modifier = Modifier
                    .weight(2f)
            )
            Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
            TextButton(
                onClick = { showDatePicker() },
                modifier = Modifier
                    .weight(1f)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = budgetEndDate?.toString() ?: "Limit date",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    Spacer(modifier = Modifier.width(screenWidth(x = 5.0)))
                    Icon(
                        painter = painterResource(id = R.drawable.calendar),
                        contentDescription = "Select limit date",
                        modifier = Modifier
                            .size(screenWidth(x = 24.0))
                    )
                }
            }
        }
        Spacer(modifier = Modifier.weight(1f))
        Button(
            enabled = saveButtonEnabled && loadingStatus != LoadingStatus.LOADING,
            onClick = onCreateBudget,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            if(loadingStatus == LoadingStatus.LOADING) {
                Text(
                    text = "Loading...",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            } else {
                Text(
                    text = "Create budget",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun BudgetCreationScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        BudgetCreationScreen(
            categories = emptyList(),
            selectedCategory = transactionCategory,
            onSelectCategory = {},
            budgetName = "",
            onBudgetNameChange = {},
            budgetLimit = "",
            onBudgetLimitChange = {},
            budgetEndDate = null,
            onBudgetEndDateChange = {},
            saveButtonEnabled = false,
            loadingStatus = LoadingStatus.INITIAL,
            navigateToPreviousScreen = {},
            onCreateBudget = {}
        )
    }

}