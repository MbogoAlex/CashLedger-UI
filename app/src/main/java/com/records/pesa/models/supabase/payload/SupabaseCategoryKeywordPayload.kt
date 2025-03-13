package com.records.pesa.models.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryKeywordPayload(
    val keyword: String,
    val nickName: String?,
    val categoryId: Int,
    val userId: Int
)
