package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class PaymentRequestBody(
    val packageId: Int,
    val phoneNumber: String,
    val transactionMethodId: Int,
    val transactionTypeId: Int
)
