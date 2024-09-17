package com.records.pesa.models.dbModel

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

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
    val darkThemeSet: Boolean = false
)
