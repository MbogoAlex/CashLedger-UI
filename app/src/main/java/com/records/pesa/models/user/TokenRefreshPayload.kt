package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class TokenRefreshPayload(
    val refreshToken: String
)
