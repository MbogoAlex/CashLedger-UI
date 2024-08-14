package com.records.pesa.models.transaction

import kotlinx.serialization.Serializable

@Serializable
data class SingleTransactionResponseBody(
    val statusCode: Int,
    val message: String,
    val data: SingleTransactionData
)

@Serializable
data class SingleTransactionData(
    val transaction: TransactionItem
)

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
    val transactionId: Int?,
    val transactionCode: String,
    val transactionType: String,
    val transactionAmount: Double,
    val transactionCost: Double,
    val date: String,
    val time: String,
    val sender: String,
    val nickName: String?,
    val recipient: String,
    val entity: String,
    val balance: Double,
    val comment: String?,
    val categories: List<ItemCategory>?
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
    val timesOut: Int,
    val timesIn: Int,
    val totalOut: Double,
    val totalIn: Double,
    val transactionCost: Double,
    val entity: String,
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
    val nickName: String?,
    val comment: String?
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

@Serializable
data class GroupedTransactionsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: GroupedTransactionsDt
)

@Serializable
data class GroupedTransactionsDt(
    val transaction: GroupedTransactionsOverview
)

@Serializable
data class GroupedTransactionsOverview(
    val totalMoneyIn: Double,
    val totalMoneyOut: Double,
    val transactions: List<GroupedTransactionData>
)
@Serializable
data class GroupedTransactionData(
    val date: String,
    val times: Int,
    val moneyIn: Float,
    val moneyOut: Float,
    val transactionCost: Float
)

@Serializable
data class TransactionCodesResponseBody(
    val statusCode: Int,
    val message: String,
    val data: TransactionCodesDt

)

@Serializable
data class TransactionCodesDt(
    val transaction: List<String>
)
