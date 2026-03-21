package com.records.pesa.models.user.update

import kotlinx.serialization.Serializable

@Serializable
data class UserBackupDataUpdatePayload(
    val userId: String,
    val lastBackup: String,
    val backupItemsSize: String,
    val transactions: String,
    val categories: String,
    val categoryKeywords: String,
    val categoryMappings: String
)
