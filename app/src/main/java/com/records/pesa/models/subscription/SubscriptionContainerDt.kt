package com.records.pesa.models.subscription

import kotlinx.serialization.Serializable

@Serializable
data class SubscriptionContainerDt(
    val id: Int,
    val title: String,
    val description: String,
    val createdAt: String,
    val updatedAt: String,
    val totalPackages: Int,
    val subscriptionPackages: List<SubscriptionPackageDt>
)
