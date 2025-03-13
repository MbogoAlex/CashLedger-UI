package com.records.pesa.models.user

import kotlinx.serialization.Serializable

@Serializable
data class UserDetailsData(
    val id: String,
    val createdAt: String,
    val email: String?,
    val fname: String?,
    val lname: String?,
    val phoneNumber: String,
    val password: String,
    val role: Int,
    val lastLogin: String?,
    val month: Int,
    val permanent: Boolean,
    val backupSet: Boolean,
    val lastBackup: String?,
    val backupItemsSize: String?,
    val transactions: String?,
    val categories: String?,
    val categoryKeywords: String?,
    val categoryMappings: String?
)
