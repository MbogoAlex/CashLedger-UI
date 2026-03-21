package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentStatusResponseBody(
    val statusCode: Int,
    val message: String,
    val data: IntasendPaymentStatusResponseBodyData
)