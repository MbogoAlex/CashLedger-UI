package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalDateTime

/**
 * Archived record of a completed recurring budget cycle.
 * Written at the moment a cycle rolls over so the history is never lost.
 */
@Entity(tableName = "budget_cycle_log", indices = [Index(value = ["budgetId"])])
data class BudgetCycleLog(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val budgetId: Int,
    val budgetName: String,
    val cycleNumber: Int,
    val cycleStartDate: LocalDate,
    val cycleEndDate: LocalDate,
    val budgetLimit: Double,
    val finalExpenditure: Double,
    val limitReached: Boolean,
    val exceededBy: Double,
    val closedAt: LocalDateTime,
)
