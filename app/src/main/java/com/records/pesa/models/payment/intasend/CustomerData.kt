package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

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