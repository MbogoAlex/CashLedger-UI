package com.records.pesa.models.payment.intasend

import kotlinx.serialization.Serializable

@Serializable
data class PaymentData(
    val id: String,
    val amount: String,
    val expiredAt: String?,
    val paidAt: String?,
    val month: Int,
    val userId: String,
    val freeTrialEndedOn: String?,
    val freeTrialStartedOn: String?
)
