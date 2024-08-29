package com.records.pesa.db.models

data class AggregatedTransaction(
    val entity: String,
    val nickName: String?,
    val transactionType: String,
    val times: Int,
    val totalAmount: Double,
    val totalCost: Double
)

