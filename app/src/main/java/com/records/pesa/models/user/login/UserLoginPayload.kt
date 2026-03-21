package com.records.pesa.models.user.login

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginPayload(
    val phoneNumber: String,
    val password: String
)