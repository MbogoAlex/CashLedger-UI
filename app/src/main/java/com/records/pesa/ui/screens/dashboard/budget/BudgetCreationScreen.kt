package com.records.pesa.ui.screens.dashboard.budget

import android.app.DatePickerDialog
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.functions.formatLocalDate
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.functions.RecurrenceHelper
import com.records.pesa.functions.RecurrenceType
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.components.SubscriptionDialog
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import java.time.LocalDate

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
    var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = { showSubscriptionDialog = false },
        )
    }

    LaunchedEffect(uiState.loadingStatus) {
        when (uiState.loadingStatus) {
            LoadingStatus.SUCCESS -> {
                navigateToBudgetInfoScreen(uiState.newBudgetId.toString())
                viewModel.resetLoadingStatus()
            }
            LoadingStatus.FAIL -> {
                Toast.makeText(context, "Failed to create budget. Try again.", Toast.LENGTH_SHORT).show()
                viewModel.resetLoadingStatus()
            }
            else -> {}
        }
    }

    BudgetCreationScreen(
        uiState = uiState,
        onSelectCategory = { viewModel.selectCategory(it) },
        onCategorySearchChange = { viewModel.updateCategorySearch(it) },
        onClearCategory = { viewModel.clearCategory() },
        onBudgetNameChange = { viewModel.updateBudgetName(it) },
        onBudgetLimitChange = { viewModel.updateBudgetLimit(it) },
        onBudgetStartDateChange = { viewModel.updateStartDate(it) },
        onBudgetEndDateChange = { viewModel.updateLimitDate(it) },
        onAlertThresholdChange = { viewModel.setAlertThreshold(it) },
        onToggleMember = { viewModel.toggleMember(it) },
        onToggleRecurring = { viewModel.toggleRecurring() },
        onRecurrenceTypeChange = { viewModel.setRecurrenceType(it) },
        onRecurrenceIntervalDaysChange = { viewModel.setRecurrenceIntervalDays(it) },
        onShowSubscriptionDialog = { showSubscriptionDialog = true },
        onCreateBudget = { viewModel.createBudget() },
        navigateToPreviousScreen = navigateToPreviousScreen,
        navigateToCreateCategory = navigateToCreateCategory,
        modifier = modifier,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun BudgetCreationScreen(
    uiState: BudgetCreationScreenUiState,
    onSelectCategory: (CategoryPickerItem) -> Unit,
    onCategorySearchChange: (String) -> Unit,
    onClearCategory: () -> Unit,
    onBudgetNameChange: (String) -> Unit,
    onBudgetLimitChange: (String) -> Unit,
    onBudgetStartDateChange: (LocalDate) -> Unit,
    onBudgetEndDateChange: (LocalDate) -> Unit,
    onAlertThresholdChange: (Int) -> Unit,
    onToggleMember: (String) -> Unit = {},
    onToggleRecurring: () -> Unit,
    onRecurrenceTypeChange: (String) -> Unit,
    onRecurrenceIntervalDaysChange: (Int) -> Unit,
    onShowSubscriptionDialog: () -> Unit = {},
    onCreateBudget: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToCreateCategory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    fun showEndDatePicker() {
        val current = uiState.limitDate ?: uiState.startDate
        val picker = DatePickerDialog(
            context,
            { _, year, month, day -> onBudgetEndDateChange(LocalDate.of(year, month + 1, day)) },
            current.year, current.monthValue - 1, current.dayOfMonth
        )
        val minMillis = uiState.startDate.toEpochDay() * 86_400_000L + 86_400_000L
        picker.datePicker.minDate = minMillis
        picker.show()
    }

    fun showStartDatePicker() {
        val current = uiState.startDate
        val picker = DatePickerDialog(
            context,
            { _, year, month, day -> onBudgetStartDateChange(LocalDate.of(year, month + 1, day)) },
            current.year, current.monthValue - 1, current.dayOfMonth
        )
        picker.show()
    }

    Column(modifier = modifier.fillMaxSize()) {

        // Top bar
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding(),
            color = MaterialTheme.colorScheme.surface,
            shadowElevation = 0.dp,
            tonalElevation = 0.dp,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                IconButton(onClick = navigateToPreviousScreen) {
                    Icon(
                        painter = painterResource(R.drawable.ic_arrow_right),
                        contentDescription = "Back",
                        modifier = Modifier
                            .size(22.dp)
                            .scale(scaleX = -1f, scaleY = 1f),
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = "New Budget",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f),
                )
            }
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        // Scrollable form
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = screenWidth(x = 16.0), vertical = screenHeight(x = 12.0)),
            verticalArrangement = Arrangement.spacedBy(screenHeight(x = 20.0)),
        ) {

            // ── Hero card ───────────────────────────────────────────────────
            Card(
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.15f),
                                    MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
                                )
                            )
                        )
                        .padding(16.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.budget_2),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp),
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = if (uiState.budgetName.isBlank()) "New Budget" else uiState.budgetName,
                                fontWeight = FontWeight.Bold,
                                fontSize = screenFontSize(x = 16.0).sp,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = if (uiState.categoryName != null) "Category: ${uiState.categoryName}"
                                       else "Starts ${formatLocalDate(uiState.startDate)}",
                                fontSize = screenFontSize(x = 12.0).sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }

            // ── Category ────────────────────────────────────────────────────
            SectionLabel("Category")
            Text(
                text = if (uiState.categoryId == null) "Select the category this budget will track"
                       else "Tap to change or remove the selected category",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (uiState.categoryId == null) {
                if (uiState.allCategories.isEmpty()) {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.budget_2),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.45f),
                                modifier = Modifier.size(40.dp),
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "No categories yet",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Create a category first, then set up a budget for it.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Button(
                                onClick = navigateToCreateCategory,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Create a Category", fontWeight = FontWeight.SemiBold)
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
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        },
                        trailingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.search),
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    val filtered = uiState.allCategories.filter {
                        it.name.contains(uiState.categorySearch, ignoreCase = true)
                    }
                    if (filtered.isEmpty()) {
                        Text(
                            text = "No categories found for \"${uiState.categorySearch}\"",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 8.dp),
                        )
                    } else {
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            filtered.take(6).forEachIndexed { index, cat ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onSelectCategory(cat) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = cat.name,
                                            style = MaterialTheme.typography.bodyMedium,
                                            fontWeight = FontWeight.SemiBold,
                                        )
                                        Text(
                                            text = "Last month: ${formatMoneyValue(cat.lastMonthSpend)}  ·  ${cat.transactionCount} txns",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    Icon(
                                        painter = painterResource(R.drawable.ic_arrow_right),
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.size(16.dp),
                                    )
                                }
                                if (index < filtered.take(6).lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            } else {
                // Selected category chip
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.08f))
                        .clickable { onClearCategory() }
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = uiState.categoryName ?: "",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                        )
                        if (uiState.lastMonthSpend > 0.0) {
                            Text(
                                text = "Last month: ${formatMoneyValue(uiState.lastMonthSpend)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    Icon(
                        painter = painterResource(R.drawable.remove),
                        contentDescription = "Clear category",
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            // ── Budget name ────────────────────────────────────────────────
            SectionLabel("Budget name")
            OutlinedTextField(
                value = uiState.budgetName,
                onValueChange = onBudgetNameChange,
                placeholder = {
                    Text(
                        text = if (!uiState.categoryName.isNullOrBlank())
                            "e.g. ${uiState.categoryName} Budget"
                        else "e.g. Groceries Budget",
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Next,
                    keyboardType = KeyboardType.Text,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Spending limit ─────────────────────────────────────────────
            SectionLabel("Spending limit (KES)")

            if (uiState.avgSpend3Months > 0.0) {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.elevatedCardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                    ),
                    elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.info),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "3-month avg spend: ${formatMoneyValue(uiState.avgSpend3Months)}  ·  Last month: ${formatMoneyValue(uiState.lastMonthSpend)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            OutlinedTextField(
                value = uiState.budgetLimit,
                onValueChange = onBudgetLimitChange,
                placeholder = {
                    Text(
                        text = "0.00",
                        fontSize = screenFontSize(x = 14.0).sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                },
                shape = RoundedCornerShape(12.dp),
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Done,
                    keyboardType = KeyboardType.Decimal,
                ),
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            // ── Recurrence ─────────────────────────────────────────────────
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                if (!uiState.isPremium) {
                    ElevatedCard(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onShowSubscriptionDialog() },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 0.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.lock),
                                contentDescription = null,
                                modifier = Modifier.size(20.dp),
                                tint = MaterialTheme.colorScheme.primary,
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "Recurring Budgets — Premium",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                                Text(
                                    "Automatically reset your budget on a schedule — daily, weekly, monthly and more. Upgrade to unlock.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }
                } else {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            SectionLabel("Repeat this budget")
                            Text(
                                text = if (uiState.isRecurring)
                                    "Budget will automatically reset at the end of each cycle. Past cycles are saved in history."
                                else
                                    "Turn on to make this a recurring budget that resets on a schedule.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = uiState.isRecurring,
                            onCheckedChange = { onToggleRecurring() },
                        )
                    }
                    if (uiState.isRecurring) {
                        Text(
                            text = "Reset every:",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            RecurrenceType.entries.forEach { type ->
                                FilterChip(
                                    selected = uiState.recurrenceType == type.name,
                                    onClick = { onRecurrenceTypeChange(type.name) },
                                    label = { Text(type.displayName, fontSize = screenFontSize(x = 13.0).sp) },
                                )
                            }
                        }
                        if (uiState.recurrenceType == RecurrenceType.CUSTOM.name) {
                            OutlinedTextField(
                                value = if (uiState.recurrenceIntervalDays > 0) uiState.recurrenceIntervalDays.toString() else "",
                                onValueChange = { v -> v.toIntOrNull()?.let { onRecurrenceIntervalDaysChange(it) } },
                                label = { Text("Interval in days") },
                                placeholder = { Text("e.g. 45") },
                                keyboardOptions = KeyboardOptions.Default.copy(keyboardType = KeyboardType.Number, imeAction = ImeAction.Done),
                                singleLine = true,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        }
                    }
                }
            }

            // ── Dates ──────────────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel("Start date")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { showStartDatePicker() },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = formatLocalDate(uiState.startDate),
                            fontSize = screenFontSize(x = 13.0).sp,
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    SectionLabel(if (uiState.isRecurring) "Cycle ends (auto)" else "End date")
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedButton(
                        onClick = { if (!uiState.isRecurring) showEndDatePicker() },
                        enabled = !uiState.isRecurring,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (uiState.limitDate != null) formatLocalDate(uiState.limitDate) else "Pick date",
                            fontSize = screenFontSize(x = 13.0).sp,
                            color = if (uiState.limitDate != null) MaterialTheme.colorScheme.onSurface
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (uiState.isRecurring) {
                        Text(
                            text = "Set by recurrence type",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            // ── Alert threshold ────────────────────────────────────────────
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SectionLabel("Alert threshold")
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${uiState.alertThreshold}%",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        if (!uiState.isPremium) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Icon(
                                painter = painterResource(R.drawable.star),
                                contentDescription = "Premium",
                                tint = Color(0xFFFFD700),
                                modifier = Modifier.size(14.dp),
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Slider(
                    value = uiState.alertThreshold.toFloat(),
                    onValueChange = { onAlertThresholdChange(it.toInt()) },
                    valueRange = 10f..100f,
                    steps = 8,
                    enabled = uiState.isPremium,
                    modifier = Modifier.fillMaxWidth(),
                )
                Text(
                    text = "You'll be notified when spending reaches ${uiState.alertThreshold}% of the limit",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            // ── Member filter ──────────────────────────────────────────────
            if (uiState.categoryId != null) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    SectionLabel("Filter by member (optional)")
                    Text(
                        text = if (uiState.selectedMembers.isEmpty())
                            "No member selected — the entire \"${uiState.categoryName}\" category will be tracked. Select one or more members below to track only their expenses."
                        else
                            "${uiState.selectedMembers.size} member${if (uiState.selectedMembers.size > 1) "s" else ""} selected — only transactions from ${uiState.selectedMembers.joinToString(", ")} will count toward this budget.",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (uiState.selectedMembers.isEmpty()) MaterialTheme.colorScheme.onSurfaceVariant
                                else MaterialTheme.colorScheme.primary,
                    )
                    if (uiState.categoryMembers.isEmpty()) {
                        Text(
                            text = "No members found for this category.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        Spacer(modifier = Modifier.height(2.dp))
                        Card(
                            shape = RoundedCornerShape(12.dp),
                            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            uiState.categoryMembers.forEachIndexed { index, memberName ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onToggleMember(memberName) }
                                        .padding(horizontal = 12.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = memberName in uiState.selectedMembers,
                                        onCheckedChange = { onToggleMember(memberName) },
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = memberName,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                                if (index < uiState.categoryMembers.lastIndex) {
                                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(screenHeight(x = 8.0)))
        } // end scrollable column

        // ── Pinned Create button ───────────────────────────────────────────
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                onClick = onCreateBudget,
                enabled = uiState.saveButtonEnabled && uiState.loadingStatus != LoadingStatus.LOADING,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            ) {
                if (uiState.loadingStatus == LoadingStatus.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary,
                    )
                } else {
                    Text(
                        text = "Create Budget",
                        fontSize = screenFontSize(x = 15.0).sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        fontWeight = FontWeight.SemiBold,
        fontSize = screenFontSize(x = 14.0).sp,
        color = MaterialTheme.colorScheme.onSurface,
    )
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, name = "With categories")
@Composable
private fun BudgetCreationScreenPreview() {
    MaterialTheme {
        BudgetCreationScreen(
            uiState = BudgetCreationScreenUiState(
                allCategories = listOf(
                    CategoryPickerItem(1, "Food & Groceries", 4200.0, 12),
                    CategoryPickerItem(2, "Transport", 1800.0, 8),
                    CategoryPickerItem(3, "Entertainment", 950.0, 5),
                ),
                budgetName = "Monthly Groceries",
                budgetLimit = "5000",
                alertThreshold = 80,
                saveButtonEnabled = true,
            ),
            onSelectCategory = {},
            onCategorySearchChange = {},
            onClearCategory = {},
            onBudgetNameChange = {},
            onBudgetLimitChange = {},
            onBudgetStartDateChange = {},
            onBudgetEndDateChange = {},
            onAlertThresholdChange = {},
            onToggleRecurring = {},
            onRecurrenceTypeChange = {},
            onRecurrenceIntervalDaysChange = {},
            onCreateBudget = {},
            navigateToPreviousScreen = {},
            navigateToCreateCategory = {},
        )
    }
}

@RequiresApi(Build.VERSION_CODES.O)
@Preview(showBackground = true, name = "No categories yet")
@Composable
private fun BudgetCreationScreenNoCategoriesPreview() {
    MaterialTheme {
        BudgetCreationScreen(
            uiState = BudgetCreationScreenUiState(allCategories = emptyList()),
            onSelectCategory = {},
            onCategorySearchChange = {},
            onClearCategory = {},
            onBudgetNameChange = {},
            onBudgetLimitChange = {},
            onBudgetStartDateChange = {},
            onBudgetEndDateChange = {},
            onAlertThresholdChange = {},
            onToggleRecurring = {},
            onRecurrenceTypeChange = {},
            onRecurrenceIntervalDaysChange = {},
            onCreateBudget = {},
            navigateToPreviousScreen = {},
            navigateToCreateCategory = {},
        )
    }
}
