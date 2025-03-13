package com.records.pesa.models.user.login

import kotlinx.serialization.Serializable

@Serializable
data class UserLoginResponseBody(
    val statusCode: Int,
    val message: String,
    val data: UserLoginDt
)