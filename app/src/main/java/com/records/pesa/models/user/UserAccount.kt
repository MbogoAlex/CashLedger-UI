package com.records.pesa.models.user

import kotlinx.serialization.Serializable

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
    val backedUpItemsSize: Int = 0,
    val transactions: Int = 0,
    val categories: Int = 0,
    val categoryKeywords: Int = 0,
    val categoryMappings: Int = 0,
)