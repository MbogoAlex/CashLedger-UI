package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentStatusPayload(
    val invoice_id: String
)

@Serializable
data class IntasendPaymentStatusResponseBody(
    val invoice: InvoiceData,
    val meta: MetaData
)

@Serializable
data class MetaData(
    val id: String,
    val customer: StatusCustomerData,
    val payment_link: String?,
    val customer_comment: String?,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class StatusCustomerData(
    val customer_id: String,
    val phone_number: String,
    val email: String?,
    val first_name: String?,
    val last_name: String?,
    val country: String?,
    val provider: String,
    val created_at: String,
    val updated_at: String
)

