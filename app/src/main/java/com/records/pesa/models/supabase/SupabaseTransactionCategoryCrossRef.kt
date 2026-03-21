package com.records.pesa.models.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionCategoryCrossRef(
    val id: Int,
    val transactionId: Int,
    val categoryId: Int,
    val userId: Int
)
