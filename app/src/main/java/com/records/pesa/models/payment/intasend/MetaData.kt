package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class MetaData(
    val id: String,
    val customer: StatusCustomerData,
    val payment_link: String?,
    val customer_comment: String?,
    val created_at: String,
    val updated_at: String
)