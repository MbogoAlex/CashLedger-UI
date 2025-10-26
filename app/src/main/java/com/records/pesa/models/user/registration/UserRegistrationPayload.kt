package com.records.pesa.models.user.registration

import kotlinx.serialization.Serializable

@Serializable
data class UserRegistrationPayload(
    val phoneNumber: String,
    val password: String,
)