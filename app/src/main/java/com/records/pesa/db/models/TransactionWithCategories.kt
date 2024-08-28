package com.records.pesa.db.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TransactionWithCategories(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TransactionCategoryCrossRef::class)
    )
    val categories: List<TransactionCategory>
)