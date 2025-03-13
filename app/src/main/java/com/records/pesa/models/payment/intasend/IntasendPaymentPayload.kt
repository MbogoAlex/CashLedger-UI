package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentPayload (
    val amount: String,
    val phone_number: String
)