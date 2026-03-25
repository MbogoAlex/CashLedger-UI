package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "budget_member", indices = [Index(value = ["budgetId"])])
data class BudgetMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val budgetId: Int,
    val memberName: String
)
