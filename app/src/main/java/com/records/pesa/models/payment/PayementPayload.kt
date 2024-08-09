package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentPayload(
    val userId: Int,
    val phoneNumber: String
)
