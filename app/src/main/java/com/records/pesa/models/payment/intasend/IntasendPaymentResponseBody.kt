package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentResponseBody(
    val statusCode: Int,
    val message: String,
    val data: IntasendPaymentResponseBodyData
)