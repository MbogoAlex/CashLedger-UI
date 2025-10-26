package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserInfoDt(
    val userInfo: UserProfileData,
    val token: String
)