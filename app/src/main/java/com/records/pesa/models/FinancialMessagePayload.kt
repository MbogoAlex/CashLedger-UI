package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class FinancialMessagePayload (
    val message: String,
    val sender: String,
    val receiver: String,
    val timestamp: String,
    val messageType: String,
    val bank: String?,
    val carrierName: String,
    val receivingPhoneNumber: String,
    val userId: Long
)