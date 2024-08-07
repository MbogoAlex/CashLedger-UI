package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginPayload(
    val phoneNumber: String,
    val password: String
)

@Serializable
data class UserLoginResponseBody(
    val statusCode: Int,
    val message: String,
    val data: UserLoginDt
)

@Serializable
data class UserLoginDt(
    val user: UserInfoDt,
    val token: String
)

@Serializable
data class UserInfoDt(
    val userInfo: UserDetailsData
)