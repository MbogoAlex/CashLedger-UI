package com.records.pesa.models.payment.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionCategoryPayload(
    val createdAt: String,
    val updatedAt: String,
    val name: String,
    val contains: List<String>,
    val updatedTimes: Int?,
    val userId: Int
)

