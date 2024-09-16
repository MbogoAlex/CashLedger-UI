package com.records.pesa.models.payment.supabase

import kotlinx.serialization.Serializable

@Serializable
data class PaymentData(
    val id: Int,
    val amount: Double,
    val expiredAt: String?,
    val paidAt: String?,
    val month: String?,
    val userId: Int,
    val freeTrialEndedOn: String,
    val freeTrialStartedOn: String
)
