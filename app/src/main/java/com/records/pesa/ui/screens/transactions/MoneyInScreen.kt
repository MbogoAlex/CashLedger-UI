package com.records.pesa.ui.screens.transactions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.composables.SortedTransactionItemCell
import com.records.pesa.models.SortedTransactionItem
import com.records.pesa.reusables.moneyInSortedTransactionItems
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun MoneyInScreenComposable(
    sortedTransactionItems: List<SortedTransactionItem>,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyIn: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        MoneyInScreen(
            sortedTransactionItems = sortedTransactionItems,
            navigateToEntityTransactionsScreen = navigateToEntityTransactionsScreen
        )
    }
}

@Composable
fun MoneyInScreen(
    sortedTransactionItems: List<SortedTransactionItem>,
    navigateToEntityTransactionsScreen: (transactionType: String, entity: String, times: String, moneyIn: Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            items(sortedTransactionItems) {
                SortedTransactionItemCell(
                    transaction = it,
                    modifier = Modifier
                        .clickable {
                            navigateToEntityTransactionsScreen(it.transactionType, it.name, it.times.toString(), true)
                        }
                )
                Divider()
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MoneyInScreenPreview() {
    CashLedgerTheme {
        MoneyInScreen(
            navigateToEntityTransactionsScreen = {transactionType, entity, times, moneyIn ->  },
            sortedTransactionItems = moneyInSortedTransactionItems
        )
    }
}