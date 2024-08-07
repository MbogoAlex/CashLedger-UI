package com.records.pesa.models.dbModel

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "user", [Index(value = ["userId"], unique = true)])
data class UserDetails(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val userId: Int,
    val firstName: String?,
    val lastName: String?,
    val email: String?,
    val phoneNumber: String,
    val password: String,
    val paymentStatus: Boolean
)
