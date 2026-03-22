package com.records.pesa.ui.screens.transactions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.components.TxDateHeader
import com.records.pesa.ui.screens.components.TxEmptyState
import com.records.pesa.ui.screens.components.TxItemRow
import com.records.pesa.ui.screens.components.formatTxDateHeader
import com.records.pesa.ui.screens.components.formatTxShortDate
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDateTime
import kotlin.math.absoluteValue

object SingleEntityTransactionsScreenDestination: AppNavigation {
    override val title: String = "Single entity transactions screen"
    override val route: String = "single-entity-transactions-screen"
    val userId: String = "userId"
    val transactionType: String = "transactionType"
    val entity: String = "entity"
    val categoryId: String = "categoryId"
    val budgetId: String = "categoryId"
    val startDate: String = "startDate"
    val endDate: String = "endDate"
    val times: String = "times"
    val moneyDirection: String = "moneyDirection"
    val routeWithArgs: String = "$route/{$userId}/{$transactionType}/{$entity}/{$startDate}/{$endDate}/{$times}/{$moneyDirection}"
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SingleEntityTransactionsScreenComposable(
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    navigateToLoginScreenWithArgs: (phoneNumber: String, password: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val viewModel: SingleEntityTransactionsScreenViewModel = viewModel(factory = AppViewModelFactory.Factory)
    val uiState by viewModel.uiState.collectAsState()

    if(uiState.errorCode == 401 && uiState.loadingStatus == LoadingStatus.FAIL) {
        navigateToLoginScreenWithArgs(uiState.userDetails.phoneNumber, uiState.userDetails.phoneNumber)
        viewModel.resetLoadingStatus()
    }

    val pullRefreshState = rememberPullRefreshState(
        refreshing = uiState.loadingStatus == LoadingStatus.LOADING,
        onRefresh = {
            viewModel.getTransactions()
        }
    )

    var showDownloadReportDialog by rememberSaveable {
        mutableStateOf(false)
    }

    var reportType by rememberSaveable {
        mutableStateOf("PDF")
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.fetchReportAndSave(
                context = context,
                saveUri = it,
                reportType = reportType
            )
        }
    }

    if(showDownloadReportDialog) {
        DownloadReportDialog(
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            onDismiss = {
                showDownloadReportDialog = !showDownloadReportDialog
            },
            onConfirm = { type ->
                reportType = type
                showDownloadReportDialog = !showDownloadReportDialog
                if(reportType == "PDF") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.pdf")
                } else if(reportType == "CSV") {
                    createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.csv")
                }
            }
        )
    }

