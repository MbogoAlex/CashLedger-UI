package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserRegistrationPayload(
    val fname: String?,
    val lname: String?,
    val email: String?,
    val phoneNumber: String,
    val password: String
)

@Serializable
data class UserRegistrationResponseBody(
    val statusCode: Int,
    val message: String,
    val data: UserRegData
)

@Serializable
data class UserRegData(
    val user: UserDetailsData
)
