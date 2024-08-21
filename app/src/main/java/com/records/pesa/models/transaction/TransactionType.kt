package com.records.pesa.models.transaction

import kotlinx.serialization.Serializable

@Serializable
data class TransactionTypeResponseBody(
    val statusCode: Int,
    val message: String,
    val data: TransactionTypeResponseBodyData
)

@Serializable
data class TransactionTypeResponseBodyData(
    val transaction: TransactionTypeResponseBodyDataTransaction
)

@Serializable
data class TransactionTypeResponseBodyDataTransaction(
    val transactions: List<TransactionTypeItem>
)

@Serializable
data class TransactionTypeItem(
    val transactionType: String,
    val amount: Double,
    val amountSign: String
)