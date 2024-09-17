package com.records.pesa.models.payment.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionCategory(
    val id: Int,
    val createdAt: String,
    val updatedAt: String,
    val name: String,
    val contains: List<String>,
    val updatedTimes: Int?,
    val userId: Int
)

