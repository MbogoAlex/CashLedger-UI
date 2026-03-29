package com.records.pesa.ui.screens.dashboard.category

import android.app.DatePickerDialog
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.pullRefresh
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.TransactionCategory
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.transactionCategories
import com.records.pesa.ui.screens.components.txAvatarColor
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime
import com.records.pesa.ui.screens.components.SubscriptionDialog
import com.records.pesa.ui.screens.components.DownloadReportDialog
import java.time.ZoneId

object CategoriesScreenDestination : AppNavigation {
    override val title: String = "Categories screen"
    override val route: String = "categories-screen"
}

// ─── Composable entry point ───────────────────────────────────────────────────

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
    val context = LocalContext.current
    BackHandler(onBack = {
        if (showBackArrow) navigateToPreviousScreen() else navigateToHomeScreen()
    })

    val viewModel: CategoriesScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = { viewModel.getUserCategories() }
    )

    var showSubscriptionDialog by rememberSaveable { mutableStateOf(false) }
    var filteringOn by rememberSaveable { mutableStateOf(false) }
    var showDownloadReportDialog by rememberSaveable { mutableStateOf(false) }
    var reportType by rememberSaveable { mutableStateOf("PDF") }
    var pendingStartDate by remember { mutableStateOf(LocalDate.now().withDayOfMonth(1)) }
    var pendingEndDate by remember { mutableStateOf(LocalDate.now()) }

    val createDocumentLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
            uri?.let {
                viewModel.fetchReportAndSave(
                    context = context,
                    saveUri = it,
                    reportType = reportType,
                    startDate = pendingStartDate,
                    endDate = pendingEndDate
                )
            }
        }

    if (uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        filteringOn = false
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        if (reportType == "PDF") {
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))
        } else if (reportType == "CSV") {
            val csvIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(csvIntent, "Open CSV with:"))
        }
    } else if (uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    if (showSubscriptionDialog) {
        SubscriptionDialog(
            onDismiss = { showSubscriptionDialog = false },
            onConfirm = {
                showSubscriptionDialog = false
                navigateToSubscriptionScreen()
            }
        )
    }

    if (showDownloadReportDialog) {
        DownloadReportDialog(
            isPremium = uiState.isPremium,
            onDismiss = { showDownloadReportDialog = false },
            onConfirm = { type, startDate, endDate ->
                reportType = type
                pendingStartDate = startDate
                pendingEndDate = endDate
                showDownloadReportDialog = false
                val suffix = if (type == "PDF") ".pdf" else ".csv"
                val mime = if (type == "PDF") "application/pdf" else "text/csv"
                createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}$suffix")
            }
        )
    }

    Box(modifier = Modifier.safeDrawingPadding()) {
        CategoriesScreen(
            pullRefreshState = pullRefreshState,
            loadingStatus = uiState.loadingStatus,
            premium = uiState.userDetails.paymentStatus || uiState.userDetails.phoneNumber == "0888888888",
            searchQuery = uiState.name,
            startDate = LocalDate.parse(uiState.startDate),
            endDate = LocalDate.parse(uiState.endDate),
            onChangeStartDate = { viewModel.changeStartDate(it) },
            onChangeLastDate = { viewModel.changeEndDate(it) },
            onChangeSearchQuery = { viewModel.updateName(it) },
            onClearSearch = { viewModel.updateName("") },
            categories = uiState.categories,
            selectedCategories = uiState.selectedCategories,
            onRemoveCategory = { viewModel.removeCategoryId(it) },
            onAddCategory = { viewModel.addCategoryId(it) },
            navigateToCategoryDetailsScreen = navigateToCategoryDetailsScreen,
            navigateToCategoryAdditionScreen = navigateToCategoryAdditionScreen,
            navigateToPreviousScreen = navigateToPreviousScreen,
            onShowSubscriptionDialog = { showSubscriptionDialog = true },
            onDownloadReport = { showDownloadReportDialog = !showDownloadReportDialog },
            onFilter = {
                if (filteringOn) viewModel.clearSelectedCategories()
                filteringOn = !filteringOn
            },
            filteringOn = filteringOn,
            showBackArrow = showBackArrow,
            getCategories = { viewModel.getUserCategories() },
            downloadingStatus = uiState.downloadingStatus
        )
    }
}

