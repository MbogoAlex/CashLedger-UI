package com.records.pesa.ui.screens.dashboard.category

import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.TransactionTypeData
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme

object CategoryAdditionScreenDestination : AppNavigation {
    override val title = "Category addition screen"
    override val route = "category-addition-screen"
}

private val commonSuggestions = listOf(
    "Groceries", "Transport", "School Fees", "Rent", "Utilities",
    "Entertainment", "Healthcare", "Savings", "Salary", "Shopping",
    "Food & Drink", "Investments", "Insurance", "Airtime & Data",
    "Spouse 💸", "Girlfriend 💝", "Family"
)

@RequiresApi(Build.VERSION_CODES.O)
@Composable
fun CategoryAdditionScreenComposable(
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: CategoryAdditionScreenViewModel =
        viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.loadingStatus == LoadingStatus.SUCCESS) {
        Toast.makeText(context, "Category created! Now add members to start tracking transactions.", Toast.LENGTH_SHORT).show()
        navigateToCategoryDetailsScreen(uiState.categoryId.toString())
        viewModel.resetLoadingStatus()
    } else if (uiState.loadingStatus == LoadingStatus.FAIL) {
        Toast.makeText(context, "Failed to create category. Try again.", Toast.LENGTH_SHORT).show()
        viewModel.resetLoadingStatus()
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        CategoryAdditionScreen(
            categoryName = uiState.categoryName,
            totalTransactions = uiState.totalTransactions,
            uncategorizedCount = uiState.uncategorizedCount,
            totalCategories = uiState.totalCategories,
            typeBreakdown = uiState.typeBreakdown,
            topUncategorizedEntities = uiState.topUncategorizedEntities,
            onChangeCategoryName = { viewModel.updateCategoryName(it) },
            onCreateCategory = { viewModel.createCategory() },
            navigateToPreviousScreen = navigateToPreviousScreen,
            loadingStatus = uiState.loadingStatus
        )
    }
}

