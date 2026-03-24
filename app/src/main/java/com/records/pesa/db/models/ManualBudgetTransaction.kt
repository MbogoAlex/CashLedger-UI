package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

@Entity(
    tableName = "manual_budget_transaction",
    indices = [Index(value = ["budgetId"])]
)
data class ManualBudgetTransaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val budgetId: Int,
    val amount: Double,
    val description: String,
    val date: LocalDate,
    val createdAt: LocalDateTime
)
