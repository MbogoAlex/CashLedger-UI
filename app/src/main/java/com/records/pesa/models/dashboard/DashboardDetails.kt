package com.records.pesa.models.dashboard

import com.records.pesa.models.BudgetDt
import com.records.pesa.models.TransactionCategory
import com.records.pesa.models.transaction.TransactionItem
import kotlinx.serialization.Serializable

@Serializable
data class DashboardDetailsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: DashboardDt
)

@Serializable()
data class DashboardDt(
    val transaction: DashboardTransactionDt
)

@Serializable
data class DashboardTransactionDt(
    val firstTransactionDate: String,
    val budgets: List<BudgetDt>,
    val latestTransactions: List<TransactionItem>,
    val categories: List<TransactionCategory>,
    val accountBalance: Double,
    val todayExpenditure: TodayExpenditure
)

@Serializable
data class TodayExpenditure(
    val totalIn: Double,
    val totalOut: Double
)