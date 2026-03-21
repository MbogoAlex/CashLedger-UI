package com.records.pesa.models.user.update

import kotlinx.serialization.Serializable

@Serializable
data class PasswordResetPayload(
    val phoneNumber: String,
    val password: String
)