    if(uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        if(reportType == "PDF") {
            val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))
        } else if(reportType == "CSV") {
            val csvIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "text/csv")
                flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(Intent.createChooser(csvIntent, "Open CSV with:"))
        }
    } else if(uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    Box(
        modifier = Modifier.safeDrawingPadding()
    ) {
        SingleEntityTransactionsScreen(
            pullRefreshState = pullRefreshState,
            transactions = uiState.transactions,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            totalMoneyIn = uiState.totalMoneyIn,
            totalMoneyOut = uiState.totalMoneyOut,
            downloadingStatus = uiState.downloadingStatus,
            onDownloadReport = {
                showDownloadReportDialog = !showDownloadReportDialog
            },
            loadingStatus = uiState.loadingStatus,
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen,
            navigateToPreviousScreen = navigateToPreviousScreen
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun SingleEntityTransactionsScreen(
    pullRefreshState: PullRefreshState?,
    transactions: List<TransactionItem>,
    startDate: String,
    endDate: String,
    totalMoneyIn: Double,
    totalMoneyOut: Double,
    downloadingStatus: DownloadingStatus,
    onDownloadReport: () -> Unit,
    loadingStatus: LoadingStatus,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLoading = loadingStatus == LoadingStatus.LOADING
    val net = totalMoneyIn - totalMoneyOut
    val primaryColor = MaterialTheme.colorScheme.primary

    val groupedByDate = remember(transactions) {
        transactions.groupBy { it.date }
            .entries.sortedByDescending { it.key }
    }

    val lazyListState = rememberLazyListState()
    var stickyDate by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(lazyListState.firstVisibleItemIndex) {
        val visibleItems = lazyListState.layoutInfo.visibleItemsInfo
        val dateHeaders = visibleItems.filter {
            (it.key as? String)?.startsWith("header_") == true
        }
        val onScreenHeader = dateHeaders.firstOrNull { it.offset >= 0 }
        if (onScreenHeader != null) {
            stickyDate = null
        } else {
            dateHeaders.maxByOrNull { it.index }?.let {
                stickyDate = (it.key as? String)?.removePrefix("header_")
            }
        }
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
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 4.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = navigateToPreviousScreen) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_right),
                            contentDescription = "Back",
                            modifier = Modifier
                                .size(22.dp)
                                .scale(scaleX = -1f, scaleY = 1f)
                        )
                    }
                    Text(
                        text = "Transactions",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(start = 4.dp)
                    )
                    IconButton(
                        onClick = onDownloadReport,
                        enabled = downloadingStatus != DownloadingStatus.LOADING
                    ) {
                        if (downloadingStatus == DownloadingStatus.LOADING) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(
                                painter = painterResource(R.drawable.download),
                                contentDescription = "Download statement",
                                modifier = Modifier.size(20.dp),
                                tint = primaryColor
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.08f),
                thickness = 1.dp
            )

            // ── Scrollable content ────────────────────────────────────────────
            Box(modifier = Modifier.weight(1f)) {
                LazyColumn(
                    state = lazyListState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 120.dp)
                ) {
                    // Hero summary card
                    item {
                        ElevatedCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 12.dp)
                                .shadow(elevation = 4.dp, shape = RoundedCornerShape(20.dp), spotColor = primaryColor.copy(alpha = 0.15f)),
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
                                    // Period label
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "From ${formatTxShortDate(startDate)} to ${formatTxShortDate(endDate)}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${transactions.size} txn${if (transactions.size != 1) "s" else ""}",
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(14.dp))

                                    // Net flow
                                    Text(
                                        text = "Net Flow",
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium,
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${if (net >= 0) "+" else "-"}Ksh ${String.format("%,.2f", net.absoluteValue)}",
                                        fontSize = 28.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (net >= 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.error
                                    )

                                    Spacer(modifier = Modifier.height(14.dp))
                                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.10f))
                                    Spacer(modifier = Modifier.height(12.dp))

                                    // In / Out row
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Column {
                                            Text("Money In", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text("Ksh ${String.format("%,.2f", totalMoneyIn)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                        Column(horizontalAlignment = Alignment.End) {
                                            Text("Money Out", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), fontWeight = FontWeight.Medium, letterSpacing = 0.3.sp)
                                            Spacer(modifier = Modifier.height(3.dp))
                                            Text("Ksh ${String.format("%,.2f", totalMoneyOut)}", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // Transactions grouped by date
                    if (!isLoading) {
                        if (transactions.isEmpty()) {
                            item {
                                TxEmptyState(message = "No transactions for this period")
                            }
                        } else {
                            groupedByDate.forEach { (date, txsForDate) ->
                                item(key = "header_$date") {
                                    TxDateHeader(date = date)
                                }
                                items(txsForDate, key = { it.transactionId ?: it.transactionCode }) { tx ->
                                    TxItemRow(
                                        transaction = tx,
                                        onClick = { navigateToTransactionDetailsScreen(tx.transactionId.toString()) },
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
                }

                // Floating date banner
                if (stickyDate != null) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopStart)
                            .zIndex(1f),
                        color = MaterialTheme.colorScheme.background,
                        shadowElevation = 2.dp
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.22f))
                                .padding(horizontal = 16.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.calendar),
                                contentDescription = null,
                                modifier = Modifier.size(11.dp),
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                            )
                            Text(
                                text = formatTxDateHeader(stickyDate!!),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f),
                                letterSpacing = 0.3.sp
                            )
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
                        contentColor = primaryColor
                    )
                }

                // Full-screen loading
                if (isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(
                            color = primaryColor,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DownloadReportDialog(
    startDate: String,
    endDate: String,
    onDismiss: () -> Unit,
    onConfirm: (type: String) -> Unit,
) {
    val types = listOf("PDF", "CSV")
    var selectedType by rememberSaveable {
        mutableStateOf("PDF")
    }

    var expanded by rememberSaveable {
        mutableStateOf(false)
    }

    AlertDialog(
        title = {
            Text(
                text = "Report for $startDate to $endDate",
                fontSize = screenFontSize(x = 14.0).sp
            )
        },
        text = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                Text(
                    text = "Export to ",
                    fontSize = screenFontSize(x = 14.0).sp
                )
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clickable {
                                expanded = !expanded
                            }
                    ) {
                        Text(
                            text = selectedType,
                            color = MaterialTheme.colorScheme.surfaceTint,
                            fontSize = screenFontSize(x = 14.0).sp
                        )
                        Icon(
                            tint = MaterialTheme.colorScheme.surfaceTint,
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Select report type",
                            modifier = Modifier.size(screenFontSize(x = 24.0).dp)
                        )
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = {expanded = !expanded},
                        modifier = Modifier
                    ) {
                        types.forEach {
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = it,
                                        fontSize = screenFontSize(x = 14.0).sp
                                    )
                                },
                                onClick = {
                                    selectedType = it
                                    expanded = !expanded
                                }
                            )
                        }
                    }
                }
            }
        },
        onDismissRequest = onDismiss,
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Cancel",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(selectedType) }) {
                Text(
                    text = "Confirm",
                    fontSize = screenFontSize(x = 14.0).sp
                )
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun SingleEntityTransactionsScreenPreview(
    modifier: Modifier = Modifier
) {
    CashLedgerTheme {
        SingleEntityTransactionsScreen(
            pullRefreshState = null,
            loadingStatus = LoadingStatus.INITIAL,
            transactions = transactions,
            totalMoneyIn = 1000.0,
            totalMoneyOut = 500.0,
            startDate = "2023-03-06",
            endDate = "2024-06-25",
            downloadingStatus = DownloadingStatus.INITIAL,
            onDownloadReport = {},
            navigateToTransactionDetailsScreen = {},
            navigateToPreviousScreen = {}
        )
    }
}
