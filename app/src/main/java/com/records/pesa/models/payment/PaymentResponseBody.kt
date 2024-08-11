package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentResponseBody(
    val statusCode: Int,
    val message: String,
    val data: PaymentDt,
)
@Serializable
data class PaymentDt(
    val payment: PaymentData
)
@Serializable
data class PaymentData(
    val order_tracking_id: String,
    val merchant_reference: String,
    val redirect_url: String,
    val error: String?,
    val status: String,
    val token: String
)
