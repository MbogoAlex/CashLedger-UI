package com.records.pesa.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AllTransactionsScreenComposable(
    pullRefreshState: PullRefreshState,
    transactions: List<TransactionItem>,
    loadingStatus: LoadingStatus,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AllTransactionsScreen(
            transactions = transactions,
            pullRefreshState = pullRefreshState,
            loadingStatus = loadingStatus,
            navigateToTransactionDetailsScreen = navigateToTransactionDetailsScreen
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun AllTransactionsScreen(
    transactions: List<TransactionItem>,
    pullRefreshState: PullRefreshState?,
    loadingStatus: LoadingStatus,
    navigateToTransactionDetailsScreen: (transactionId: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        if(loadingStatus != LoadingStatus.LOADING) {
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
@Preview(showBackground = true)
@Composable
fun AllTransactionsScreenPreview() {
    CashLedgerTheme {
        AllTransactionsScreen(
            transactions = transactions,
            pullRefreshState = null,
            loadingStatus = LoadingStatus.INITIAL,
            navigateToTransactionDetailsScreen = {}
        )
    }
}