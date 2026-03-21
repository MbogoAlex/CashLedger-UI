package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "deletedTransactions")
data class DeletedTransaction(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val entity: String
)
