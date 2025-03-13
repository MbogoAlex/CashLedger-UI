package com.records.pesa.models.user.update

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileUpdatePayload(
    val userId: String,
    val fname: String?,
    val lname: String?,
    val email: String?,
    val phoneNumber: String?,
    val password: String?
)
