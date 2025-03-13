package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class PaymentSavePayload(
    val userId: String,
    val amount: String,
    val paidAt: String,
    val expiredAt: String,
    val month: Int,
    val permanent: Boolean
)
