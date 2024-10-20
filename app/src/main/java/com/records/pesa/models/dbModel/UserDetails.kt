package com.records.pesa.models.dbModel

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "user", [Index(value = ["userId"], unique = true)])
data class UserDetails(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int = 0,
    val firstName: String? = null,
    val lastName: String? = null,
    val email: String? = null,
    val phoneNumber: String = "",
    val password: String = "",
    val token: String = "",
    val paymentStatus: Boolean = false,
    val paidAt: String? = null,
    val expiredAt: String? = null,
    val supabaseLogin: Boolean = false,
    val permanent: Boolean = false,
    val backupSet: Boolean = false,
    val backupWorkerInitiated: Boolean = false,
    val lastBackup: LocalDateTime? = null,
    val backedUpItemsSize: Int = 0,
    val transactions: Int = 0,
    val categories: Int = 0,
    val categoryKeywords: Int = 0,
    val categoryMappings: Int = 0,
    val darkThemeSet: Boolean = false
)
