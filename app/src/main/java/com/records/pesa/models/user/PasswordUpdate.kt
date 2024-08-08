package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class PasswordUpdatePayload(
    val phoneNumber: String,
    val newPassword: String
)