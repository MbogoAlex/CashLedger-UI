package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionTransactionResponseBody(
    val success: Boolean,
    val message: String,
    val data: SubscriptionTransactionDt?
)
