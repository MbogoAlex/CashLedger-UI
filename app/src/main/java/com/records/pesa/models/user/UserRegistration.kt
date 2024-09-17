package com.records.pesa.models.user

import kotlinx.serialization.Serializable
import java.time.LocalDateTime

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

@Serializable
data class UserAccount(
    val id: Int? = null,
    val email: String? = null,
    val fname: String? = null,
    val lname: String? = null,
    val phoneNumber: String,
    val password: String,
    val createdAt: String? = null,
    val month: Int,
    val lastLogin: String? = null,
    val role: Int,
    val permanent: Boolean = false,
    val backupSet: Boolean = false,
    val lastBackup: String? = null,
)
