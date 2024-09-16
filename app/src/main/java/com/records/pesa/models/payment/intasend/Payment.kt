package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class IntasendPaymentPayload (
    val amount: String,
    val phone_number: String
)

@Serializable
data class IntasendPaymentResponseBody(
    val id: String,
    val invoice: InvoiceData,
    val customer: CustomerData,
    val payment_link: String?,
    val customer_comment: String?,
    val refundable: Boolean,
    val created_at: String,
    val updated_at: String
)

@Serializable
data class InvoiceData(
    val invoice_id: String,
    val state: String,
    val provider: String,
    val charges: Double,
    val net_amount: String,
    val currency: String,
    val value: Double,
    val account: String,
    val api_ref: String,
    val mpesa_reference: String?,
    val host: String,
    val card_info: CardInfo,
    val retry_count: Int,
    val failed_reason: String?,
    val failed_code_link: String?,
    val created_at: String,
    val updated_at: String,
)

@Serializable
data class CardInfo(
    val bin_country: String?,
    val card_type: String?
)

@Serializable
data class CustomerData(
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

