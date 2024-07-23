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

enum class BudgetHomeScreenTab {
    INFO,
    MONEY_IN,
    MONEY_OUT,
    CHART
}

data class BudgetHomeScreenTabItem(
    val name: String,
    val icon: Int,
    val tab: BudgetHomeScreenTab
)

enum class LoadingStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    FAIL
}

enum class ExecutionStatus {
    INITIAL,
    LOADING,
    SUCCESS,
    FAIL
}