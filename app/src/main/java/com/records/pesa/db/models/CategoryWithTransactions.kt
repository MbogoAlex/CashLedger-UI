package com.records.pesa.db.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CategoryWithTransactions(
    @Embedded val category: TransactionCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TransactionCategoryCrossRef::class)
    )
    val transactions: List<Transaction>
)