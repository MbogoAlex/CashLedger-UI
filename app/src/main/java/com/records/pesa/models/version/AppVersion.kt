package com.records.pesa.models.version

import kotlinx.serialization.Serializable

@Serializable
data class AppVersionCheckResponseBody(
    val statusCode: Int,
    val message: String,
    val data: AppVersionCheckData
)
@Serializable
data class AppVersionCheckData(
    val version: Double
)