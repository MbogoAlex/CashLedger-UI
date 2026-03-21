package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class PaymentsResponseBody(
    val statusCode: Int,
    val message: String,
    val data: List<PaymentData>
)
