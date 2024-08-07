package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionDetails(
    val id: Int,
    val month: String,
    val paidAt: String,
    val expiredAt: String,
    val userId: Int,
    val sessionExpired: Boolean
)

@Serializable
data class SubscriptionStatusResponseBody(
    val statusCode: Int,
    val message: String,
    val data: SubscriptionStatus
)

@Serializable
data class SubscriptionStatus(
    val payment: Boolean
)


