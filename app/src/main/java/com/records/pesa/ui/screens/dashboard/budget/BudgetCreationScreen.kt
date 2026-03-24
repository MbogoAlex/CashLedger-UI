package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.functions.formatMoneyValue
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
    navigateToCreateCategory: () -> Unit,
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
            uiState = uiState,
            onSelectBudgetType = { viewModel.selectBudgetType(it) },
            onGoToStep0 = { viewModel.goToStep0() },
            onDismissUpgradeDialog = { viewModel.dismissUpgradeDialog() },
            onSelectCategory = { viewModel.selectCategory(it) },
            onCategorySearchChange = { viewModel.updateCategorySearch(it) },
            onClearCategory = { viewModel.clearCategory() },
            onBudgetNameChange = { viewModel.updateBudgetName(it) },
            onBudgetLimitChange = { value ->
                val filtered = value.filter { it.isDigit() || it == '.' }
                viewModel.updateBudgetLimit(filtered)
            },
            onBudgetStartDateChange = { viewModel.updateStartDate(it) },
            onBudgetEndDateChange = { viewModel.updateLimitDate(it) },
            onAlertThresholdChange = { viewModel.setAlertThreshold(it) },
            onCreateBudget = { viewModel.createBudget() },
            navigateToPreviousScreen = navigateToPreviousScreen,
            navigateToCreateCategory = navigateToCreateCategory
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreen(
    uiState: BudgetCreationScreenUiState,
    onSelectBudgetType: (BudgetType) -> Unit,
    onGoToStep0: () -> Unit,
    onDismissUpgradeDialog: () -> Unit,
    onSelectCategory: (CategoryPickerItem) -> Unit,
    onCategorySearchChange: (String) -> Unit,
    onClearCategory: () -> Unit,
    onBudgetNameChange: (String) -> Unit,
    onBudgetLimitChange: (String) -> Unit,
    onBudgetStartDateChange: (LocalDate) -> Unit,
    onBudgetEndDateChange: (LocalDate) -> Unit,
    onAlertThresholdChange: (Int) -> Unit,
    onCreateBudget: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToCreateCategory: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val today = LocalDate.now()

    // On step 1, back always returns to step 0 (type selection)
    val isOnStep1 = uiState.step == 1
    BackHandler(enabled = isOnStep1) {
        onClearCategory()
        onGoToStep0()
    }

    // Upgrade dialog
    if (uiState.showUpgradeDialog) {
        AlertDialog(
            onDismissRequest = onDismissUpgradeDialog,
            icon = {
                Icon(
                    painter = painterResource(R.drawable.star),
                    contentDescription = null,
                    tint = Color(0xFFFFA000)
                )
            },
            title = { Text("Premium Feature") },
            text = {
                Text("Standalone budgets are a Premium feature. Upgrade to cap all outflows or track a specific transaction type across your entire account.")
            },
            confirmButton = {
                TextButton(onClick = onDismissUpgradeDialog) { Text("Upgrade") }
            },
            dismissButton = {
                TextButton(onClick = onDismissUpgradeDialog) { Text("Not now") }
            }
        )
    }

    fun showEndDatePicker() {
        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->
                onBudgetEndDateChange(LocalDate.of(year, month + 1, day))
            },
            today.year,
            today.monthValue - 1,
            today.dayOfMonth
        )
        val minMillis = (uiState.startDate.plusDays(1))
            .atStartOfDay(java.time.ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        picker.datePicker.minDate = minMillis
        picker.show()
    }

    fun showStartDatePicker() {
        val current = uiState.startDate
        val picker = DatePickerDialog(
            context,
            { _, year, month, day ->
                onBudgetStartDateChange(LocalDate.of(year, month + 1, day))
            },
            current.year,
            current.monthValue - 1,
            current.dayOfMonth
        )
        picker.show()
    }

    Column(modifier = modifier.fillMaxSize()) {

        // ── Fixed top bar ─────────────────────────────────────────────────
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp)
            ) {
                IconButton(onClick = {
                    if (isOnStep1) {
                        onClearCategory()
                        onGoToStep0()
                    } else {
                        navigateToPreviousScreen()
                    }
                }) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scaleX = -1f, scaleY = 1f),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = if (uiState.step == 0) "New Budget" else "Set Budget",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // ── Step 0: Budget type selection ─────────────────────────────────
        if (uiState.step == 0) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = screenWidth(x = 16.0), vertical = screenHeight(x = 24.0))
            ) {
                Text(
                    text = "What kind of budget?",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Choose how you want to track your spending",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Category Budget card
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectBudgetType(BudgetType.CATEGORY) },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.budget_2),
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Category",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Budget",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Track spending for a specific group (Food, Rent, etc.)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    // Standalone Budget card (premium)
                    ElevatedCard(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSelectBudgetType(BudgetType.STANDALONE) },
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.linearGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f)
                                        )
                                    )
                                )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        painter = painterResource(R.drawable.wallet),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(32.dp)
                                    )
                                    if (!uiState.isPremium) {
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(
                                            text = "PREMIUM",
                                            fontSize = 8.sp,
                                            color = Color.White,
                                            modifier = Modifier
                                                .background(
                                                    Color(0xFFFFA000),
                                                    RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Standalone",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = "Budget",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = "Cap all outflows or a specific type (Airtime, Withdrawals…)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                                if (!uiState.isPremium) {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Icon(
                                        painter = painterResource(R.drawable.lock),
                                        contentDescription = "Premium",
                                        tint = Color(0xFFFFA000),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        painter = painterResource(R.drawable.info),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "For category budgets, you need a category first.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            // ── Step 1: Budget form ───────────────────────────────────────
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        horizontal = screenWidth(x = 16.0),
                        vertical = screenHeight(x = 8.0)
                    )
                    .verticalScroll(rememberScrollState())
            ) {

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

                // ── Category picker (only for CATEGORY type without pre-selected category) ──
                if (uiState.budgetType == BudgetType.CATEGORY) {
                    Text(
                        text = "Category",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (uiState.categoryId == null) {
                        if (uiState.allCategories.isEmpty()) {
                            // No categories exist yet — guide the user
                            ElevatedCard(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.budget_2),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                        modifier = Modifier.size(40.dp)
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Text(
                                        text = "You don't have any categories yet",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.SemiBold,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = "Category budgets need a category to track. Create one first, then come back to set up your budget.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Button(
                                        onClick = navigateToCreateCategory,
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(
                                            painter = painterResource(R.drawable.ic_add),
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Create a Category First", fontWeight = FontWeight.SemiBold)
                                    }
                                }
                            }
                        } else {
                            OutlinedTextField(
                                value = uiState.categorySearch,
                                onValueChange = onCategorySearchChange,
                                placeholder = {
                                    Text(
                                        text = "Search categories…",
                                        fontSize = screenFontSize(x = 14.0).sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                },
                                trailingIcon = {
                                    Icon(
                                        painter = painterResource(R.drawable.search),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                shape = RoundedCornerShape(12.dp),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        Spacer(modifier = Modifier.height(4.dp))
                        val filtered = uiState.allCategories.filter {
                            it.name.contains(uiState.categorySearch, ignoreCase = true)
                        }
                        if (filtered.isEmpty()) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(
                                    text = if (uiState.categorySearch.isBlank()) "No categories yet"
                                           else "No categories found for \"${uiState.categorySearch}\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                OutlinedButton(
                                    onClick = navigateToCreateCategory,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Icon(
                                        painter = painterResource(R.drawable.ic_add),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Add category")
                                }
                            }
                        } else {
                            filtered.take(5).forEach { cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCategory(cat) }
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        Text(
                                            text = "${cat.transactionCount} transactions · last month: ${formatMoneyValue(cat.lastMonthSpend)}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                            }
                        }
                        } // end else (categories exist)
                    } else {
                        // Selected category chip
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                                .clickable { onClearCategory() }
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = uiState.categoryName ?: "",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (uiState.lastMonthSpend > 0.0) {
                                    Text(
                                        text = "Last month: ${formatMoneyValue(uiState.lastMonthSpend)}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                            Icon(
                                painter = painterResource(R.drawable.remove),
                                contentDescription = "Clear category",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Standalone: transaction type selector (coming soon) ─────
                if (uiState.budgetType == BudgetType.STANDALONE) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxWidth().background(
                                Brush.linearGradient(listOf(
                                    MaterialTheme.colorScheme.tertiary.copy(alpha = 0.12f),
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.06f)
                                ))
                            )
                        ) {
                            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(painter = painterResource(R.drawable.wallet), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text("What will this budget track?", fontWeight = FontWeight.Bold, fontSize = screenFontSize(x = 14.0).sp)
                                }
                                Spacer(Modifier.height(10.dp))
                                val types = listOf("All Outflows", "Send Money", "Buy Goods", "Paybill", "Withdraw", "Buy Airtime")
                                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    types.forEach { type ->
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(20.dp))
                                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                        ) {
                                            Text(type, fontSize = screenFontSize(x = 12.0).sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                                Spacer(Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)).padding(10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(painter = painterResource(R.drawable.info), contentDescription = null, modifier = Modifier.size(14.dp), tint = MaterialTheme.colorScheme.tertiary)
                                    Spacer(Modifier.width(8.dp))
                                    Text(
                                        "Standalone budgets (track spending across all categories) are coming in the next update. Your data is safe.",
                                        fontSize = screenFontSize(x = 11.0).sp,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                }

                // ── Budget name ────────────────────────────────────────────
                Text(
                    text = "Budget name",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
                )
                OutlinedTextField(
                    value = uiState.budgetName,
                    onValueChange = onBudgetNameChange,
                    placeholder = {
                        val monthYear = LocalDate.now().format(DateTimeFormatter.ofPattern("MMMM yyyy"))
                        val placeholderText = if (!uiState.categoryName.isNullOrBlank())
                            "e.g. ${uiState.categoryName} – $monthYear"
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

                // ── Spending limit ─────────────────────────────────────────
                Text(
                    text = "Spending limit (KES)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
                )

                // Smart suggestion banner
                if (uiState.avgSpend3Months > 0.0) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "Smart suggestion",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "You average ${formatMoneyValue(uiState.avgSpend3Months)}/month on ${uiState.categoryName}. Consider setting your limit around ${formatMoneyValue(uiState.avgSpend3Months * 1.1)} (+10% buffer).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                TextButton(
                                    onClick = {
                                        onBudgetLimitChange(uiState.avgSpend3Months.toInt().toString())
                                    },
                                    contentPadding = PaddingValues(0.dp)
                                ) {
                                    Text(
                                        text = "Use this suggestion →",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(screenHeight(x = 6.0)))
                }

                OutlinedTextField(
                    value = uiState.budgetLimit,
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

                // Validation warning: limit below last month spend
                val limitVal = uiState.budgetLimit.toDoubleOrNull() ?: 0.0
                if (limitVal > 0 && uiState.lastMonthSpend > 0 && limitVal < uiState.lastMonthSpend) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = Color(0xFFE65100),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "This is lower than last month's spend of ${formatMoneyValue(uiState.lastMonthSpend)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))

                // ── Alert Threshold ────────────────────────────────────────
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Alert Threshold",
                        fontWeight = FontWeight.SemiBold,
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (!uiState.isPremium) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Premium",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color(0xFFFFA000),
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Alert me when budget is ${uiState.alertThreshold}% used",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (uiState.isPremium) MaterialTheme.colorScheme.onSurfaceVariant
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Slider(
                    value = uiState.alertThreshold.toFloat(),
                    onValueChange = { onAlertThresholdChange(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8,
                    enabled = uiState.isPremium,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!uiState.isPremium) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onDismissUpgradeDialog() }
                            .padding(vertical = 4.dp)
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Upgrade to Premium to set a custom alert threshold",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))

                // ── Start date ─────────────────────────────────────────────
                Text(
                    text = "Budget starts on",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = screenFontSize(x = 14.0).sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(bottom = screenHeight(x = 6.0))
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                        .clickable { showStartDatePicker() }
                        .padding(horizontal = screenWidth(x = 16.0), vertical = screenHeight(x = 14.0))
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = formatLocalDate(uiState.startDate),
                            fontSize = screenFontSize(x = 14.0).sp,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            painter = painterResource(R.drawable.calendar),
                            contentDescription = "Pick start date",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(screenWidth(x = 22.0))
                        )
                    }
                }

                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))

                // ── End date ───────────────────────────────────────────────
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
                        .clickable { showEndDatePicker() }
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
                            text = if (uiState.limitDate != null) formatLocalDate(uiState.limitDate) else "Select date",
                            fontSize = screenFontSize(x = 14.0).sp,
                            color = if (uiState.limitDate != null)
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

                Spacer(modifier = Modifier.height(screenHeight(x = 32.0)))

                // Create button
                Button(
                    onClick = onCreateBudget,
                    enabled = uiState.saveButtonEnabled && uiState.loadingStatus != LoadingStatus.LOADING,
                    shape = RoundedCornerShape(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(screenHeight(x = 50.0))
                ) {
                    Text(
                        text = if (uiState.loadingStatus == LoadingStatus.LOADING) "Creating..." else "Create Budget",
                        fontSize = screenFontSize(x = 16.0).sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(screenHeight(x = 16.0)))
            } // end scrollable Column
        } // end step 1
    } // end outer Column
}