@Composable
fun CategoryAdditionScreen(
    categoryName: String,
    totalTransactions: Int,
    uncategorizedCount: Int,
    totalCategories: Int,
    typeBreakdown: List<TransactionTypeData>,
    topUncategorizedEntities: List<AggregatedTransaction>,
    onChangeCategoryName: (String) -> Unit,
    onCreateCategory: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    loadingStatus: LoadingStatus,
    modifier: Modifier = Modifier
) {
    val focusManager = LocalFocusManager.current
    var showHowItWorks by rememberSaveable { mutableStateOf(false) }

    val avatarColor by animateColorAsState(
        targetValue = if (categoryName.isNotBlank()) txAvatarColor(categoryName)
        else MaterialTheme.colorScheme.outlineVariant,
        label = "avatarColor"
    )
    val initials = categoryName.trim()
        .split(" ")
        .filter { it.isNotEmpty() }
        .take(2)
        .joinToString("") { it.first().uppercaseChar().toString() }
        .ifEmpty { "?" }

    // Smart suggestions: prioritize top uncategorized entity names, supplement with common ones
    val smartSuggestions = remember(topUncategorizedEntities) {
        val entityNames = topUncategorizedEntities
            .map { it.nickName?.ifBlank { null } ?: it.entity }
            .filter { it.length <= 20 }
            .take(5)
        (entityNames + commonSuggestions).distinct().take(14)
    }

    val categorizedCount = totalTransactions - uncategorizedCount
    val categorizedPct = if (totalTransactions > 0)
        (categorizedCount * 100f / totalTransactions).toInt() else 0

    Column(modifier = modifier.fillMaxSize()) {
        // ── App bar ──────────────────────────────────────────────────────────
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            IconButton(onClick = navigateToPreviousScreen) {
                Icon(
                    painter = painterResource(R.drawable.ic_arrow_right),
                    contentDescription = "Back",
                    modifier = Modifier
                        .size(22.dp)
                        .graphicsLayer { rotationZ = 180f },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Text(
                text = "New Category",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f).padding(start = 4.dp)
            )
            // Step indicator
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Text(
                    text = "Step 1 of 2",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // ── Live category preview card ────────────────────────────────────
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                                        MaterialTheme.colorScheme.surface
                                    )
                                )
                            )
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(avatarColor),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = initials,
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 26.sp
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Text(
                            text = if (categoryName.isBlank()) "Your Category Name" else categoryName,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = if (categoryName.isBlank())
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
                            else MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = "Live preview",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            // ── Name input ────────────────────────────────────────────────────
            item {
                Column {
                    OutlinedTextField(
                        value = categoryName,
                        onValueChange = { if (it.length <= 50) onChangeCategoryName(it) },
                        label = {
                            Text("Category name", fontSize = 14.sp)
                        },
                        placeholder = {
                            Text(
                                "e.g. School Fees, Groceries…",
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Words,
                            imeAction = ImeAction.Done,
                            keyboardType = KeyboardType.Text
                        ),
                        keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                        trailingIcon = {
                            if (categoryName.isNotEmpty()) {
                                IconButton(onClick = { onChangeCategoryName("") }) {
                                    Icon(
                                        painter = painterResource(R.drawable.baseline_clear_24),
                                        contentDescription = "Clear",
                                        modifier = Modifier.size(18.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = avatarColor,
                            focusedLabelColor = avatarColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, end = 4.dp),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Text(
                            text = "${categoryName.length}/50",
                            fontSize = 11.sp,
                            color = if (categoryName.length > 45)
                                MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            // ── Smart suggestions ─────────────────────────────────────────────
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painter = painterResource(R.drawable.star),
                            contentDescription = null,
                            modifier = Modifier.size(15.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            text = "Quick picks",
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        smartSuggestions.forEach { suggestion ->
                            val isSelected = categoryName.equals(suggestion, ignoreCase = true)
                            val chipColor by animateColorAsState(
                                targetValue = if (isSelected) txAvatarColor(suggestion)
                                else MaterialTheme.colorScheme.surfaceVariant,
                                label = "chipColor"
                            )
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = chipColor,
                                modifier = Modifier.clickable { onChangeCategoryName(suggestion) }
                            ) {
                                Text(
                                    text = suggestion,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            // ── Insights card ─────────────────────────────────────────────────
            if (totalTransactions > 0) {
                item {
                    ElevatedCard(
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(
                                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                                            MaterialTheme.colorScheme.surface
                                        )
                                    )
                                )
                                .padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    painter = painterResource(R.drawable.chart),
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = "Your Transaction Insights",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }

                            // Stats row
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                InsightStatBox(
                                    label = "Total",
                                    value = totalTransactions.toString(),
                                    sub = "transactions",
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.weight(1f)
                                )
                                InsightStatBox(
                                    label = "Uncategorized",
                                    value = uncategorizedCount.toString(),
                                    sub = "need organizing",
                                    color = if (uncategorizedCount > 0)
                                        MaterialTheme.colorScheme.error
                                    else Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                                InsightStatBox(
                                    label = "Categories",
                                    value = totalCategories.toString(),
                                    sub = "tracking ${totalTransactions - uncategorizedCount} txns",
                                    color = Color(0xFF2E7D32),
                                    modifier = Modifier.weight(1f)
                                )
                            }

                            // Uncategorized percentage bar
                            if (totalTransactions > 0) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Categorization progress",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                        Text(
                                            text = "$categorizedPct%",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(3.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth(categorizedPct / 100f)
                                                .height(6.dp)
                                                .clip(RoundedCornerShape(3.dp))
                                                .background(MaterialTheme.colorScheme.primary)
                                        )
                                    }
                                }
                            }

                            // Type breakdown (top 4)
                            if (typeBreakdown.isNotEmpty()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )
                                Text(
                                    text = "Activity by type",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                val topTypes = typeBreakdown.take(4)
                                val maxCount = topTypes.maxOf { it.count }.coerceAtLeast(1)
                                topTypes.forEach { type ->
                                    val fraction = type.count.toFloat() / maxCount
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = type.transactionType,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.width(110.dp)
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Box(modifier = Modifier.weight(1f)) {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxWidth(fraction)
                                                    .height(8.dp)
                                                    .clip(RoundedCornerShape(4.dp))
                                                    .background(txAvatarColor(type.transactionType))
                                            )
                                        }
                                        Spacer(Modifier.width(8.dp))
                                        Text(
                                            text = "${type.count}",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface,
                                            modifier = Modifier.width(32.dp),
                                            textAlign = TextAlign.End
                                        )
                                    }
                                }
                            }

                            // Top uncategorized entities
                            if (topUncategorizedEntities.isNotEmpty()) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f),
                                    thickness = 1.dp
                                )
                                Text(
                                    text = "Frequently uncategorized contacts",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                topUncategorizedEntities.take(5).forEach { entity ->
                                    val displayName = entity.nickName
                                        ?.takeIf { it.isNotBlank() } ?: entity.entity
                                    val color = txAvatarColor(displayName)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(32.dp)
                                                .clip(CircleShape)
                                                .background(color),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = displayName.firstOrNull()
                                                    ?.uppercaseChar()?.toString() ?: "?",
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp
                                            )
                                        }
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = displayName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis,
                                                color = MaterialTheme.colorScheme.onSurface
                                            )
                                            Text(
                                                text = entity.transactionType,
                                                fontSize = 10.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Surface(
                                            shape = RoundedCornerShape(20.dp),
                                            color = color.copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "${entity.times} txn${if (entity.times != 1) "s" else ""}",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = color,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ── How it works (collapsible) ────────────────────────────────────
            item {
                ElevatedCard(
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showHowItWorks = !showHowItWorks }
                                .padding(14.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.info),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "How categories work",
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                painter = painterResource(
                                    if (showHowItWorks) R.drawable.arrow_upward else R.drawable.arrow_downward
                                ),
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        AnimatedVisibility(
                            visible = showHowItWorks,
                            enter = expandVertically(),
                            exit = shrinkVertically()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 14.dp, end = 14.dp, bottom = 14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                HorizontalDivider(
                                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
                                )
                                HowItWorksStep(
                                    step = 1,
                                    title = "Name your category",
                                    desc = "Give it a meaningful name like 'School Fees' or 'Groceries'.",
                                    isCurrentStep = true
                                )
                                HowItWorksStep(
                                    step = 2,
                                    title = "Add members",
                                    desc = "Next, you'll add members — contacts or entities whose transactions belong to this category.",
                                    isCurrentStep = false
                                )
                                HowItWorksStep(
                                    step = 3,
                                    title = "Track & analyse",
                                    desc = "View spending totals, trends, and download reports per category.",
                                    isCurrentStep = false
                                )
                            }
                        }
                    }
                }
            }

            // Spacer before button
            item { Spacer(Modifier.height(8.dp)) }
        }

        // ── Create button ────────────────────────────────────────────────────
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Button(
                enabled = categoryName.isNotBlank() && loadingStatus != LoadingStatus.LOADING,
                onClick = onCreateCategory,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (categoryName.isNotBlank())
                        txAvatarColor(categoryName)
                    else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                if (loadingStatus == LoadingStatus.LOADING) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(Modifier.width(10.dp))
                    Text("Creating…", fontSize = 15.sp, color = Color.White)
                } else {
                    Text(
                        text = "Create & Add Members →",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

@Composable
private fun InsightStatBox(
    label: String,
    value: String,
    sub: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(color.copy(alpha = 0.08f))
            .padding(10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 20.sp,
            color = color
        )
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Text(
            text = sub,
            fontSize = 9.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun HowItWorksStep(
    step: Int,
    title: String,
    desc: String,
    isCurrentStep: Boolean
) {
    val color = if (isCurrentStep) MaterialTheme.colorScheme.primary
    else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
    Row(
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(26.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = if (isCurrentStep) 1f else 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = step.toString(),
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = if (isCurrentStep) Color.White else color
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                color = if (isCurrentStep) MaterialTheme.colorScheme.onSurface
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Text(
                text = desc,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
                    alpha = if (isCurrentStep) 0.85f else 0.5f
                )
            )
        }
    }
}

@Preview(showBackground = true, showSystemUi = true)
@Composable
fun CategoryAdditionScreenPreview() {
    CashLedgerTheme {
        CategoryAdditionScreen(
            categoryName = "School Fees",
            totalTransactions = 340,
            uncategorizedCount = 87,
            totalCategories = 5,
            typeBreakdown = listOf(
                TransactionTypeData("Send Money", 120, 45000.0),
                TransactionTypeData("Pay Bill", 80, 32000.0),
                TransactionTypeData("Withdraw Cash", 60, 18000.0),
                TransactionTypeData("Buy Goods", 40, 12000.0),
            ),
            topUncategorizedEntities = listOf(
                AggregatedTransaction("equity_bank", "Equity Bank", "Pay Bill", 18, 24000.0, 180.0),
                AggregatedTransaction("mama_mboga", "Mama Mboga", "Buy Goods", 14, 8400.0, 112.0),
                AggregatedTransaction("nairobi_water", null, "Pay Bill", 12, 15600.0, 96.0),
            ),
            onChangeCategoryName = {},
            onCreateCategory = {},
            navigateToPreviousScreen = {},
            loadingStatus = LoadingStatus.INITIAL
        )
    }
}