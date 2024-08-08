package com.records.pesa.ui.screens.transactions

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.records.pesa.composables.TransactionItemCell
import com.records.pesa.models.transaction.TransactionItem
import com.records.pesa.reusables.transactions
import com.records.pesa.ui.theme.CashLedgerTheme

@Composable
fun AllTransactionsScreenComposable(
    transactions: List<TransactionItem>,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier) {
        AllTransactionsScreen(
            transactions = transactions
        )
    }
}

@Composable
fun AllTransactionsScreen(
    transactions: List<TransactionItem>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
    ) {
        LazyColumn {
            items(transactions) {
                TransactionItemCell(transaction = it)
                Divider()
            }
        }

    }
}

@Preview(showBackground = true)
@Composable
fun AllTransactionsScreenPreview() {
    CashLedgerTheme {
        AllTransactionsScreen(
            transactions = transactions
        )
    }
}