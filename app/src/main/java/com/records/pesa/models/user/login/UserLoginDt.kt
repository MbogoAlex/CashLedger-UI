package com.records.pesa.models.user.login

import com.records.pesa.models.user.UserInfoDt
import kotlinx.serialization.Serializable

@Serializable
data class UserLoginDt(
    val user: UserInfoDt,
)