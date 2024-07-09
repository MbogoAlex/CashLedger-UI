package com.records.pesa.ui.screens.transactions

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
import com.records.pesa.reusables.moneyOutSortedTransactionItems
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun MoneyOutScreenComposable(
    sortedTransactionItems: List<SortedTransactionItem>,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .safeDrawingPadding()
    ) {
        MoneyInScreen(sortedTransactionItems = sortedTransactionItems)
    }
}

@Composable
fun MoneyOutScreen(
    sortedTransactionItems: List<SortedTransactionItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            items(sortedTransactionItems) {
                SortedTransactionItemCell(transaction = it)
                Divider()
            }
        }
    }
}



@Preview(showBackground = true, showSystemUi = true)
@Composable
fun MoneyOutScreenPreview() {
    CashLedgerTheme {
        MoneyOutScreen(
            sortedTransactionItems = moneyOutSortedTransactionItems
        )
    }
}