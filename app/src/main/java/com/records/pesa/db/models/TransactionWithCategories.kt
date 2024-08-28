package com.records.pesa.db.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class TransactionWithCategories(
    @Embedded val transaction: Transaction,
    @Relation(
        parentColumn = "id",
        entityColumn = "id", // This should match the primary key column in TransactionCategory
        associateBy = Junction(TransactionCategoryCrossRef::class,
            parentColumn = "transactionId",
            entityColumn = "categoryId") // Explicitly mapping the cross-reference
    )
    val categories: List<TransactionCategory>
)
