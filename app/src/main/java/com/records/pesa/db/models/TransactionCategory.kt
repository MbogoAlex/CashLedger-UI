package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "transactionCategory", indices = [
    Index(value = ["createdAt"]),
    Index(value = ["updatedAt"]),
])
data class TransactionCategory(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime
)
