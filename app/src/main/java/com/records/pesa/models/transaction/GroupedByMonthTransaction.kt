package com.records.pesa.models.transaction

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyTransactionsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: MonthlyTransactionDt
)
@Serializable
data class MonthlyTransactionDt(
    val transaction: MonthlyTransactionData,
)

@Serializable
data class MonthlyTransactionData(
    val totalMoneyIn: Double,
    val totalMoneyOut: Double,
    val transactions: List<MonthlyTransaction>
)

@Serializable
data class MonthlyTransaction(
    val date: String,
    val times: Int,
    val month: String,
    val year: String,
    val moneyIn: Double,
    val transactionCost: Double,
    val moneyOut: Double
)