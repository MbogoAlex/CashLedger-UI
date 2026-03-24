package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "manual_transaction_type")
data class ManualTransactionType(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val isOutflow: Boolean,
    val isCustom: Boolean = false,
    val createdAt: LocalDateTime
)
