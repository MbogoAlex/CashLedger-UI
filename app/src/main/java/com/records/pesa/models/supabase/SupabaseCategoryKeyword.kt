package com.records.pesa.models.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryKeyword(
    val id: Int,
    val keyword: String,
    val nickName: String?,
    val categoryId: Int,
    val userId: Int
)