// ─── Main screen ──────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun CategoriesScreen(
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    premium: Boolean,
    searchQuery: String,
    startDate: LocalDate,
    endDate: LocalDate,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    categories: List<TransactionCategory>,
    selectedCategories: List<Int>,
    onRemoveCategory: (id: Int) -> Unit,
    onAddCategory: (id: Int) -> Unit,
    navigateToCategoryDetailsScreen: (categoryId: String) -> Unit,
    navigateToCategoryAdditionScreen: () -> Unit,
    navigateToPreviousScreen: () -> Unit,
    showBackArrow: Boolean,
    onShowSubscriptionDialog: () -> Unit,
    onClearSearch: () -> Unit,
    onDownloadReport: () -> Unit,
    onChangeSearchQuery: (value: String) -> Unit,
    downloadingStatus: DownloadingStatus,
    onFilter: () -> Unit,
    filteringOn: Boolean,
    getCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = loadingStatus == LoadingStatus.LOADING
    var showSearch by rememberSaveable { mutableStateOf(false) }
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current

    LaunchedEffect(showSearch) {
        if (showSearch) focusRequester.requestFocus()
        else {
            keyboard?.hide()
            onClearSearch()
        }
    }

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter { it.name.contains(searchQuery, ignoreCase = true) }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {

            // ── Top app bar ───────────────────────────────────────────────────
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 0.dp,
                tonalElevation = 0.dp
            ) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back arrow — only when showBackArrow is true
                        if (showBackArrow) {
                            IconButton(onClick = navigateToPreviousScreen) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_arrow_right),
                                    contentDescription = "Back",
                                    modifier = Modifier
                                        .size(22.dp)
                                        .scale(scaleX = -1f, scaleY = 1f)
                                )
                            }
                        }

                        // Inline search field or title
                        if (showSearch) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = onChangeSearchQuery,
                                placeholder = {
                                    Text(
                                        "Search categories\u2026",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                                    )
                                },
                                trailingIcon = {
                                    IconButton(onClick = { showSearch = false }) {
                                        Icon(
                                            painter = painterResource(R.drawable.baseline_clear_24),
                                            contentDescription = "Close search",
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                keyboardActions = KeyboardActions(onDone = { keyboard?.hide() }),
                                colors = TextFieldDefaults.colors(
                                    focusedIndicatorColor = MaterialTheme.colorScheme.primary,
                                    unfocusedIndicatorColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                            )
                        } else {
                            Text(
                                text = "Categories",
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = if (showBackArrow) 4.dp else 12.dp)
                            )
                        }

                        // Search icon toggle
                        if (!showSearch) {
                            IconButton(onClick = { showSearch = true }) {
                                Icon(
                                    painter = painterResource(R.drawable.search),
                                    contentDescription = "Search",
                                    modifier = Modifier.size(20.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        // Select mode toggle
                        IconButton(onClick = onFilter) {
                            Icon(
                                painter = painterResource(if (filteringOn) R.drawable.baseline_clear_24 else R.drawable.check_box_blank),
                                contentDescription = if (filteringOn) "Cancel selection" else "Select categories",
                                tint = if (filteringOn) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Add (+) button
                        if (!filteringOn) {
                            IconButton(onClick = navigateToCategoryAdditionScreen) {
                                Icon(
                                    painter = painterResource(R.drawable.ic_add),
                                    contentDescription = "Add category",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }

                        // Download icon — enabled when categories selected
                        IconButton(
                            onClick = onDownloadReport,
                            enabled = selectedCategories.isNotEmpty()
                        ) {
                            if (downloadingStatus == DownloadingStatus.LOADING) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(18.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(
                                    painter = painterResource(R.drawable.download),
                                    contentDescription = "Download report",
                                    modifier = Modifier.size(20.dp),
                                    tint = if (selectedCategories.isNotEmpty())
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                        thickness = 1.dp
                    )
                }
            }

            // ── Scrollable body ───────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .weight(1f)
                    .then(
                        if (pullRefreshState != null) Modifier.pullRefresh(pullRefreshState)
                        else Modifier
                    )
            ) {
                if (isLoading && categories.isEmpty()) {
                    // Full-screen loader on initial load
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 120.dp)
                    ) {
                        // Hero summary card (scrolls with list)
                        item {
                            CategoriesHeroCard(
                                categories = categories,
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }

                        // Empty state
                        if (filteredCategories.isEmpty()) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(260.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            painter = painterResource(R.drawable.categories),
                                            contentDescription = null,
                                            modifier = Modifier.size(52.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                        )
                                        Spacer(modifier = Modifier.height(14.dp))
                                        Text(
                                            text = "No categories yet",
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "Categories group your M-PESA transactions and let you add your own custom expenses — cash, bank transfers, anything — for a complete financial picture.",
                                            fontSize = 13.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                            modifier = Modifier.padding(horizontal = 16.dp)
                                        )
                                        Spacer(modifier = Modifier.height(6.dp))
                                        Text(
                                            text = "Tap + to create your first category",
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                            }
                        } else {
                            itemsIndexed(filteredCategories, key = { _, it -> it.id }) { index, category ->
                                val isLocked = index != 0 && !premium
                                CategoryItemRow(
                                    category = category,
                                    isSelected = category.id in selectedCategories,
                                    filteringOn = filteringOn,
                                    isLocked = isLocked,
                                    onToggleSelection = {
                                        if (category.id in selectedCategories)
                                            onRemoveCategory(category.id)
                                        else
                                            onAddCategory(category.id)
                                    },
                                    onClick = {
                                        if (isLocked) onShowSubscriptionDialog()
                                        else navigateToCategoryDetailsScreen(category.id.toString())
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                )
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.07f)
                                )
                            }
                        }
                    }
                }

                // Pull-to-refresh indicator
                if (pullRefreshState != null) {
                    PullRefreshIndicator(
                        refreshing = isLoading,
                        state = pullRefreshState,
                        modifier = Modifier.align(Alignment.TopCenter),
                        backgroundColor = MaterialTheme.colorScheme.surface,
                        contentColor = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // ── Selection banner (bottom) — shown when categories are selected ───
        if (selectedCategories.isNotEmpty()) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 24.dp),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = onDownloadReport)
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${selectedCategories.size} selected",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "Download report",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

// ─── Hero summary card ────────────────────────────────────────────────────────

@Composable
private fun CategoriesHeroCard(
    categories: List<TransactionCategory>,
    modifier: Modifier = Modifier
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val totalIn = remember(categories) {
        categories.sumOf { cat ->
            cat.transactions.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
        }
    }
    val totalOut = remember(categories) {
        categories.sumOf { cat ->
            cat.transactions.filter { it.transactionAmount < 0 }
                .sumOf { kotlin.math.abs(it.transactionAmount) }
        }
    }
    val totalTransactions = remember(categories) { categories.sumOf { it.transactions.size } }

    ElevatedCard(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = 4.dp,
                shape = RoundedCornerShape(20.dp),
                spotColor = primaryColor.copy(alpha = 0.15f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.linearGradient(
                        listOf(
                            primaryColor.copy(alpha = 0.12f),
                            MaterialTheme.colorScheme.tertiary.copy(alpha = 0.06f),
                            primaryColor.copy(alpha = 0.04f)
                        )
                    )
                )
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CATEGORIES OVERVIEW",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = primaryColor.copy(alpha = 0.8f),
                        letterSpacing = 1.sp
                    )
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(primaryColor.copy(alpha = 0.10f))
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "${categories.size} ${if (categories.size == 1) "category" else "categories"}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = primaryColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Total Transactions",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 0.5.sp
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "$totalTransactions txn${if (totalTransactions != 1) "s" else ""}",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(14.dp))
                HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Money In",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "+Ksh ${String.format("%,.2f", totalIn)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.tertiary
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = "Money Out",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f),
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 0.3.sp
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = "-Ksh ${String.format("%,.2f", totalOut)}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ─── Category item row ────────────────────────────────────────────────────────

@Composable
private fun CategoryItemRow(
    category: TransactionCategory,
    isSelected: Boolean,
    filteringOn: Boolean,
    isLocked: Boolean = false,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val totalIn = remember(category) {
        category.transactions.filter { it.transactionAmount > 0 }.sumOf { it.transactionAmount }
    }
    val totalOut = remember(category) {
        category.transactions.filter { it.transactionAmount < 0 }
            .sumOf { kotlin.math.abs(it.transactionAmount) }
    }
    val initials = category.name.trim().split(" ")
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .take(2).joinToString("").ifEmpty { category.name.take(2).uppercase() }
    val avatarColor = txAvatarColor(category.name)
    val budgetCount = category.budgets.size
    val anyLimitReached = category.budgets.any { it.limitReached }

    Row(
        modifier = modifier
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = { if (filteringOn) onToggleSelection() else onClick() }
            )
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Checkbox visible in selection/filter mode
        if (filteringOn) {
            Icon(
                painter = painterResource(
                    if (isSelected) R.drawable.check_box_filled else R.drawable.check_box_blank
                ),
                contentDescription = if (isSelected) "Deselect" else "Select",
                tint = if (isSelected) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(22.dp)
            )
        }

        // Circular avatar with initials
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .clip(CircleShape)
                    .background(avatarColor.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = initials,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 0.5.sp
                )
            }
        }

        // Middle: name + badge pills
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = category.name,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Transaction count pill
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "${category.transactions.size} txn",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                // Budget count pill (if any budgets exist)
                if (budgetCount > 0) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(
                                if (anyLimitReached)
                                    MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
                                else
                                    MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (budgetCount == 1) "1 budget" else "$budgetCount budgets",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (anyLimitReached) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.tertiary
                        )
                    }
                }
            }
        }

        // Right: lock icon (locked) or money in / money out
        if (isLocked) {
            Icon(
                painter = painterResource(R.drawable.lock),
                contentDescription = "Premium",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        } else {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = "+Ksh ${String.format("%,.0f", totalIn)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.tertiary
                )
                Text(
                    text = "-Ksh ${String.format("%,.0f", totalOut)}",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

// ─── Date range picker ────────────────────────────────────────────────────────

@Composable
fun DateRangePicker(
    premium: Boolean,
    startDate: LocalDate,
    endDate: LocalDate,
    defaultStartDate: String?,
    defaultEndDate: String?,
    onChangeStartDate: (date: LocalDate) -> Unit,
    onChangeLastDate: (date: LocalDate) -> Unit,
    onShowSubscriptionDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val defaultStartLocalDate = defaultStartDate?.let { LocalDate.parse(it) }
    val defaultEndLocalDate = defaultEndDate?.let { LocalDate.parse(it) }

    val defaultStartMillis =
        defaultStartLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()
    val defaultEndMillis =
        defaultEndLocalDate?.atStartOfDay(ZoneId.systemDefault())?.toInstant()?.toEpochMilli()

    val oneMonthAgo = LocalDateTime.now().minusMonths(1)

    @RequiresApi(Build.VERSION_CODES.O)
    fun showDatePicker(isStart: Boolean) {
        val initialDate = if (isStart) startDate else endDate
        val datePicker = DatePickerDialog(
            context,
            { _, year, month, dayOfMonth ->
                val selectedDate = LocalDate.of(year, month + 1, dayOfMonth)
                if (isStart) {
                    if (selectedDate.isBefore(endDate) || selectedDate.isEqual(endDate)) {
                        if (selectedDate.isBefore(oneMonthAgo.toLocalDate())) {
                            if (premium) onChangeStartDate(selectedDate)
                            else onShowSubscriptionDialog()
                        } else {
                            onChangeStartDate(selectedDate)
                        }
                    } else {
                        Toast.makeText(
                            context,
                            "Start date must be before end date",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                } else {
                    if (selectedDate.isAfter(startDate) || selectedDate.isEqual(startDate)) {
                        onChangeLastDate(selectedDate)
                    } else {
                        Toast.makeText(
                            context,
                            "End date must be after start date",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            initialDate.year,
            initialDate.monthValue - 1,
            initialDate.dayOfMonth
        )
        defaultStartMillis?.let { datePicker.datePicker.minDate = it }
        defaultEndMillis?.let { datePicker.datePicker.maxDate = it }
        datePicker.show()
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.onPrimary),
        elevation = CardDefaults.elevatedCardElevation(screenWidth(x = 10.0)),
        modifier = modifier
            .padding(screenWidth(x = 16.0))
            .fillMaxWidth()
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            IconButton(onClick = { showDatePicker(true) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null
                )
            }
            Text(text = dateFormatter.format(startDate))
            Text(text = "to", fontSize = screenFontSize(x = 14.0).sp)
            Text(text = dateFormatter.format(endDate), fontSize = screenFontSize(x = 14.0).sp)
            IconButton(onClick = { showDatePicker(false) }) {
                Icon(
                    tint = Color(0xFF405189),
                    painter = painterResource(id = R.drawable.calendar),
                    contentDescription = null,
                    modifier = Modifier.size(screenWidth(x = 24.0))
                )
            }
        }
    }
}

// ─── Preview ──────────────────────────────────────────────────────────────────

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
            selectedCategories = emptyList(),
            endDate = LocalDate.now().plusDays(14),
            startDate = LocalDate.now(),
            onAddCategory = {},
            onRemoveCategory = {},
            onChangeStartDate = {},
            onChangeLastDate = {},
            downloadingStatus = DownloadingStatus.INITIAL,
            onClearSearch = {},
            onChangeSearchQuery = {},
            navigateToCategoryDetailsScreen = {},
            navigateToCategoryAdditionScreen = {},
            navigateToPreviousScreen = {},
            premium = false,
            onDownloadReport = {},
            filteringOn = false,
            onFilter = {},
            showBackArrow = true,
            getCategories = {},
            onShowSubscriptionDialog = {}
        )
    }
}
