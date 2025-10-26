package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionTransactionDt(
    val id: Long,
    val transactionRef: String,
    val amount: Double,
    val transactionReason: String,
    val status: String,
    val createdAt: String,
    val updatedAt: String,
    val failedReason: String?
)
