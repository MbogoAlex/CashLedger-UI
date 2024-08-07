package com.records.pesa.models.dbModel

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "app_launch_state")
data class AppLaunchStatus(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val launched: Int = 0,
    val user_id: Int? = 0,
)
