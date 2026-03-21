package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMessagesResponseBody (
    val message: String,
    val success: Boolean
)