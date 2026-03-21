package com.records.pesa.models.user.login

import kotlinx.serialization.Serializable

@Serializable
data class AuthResponseBody(
    val success: Boolean,
    val message: String,
    val data: AuthData
)

@Serializable
data class AuthData(
    val user: AuthDt
)

@Serializable
data class AuthDt(
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String?,
    val accessTokenExpiresIn: Long?,
    val refreshTokenExpiresIn: Long?,
    val userId: Long,
    val email: String?,
    val phoneNumber: String,
    val firstName: String?,
    val lastName: String?,
)