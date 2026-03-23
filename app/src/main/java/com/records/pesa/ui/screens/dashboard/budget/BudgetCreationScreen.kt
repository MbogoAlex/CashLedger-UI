package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import java.time.LocalDate
import java.time.format.DateTimeFormatter

object BudgetCreationScreenDestination : AppNavigation {
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

    if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Budget created!", Toast.LENGTH_SHORT).show()
        navigateToBudgetInfoScreen(uiState.newBudgetId.toString())
        viewModel.resetLoadingStatus()
    } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed to create budget", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        BudgetCreationScreen(
            budgetName = uiState.budgetName,
            categoryName = uiState.categoryName,
            onBudgetNameChange = { viewModel.updateBudgetName(it) },
            budgetLimit = uiState.budgetLimit,
            onBudgetLimitChange = { value ->
                val filtered = value.filter { it.isDigit() || it == '.' }
                viewModel.updateBudgetLimit(filtered)
            },
            budgetEndDate = uiState.limitDate,
            onBudgetEndDateChange = { viewModel.updateLimitDate(it) },
            saveButtonEnabled = uiState.saveButtonEnabled,
            loadingStatus = uiState.loadingStatus,
            onCreateBudget = { viewModel.createBudget() },
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreen(
    budgetName: String,
    categoryName: String?,
    onBudgetNameChange: (String) -> Unit,
    budgetLimit: String,
    onBudgetLimitChange: (String) -> Unit,
    budgetEndDate: LocalDate?,
    onBudgetEndDateChange: (LocalDate) -> Unit,
    saveButtonEnabled: Boolean,
    loadingStatus: LoadingStatus,
    onCreateBudget: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = LocalDate.now()

    fun showDatePicker() {
        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->
                onBudgetEndDateChange(LocalDate.of(year, month + 1, day))
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )
        val minMillis = today.plusDays(1)
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        picker.datePicker.minDate = minMillis
        picker.show()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                horizontal = screenWidth(x = 16.0),
                vertical = screenHeight(x = 8.0)
            )
            .verticalScroll(rememberScrollState())
    ) {
        // Top bar
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    painter = painterResource(R.drawable.arrow_back),
                    contentDescription = "Back",
                    modifier = Modifier.size(screenWidth(x = 24.0)),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = "Set Budget",
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 18.0).sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.weight(1f))
            // balance the back button
            Spacer(modifier = Modifier.size(screenWidth(x = 48.0)))
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))

        // Hero header card
        Card(
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        Brush.horizontalGradient(
                            listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f)
                            )
                        )
                    )
                    .padding(screenWidth(x = 16.0))
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(screenWidth(x = 48.0))
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(screenWidth(x = 10.0))
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.wallet),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(screenWidth(x = 24.0))
                        )
                    }
                    Spacer(modifier = Modifier.width(screenWidth(x = 12.0)))
                    Column {
                        Text(
                            text = "New Budget",
                            fontWeight = FontWeight.Bold,
                            fontSize = screenFontSize(x = 16.0).sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Starts today · ${formatLocalDate(today)}",
                            fontSize = screenFontSize(x = 12.0).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 24.0)))

        // Budget name
        Text(
            text = "Budget name",
            fontWeight = FontWeight.SemiBold,
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
        )
        OutlinedTextField(
            value = budgetName,
            onValueChange = onBudgetNameChange,
            placeholder = {
                val monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                val placeholderText = if (!categoryName.isNullOrBlank())
                    "e.g. $categoryName – $monthYear"
                else
                    "e.g. Groceries – $monthYear"
                Text(
                    text = placeholderText,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Next,
                keyboardType = KeyboardType.Text
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))

        // Spending limit
        Text(
            text = "Spending limit (KES)",
            fontWeight = FontWeight.SemiBold,
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
        )
        OutlinedTextField(
            value = budgetLimit,
            onValueChange = onBudgetLimitChange,
            placeholder = {
                Text(
                    text = "0.00",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            },
            shape = RoundedCornerShape(12.dp),
            keyboardOptions = KeyboardOptions.Default.copy(
                imeAction = ImeAction.Done,
                keyboardType = KeyboardType.Decimal
            ),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))

        // End date
        Text(
            text = "Budget ends on",
            fontWeight = FontWeight.SemiBold,
            fontSize = screenFontSize(x = 14.0).sp,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outline,
                    shape = RoundedCornerShape(12.dp)
                )
                .clickable { showDatePicker() }
                .padding(
                    horizontal = screenWidth(x = 16.0),
                    vertical = screenHeight(x = 14.0)
                )
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (budgetEndDate != null) formatLocalDate(budgetEndDate) else "Select date",
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = if (budgetEndDate != null)
                        MaterialTheme.colorScheme.onSurface
                    else
                        MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Icon(
                    painter = painterResource(R.drawable.calendar),
                    contentDescription = "Pick date",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(screenWidth(x = 22.0))
                )
            }
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 12.0)))

        // Info row
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.info),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(screenWidth(x = 16.0))
            )
            Spacer(modifier = Modifier.width(screenWidth(x = 6.0)))
            Text(
                text = "Budget starts today (${formatLocalDate(today)})",
                fontSize = screenFontSize(x = 12.0).sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 32.0)))

        // Create button
        Button(
            onClick = onCreateBudget,
            enabled = saveButtonEnabled && loadingStatus != LoadingStatus.LOADING,
            shape = RoundedCornerShape(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(screenHeight(x = 50.0))
        ) {
            Text(
                text = if (loadingStatus == LoadingStatus.LOADING) "Creating..." else "Create Budget",
                fontSize = screenFontSize(x = 16.0).sp,
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
    }
}
