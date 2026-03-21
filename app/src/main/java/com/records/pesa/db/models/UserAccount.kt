package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "userAccount", indices = [
    Index(value = ["backupUserId"], unique = true)
])
data class UserAccount(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val backupUserId: Long = 0,
    val fname: String?,
    val lname: String?,
    val email: String?,
    val phoneNumber: String,
    val password: String,
    val createdAt: LocalDateTime,
)