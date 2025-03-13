package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

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
    val retry_count: Double,
    val failed_reason: String?,
    val failed_code_link: String?,
    val created_at: String,
    val updated_at: String,
)