package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class TransactionResponseBody(
    val statusCode: Int,
    val message: String,
    val data: TransactionData
)

@Serializable
data class TransactionData(
    val transaction: TransactionDt
)

@Serializable
data class TransactionDt(
    val totalMoneyIn: Double,
    val totalMoneyOut: Double,
    val transactions: List<TransactionItem>
)

@Serializable
data class TransactionItem(
    val transactionId: Int,
    val transactionCode: String,
    val transactionType: String,
    val transactionAmount: Double,
    val transactionCost: Double,
    val date: String,
    val time: String,
    val sender: String,
    val nickName: String?,
    val recipient: String,
    val balance: Double,
    val categories: List<ItemCategory>
)

@Serializable
data class ItemCategory(
    val id: Int,
    val name: String
)

@Serializable
data class SortedTransactionsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: SortedTransactionsData
)

@Serializable
data class SortedTransactionsData(
    val transaction: SortedTransactionsDt
)

@Serializable
data class SortedTransactionsDt(
    val totalMoneyOut: Double?,
    val totalMoneyIn: Double?,
    val transactions: List<SortedTransactionItem>
)

@Serializable
data class SortedTransactionItem(
    val transactionType: String,
    val times: Int,
    val amount: Double,
    val transactionCost: Double,
    val name: String,
    val nickName: String?
)

@Serializable
data class CurrentBalanceResponseBody(
    val statusCode: Int,
    val message: String,
    val data: BalanceDt
)

@Serializable
data class BalanceDt(
    val balance: Double
)

@Serializable
data class TransactionEditPayload(
    val transactionId: Int,
    val userId: Int,
    val entity: String,
    val nickName: String
)

@Serializable
data class TransactionEditResponseBody(
    val statusCode: Int,
    val message: String,
    val data: TransactionEditDt
)

@Serializable
data class TransactionEditDt(
    val transaction: String
)
