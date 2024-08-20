package com.records.pesa.ui.screens.transactions

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.clickable
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
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material.pullrefresh.rememberPullRefreshState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.records.pesa.AppViewModelFactory
import com.records.pesa.R
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.functions.formatMoneyValue
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.nav.AppNavigation
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.TransactionScreenTab
import com.records.pesa.reusables.dateFormatter
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.screens.utils.screenFontSize
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme
import java.time.LocalDate
import java.time.LocalDateTime

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
    val moneyIn: String = "moneyIn"
    val routeWithArgs: String = "$route/{$userId}/{$transactionType}/{$entity}/{$startDate}/{$endDate}/{$times}/{$moneyIn}"
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

    val createDocumentLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument()) { uri: Uri? ->
        uri?.let {
            viewModel.fetchReportAndSave(
                context = context,
                saveUri = it
            )
        }
    }


    if(uiState.downloadingStatus == DownloadingStatus.SUCCESS) {
        Toast.makeText(context, "Report downloaded in your selected folder", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
        val uri = uiState.downLoadUri
        val pdfIntent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(Intent.createChooser(pdfIntent, "Open PDF with:"))
    } else if(uiState.downloadingStatus == DownloadingStatus.FAIL) {
        Toast.makeText(context, "Failed to download report. Check your connection", Toast.LENGTH_SHORT).show()
        viewModel.resetDownloadingStatus()
    }

    Box(
        modifier = Modifier
            .safeDrawingPadding()
    ) {
        SingleEntityTransactionsScreen(
            pullRefreshState = pullRefreshState,
            transactions = uiState.transactions,
            startDate = uiState.startDate,
            endDate = uiState.endDate,
            totalMoneyIn = formatMoneyValue(uiState.totalMoneyIn),
            totalMoneyOut = formatMoneyValue(uiState.totalMoneyOut),
            downloadingStatus = uiState.downloadingStatus,
            onDownloadReport = {
                createDocumentLauncher.launch("MPESA-Transactions_${LocalDateTime.now()}.pdf")
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
    totalMoneyIn: String,
    totalMoneyOut: String,
    downloadingStatus: DownloadingStatus,
    onDownloadReport: () -> Unit,
    loadingStatus: LoadingStatus,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    navigateToPreviousScreen: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .padding(
                horizontal = screenWidth(x = 16.0)
            )
            .fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
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
            TextButton(
                enabled = downloadingStatus != DownloadingStatus.LOADING,
                onClick = onDownloadReport
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Statement",
                        fontSize = screenFontSize(x = 14.0).sp
                    )
                    if(downloadingStatus == DownloadingStatus.LOADING) {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(screenWidth(x = 15.0))
                        )
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.download),
                            contentDescription = null,
                            modifier = Modifier
                                .size(screenWidth(x = 24.0))
                        )
                    }

                }
            }
        }
        Spacer(modifier = Modifier.height(screenHeight(x = 10.0)))
        Text(
            text = "From ${dateFormatter.format(LocalDate.parse(startDate))} to ${dateFormatter.format(LocalDate.parse(endDate))}",
            fontWeight = FontWeight.Bold,
            fontSize = screenFontSize(x = 14.0).sp
        )
        Spacer(modifier = Modifier.height(screenHeight(x = 20.0)))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .padding(
                    horizontal = screenWidth(x = 16.0)
                )
        ) {
            Icon(
                painter = painterResource(id = R.drawable.arrow_downward),
                contentDescription = null,
                modifier = Modifier
                    .size(screenWidth(x = 24.0))
            )
            Text(
                text = totalMoneyIn,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.surfaceTint
            )
            Spacer(modifier = Modifier.weight(1f))
            Icon(
                painter = painterResource(id = R.drawable.arrow_upward),
                contentDescription = null,
                modifier = Modifier
                    .size(screenWidth(x = 24.0))
            )
            Text(
                text = totalMoneyOut,
                fontWeight = FontWeight.Bold,
                fontSize = screenFontSize(x = 14.0).sp,
                color = MaterialTheme.colorScheme.error
            )
        }
        LazyColumn {
            items(transactions) {
                TransactionItemCell(
                    transaction = it,
                    modifier = Modifier
                        .clickable {
                            navigateToTransactionDetailsScreen(it.transactionId.toString())
                        }
                )
                Divider()
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
            totalMoneyIn = "Ksh 1000",
            totalMoneyOut = "Ksh 500",
            startDate = "2023-03-06",
            endDate = "2024-06-25",
            downloadingStatus = DownloadingStatus.INITIAL,
            onDownloadReport = {},
            navigateToTransactionDetailsScreen = {},
            navigateToPreviousScreen = {}
        )
    }
}