package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "userPreferences")
data class UserPreferences(
    @PrimaryKey val id: Int = 0, // Single row for settings
    val loggedIn: Boolean,
    val darkMode: Boolean,
    val paid: Boolean,
    val permanent: Boolean,
    val paidAt: LocalDateTime?,
    val expiryDate: LocalDateTime?
)
