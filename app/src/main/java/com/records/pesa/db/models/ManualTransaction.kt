package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity(tableName = "manual_transaction", indices = [Index(value = ["categoryId"])])
data class ManualTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val memberName: String,
    val transactionTypeName: String,
    val isOutflow: Boolean,
    val amount: Double,
    val description: String = "",
    val date: LocalDate,
    val time: LocalTime? = null,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null
)
