package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionPackageDt(
    val id: Int,
    val title: String,
    val description: String?,
    val amount: Double,
    val createdAt: String,
    val updatedAt: String,
    val subscriptionContainerId: Int,
    val subscriptionContainerTitle: String,
    val payments: Int
)
