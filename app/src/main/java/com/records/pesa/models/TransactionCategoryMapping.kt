package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class TransactionCategoryMapping(
    val transactionId: Int,
    val categoryId: Int
)
