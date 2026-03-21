package com.records.pesa.models.payment

import kotlinx.serialization.Serializable

@Serializable
data class FreeTrialStatusResponseBody(
    val statusCode: Int,
    val message: String,
    val data: FreeTrialStatus
)
@Serializable
data class FreeTrialStatus(
    val days: Int
)
