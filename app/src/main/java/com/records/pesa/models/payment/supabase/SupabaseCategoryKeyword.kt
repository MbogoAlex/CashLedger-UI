package com.records.pesa.models.payment.supabase

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseCategoryKeyword(
    val id: Int,
    val keyword: String,
    val nickName: String?,
    val categoryId: Int,
    val userId: Int
)
