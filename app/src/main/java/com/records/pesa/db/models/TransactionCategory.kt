package com.records.pesa.db.models

import androidx.room.ColumnInfo
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
    @ColumnInfo(name = "id")
    val id: Int = 0,
    var name: String,
    var createdAt: LocalDateTime,
    var updatedAt: LocalDateTime,
    var updatedTimes: Double?
)
