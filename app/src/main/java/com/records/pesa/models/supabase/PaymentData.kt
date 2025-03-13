package com.records.pesa.models.supabase

import kotlinx.serialization.Serializable

@Serializable
data class PaymentData(
    val id: Int? = null,
    val amount: Double?,
    val expiredAt: String?,
    val paidAt: String?,
    val month: Int?,
    val userId: Int,
    val freeTrialEndedOn: String? = null,
    val freeTrialStartedOn: String? = null
)
