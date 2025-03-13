package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserResponseBody(
    val statusCode: Int,
    val message: String,
    val data: UserDetailsData
)
