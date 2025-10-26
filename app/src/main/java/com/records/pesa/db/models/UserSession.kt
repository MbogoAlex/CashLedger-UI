package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "userSession")
data class UserSession(
    @PrimaryKey val id: Int = 1, // Single row table - always use ID = 1
    val userId: Long?,
    val accessToken: String?,
    val refreshToken: String?,
    val tokenType: String?,
    val accessTokenExpiresIn: Long?,
    val refreshTokenExpiresIn: Long?
)
