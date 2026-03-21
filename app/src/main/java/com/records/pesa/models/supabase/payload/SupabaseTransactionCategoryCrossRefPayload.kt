package com.records.pesa.models.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionCategoryCrossRefPayload(
    val transactionId: Int,
    val categoryId: Int,
    val userId: Int
)
