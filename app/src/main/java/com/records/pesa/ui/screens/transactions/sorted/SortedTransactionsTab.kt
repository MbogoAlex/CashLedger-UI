package com.records.pesa.ui.screens.transactions.sorted

enum class SortedTransactionsTab {
    MONEY_IN,
    MONEY_OUT
}

data class SortedTransactionsTabItem(
    val name: String,
    val icon: Int,
    val tab: SortedTransactionsTab
)