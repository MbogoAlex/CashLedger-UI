package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentDetails(
    val id: Int,
    val month: String,
    val paidAt: String,
    val expiredAt: String,
    val userId: Int,
    val sessionExpired: Boolean
)
