package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileData(
    val userId: Long,
    val dynamoUserId: Long?,
    val email: String?,
    val fname: String?,
    val lname: String?,
    val phoneNumber: String,
    val permanent: Boolean,
    val roles: List<String>,
    val lastBackup: String?,
    val lastLogin: String?,
    val createdAt: String?,
    val updatedAt: String?,
)
