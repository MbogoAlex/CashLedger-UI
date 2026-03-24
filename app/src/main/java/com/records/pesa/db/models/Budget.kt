package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(tableName = "budget", indices = [Index(value = ["categoryId"])])
data class Budget(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val active: Boolean,
    val expenditure: Double,
    val budgetLimit: Double,
    val createdAt: LocalDateTime,
    val startDate: LocalDate,
    val limitDate: LocalDate,
    val limitReached: Boolean,
    val limitReachedAt: LocalDateTime?,
    val exceededBy: Double,
    val categoryId: Int?,
    val alertThreshold: Int = 80
)
