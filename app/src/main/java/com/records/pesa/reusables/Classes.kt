package com.records.pesa.reusables

enum class TransactionScreenTab {
    ALL_TRANSACTIONS,
    MONEY_IN,
    MONEY_OUT,
    CHART
}

data class TransactionScreenTabItem(
    val name: String,
    val icon: Int,
    val tab: TransactionScreenTab
)

enum class HomeScreenTab {
    DASHBOARD,
    CHART,
    BUDGET
}

data class HomeScreenTabItem(
    val name: String,
    val icon: Int,
    val tab: HomeScreenTab
)