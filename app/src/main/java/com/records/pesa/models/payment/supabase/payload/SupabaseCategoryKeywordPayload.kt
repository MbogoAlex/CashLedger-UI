package com.records.pesa.models.payment.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryKeywordPayload(
    val keyword: String,
    val nickName: String?,
    val categoryId: Int,
    val userId: Int
)
