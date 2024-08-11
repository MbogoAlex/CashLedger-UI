package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPaymentStatusPayload(
    val userId: Int,
    val orderId: String,
    val token: String
)