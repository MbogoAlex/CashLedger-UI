package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponseBody(
    val success: Boolean,
    val message: String,
    val data: UserProfileData
)
