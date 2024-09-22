package com.records.pesa.models.payment.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionCategoryCrossRefPayload(
    val transactionId: Int,
    val categoryId: Int,
    val userId: Int
)
