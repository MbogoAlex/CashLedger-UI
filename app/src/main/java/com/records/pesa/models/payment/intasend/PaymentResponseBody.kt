package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponseBody(
    val statusCode: Int,
    val message: String,
    val data: PaymentDt,
)