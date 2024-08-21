package com.records.pesa.ui.screens.transactions.sorted

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.pullrefresh.PullRefreshIndicator
import androidx.compose.material.pullrefresh.PullRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.composables.IndividualSortedTransactionItemCell
import com.records.pesa.models.transaction.IndividualSortedTransactionItem
import com.records.pesa.reusables.LoadingStatus
import com.records.pesa.reusables.individualSortedTransactionItems
import com.records.pesa.ui.screens.utils.screenHeight
import com.records.pesa.ui.screens.utils.screenWidth
import com.records.pesa.ui.theme.CashLedgerTheme

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MoneyOutSortedScreenComposable(
    transactions: List<IndividualSortedTransactionItem>,
    pullRefreshState: PullRefreshState,
    loadingStatus: LoadingStatus,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyDirection: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
    ) {
        MoneyOutSortedScreen(
            transactions = transactions,
            loadingStatus = loadingStatus,
            pullRefreshState = pullRefreshState,
            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun MoneyOutSortedScreen(
    transactions: List<IndividualSortedTransactionItem>,
    loadingStatus: LoadingStatus,
    pullRefreshState: PullRefreshState?,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyDirection: String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                vertical = screenHeight(x = 8.0),
                horizontal = screenWidth(x = 16.0)
            )
    ) {
        if(loadingStatus == LoadingStatus.SUCCESS) {
            LazyColumn {
                items(transactions) {
                    IndividualSortedTransactionItemCell(
                        moneyIn = false,
                        transaction = it,
                        modifier = Modifier
                            .clickable {
                                navigateToEntityTransactionsScreen(it.transactionType, it.name, it.times.toString(), "out")
                            }
                    )
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
@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MoneyOutSortedScreenPreview() {
    CashLedgerTheme {
        MoneyOutSortedScreen(
            transactions = individualSortedTransactionItems,
            loadingStatus = LoadingStatus.INITIAL,
            pullRefreshState = null,
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyIn ->  }
        )
    }
}