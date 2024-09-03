package com.records.pesa.db.models

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation

data class CategoryWithTransactions(
    @Embedded val category: TransactionCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(TransactionCategoryCrossRef::class,
            parentColumn = "categoryId",
            entityColumn = "transactionId"
        )
    )
    val transactions: List<Transaction>,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val budgets: List<Budget>,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val keyWords: List<CategoryKeyword>
)