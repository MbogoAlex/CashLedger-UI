package com.records.pesa.models.payment.supabase.payload

import kotlinx.serialization.Serializable

@Serializable
data class SupabaseTransactionPayload(
    val transactionCode: String,
    val transactionType: String,
    val transactionAmount: Double,
    val transactionCost: Double,
    val date: String,
    val time: String,
    val sender: String,
    val recipient: String,
    val nickName: String?,
    val comment: String?,
    val balance: Double,
    val entity: String,
    val userId: Int
)
