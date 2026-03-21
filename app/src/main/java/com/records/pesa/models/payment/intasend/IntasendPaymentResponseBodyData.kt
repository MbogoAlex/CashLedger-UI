package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentResponseBodyData(
    val id: String,
    val invoice: InvoiceData,
    val customer: CustomerData,
    val payment_link: String?,
    val customer_comment: String?,
    val refundable: Boolean,
    val created_at: String,
    val updated_at: String
)