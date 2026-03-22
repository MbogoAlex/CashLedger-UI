package com.records.pesa.db.models

/**
 * Data class for transaction type breakdown query results
 */
data class TransactionTypeData(
    val transactionType: String,
    val count: Int,
    val total: Double
)
