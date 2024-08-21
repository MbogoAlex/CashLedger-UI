package com.records.pesa.models.transaction

import kotlinx.serialization.Serializable

@Serializable
data class IndividualSortedTransactionsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: IndividualSortedTransactionData
)

@Serializable
data class IndividualSortedTransactionData(
    val transaction: IndividualSortedTransactionDataTransaction
)

@Serializable
data class IndividualSortedTransactionDataTransaction(
    val totalMoneyIn: Double,
    val totalMoneyOut: Double,
    val transactions: List<IndividualSortedTransactionItem>
)

@Serializable
data class IndividualSortedTransactionItem(
    val transactionType: String,
    val times: Int,
    val amount: Double,
    val nickName: String?,
    val name: String,
    val transactionCost: Double
)