package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPaymentDt(
    val paymentId: Long,
    val transactionId: Long,
    val userId: Long,
    val name: String?,
    val senderAccount: String?,
    val amount: Double,
    val method: String?,
    val paidAt: String,
    val expiredAt: String?,
    val paidAtMonth: Int?,
    val subscriptionPackageId: Int?,
    val subscriptionPackageName: String?

)
