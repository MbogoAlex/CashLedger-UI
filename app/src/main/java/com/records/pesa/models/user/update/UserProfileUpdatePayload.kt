package com.records.pesa.models.user.update

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileUpdatePayload(
    val fname: String?,
    val lname: String?,
    val email: String?
)
