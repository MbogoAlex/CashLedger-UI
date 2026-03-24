package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "budget_recalc_log")
data class BudgetRecalcLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val budgetId: Int,
    val budgetName: String,
    val timestamp: LocalDateTime,
    val oldExpenditure: Double,
    val newExpenditure: Double,
    val thresholdCrossed: String?
)
