package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class PaymentPayload(
    val packageId: Int,
    val phoneNumber: String,
    val transactionMethodId: Int,
    val transactionTypeId: Int
)
