package com.records.pesa.db.models

import androidx.room.Embedded
import androidx.room.Relation

data class CategoryWithKeywords(
    @Embedded val category: TransactionCategory,
    @Relation(
        parentColumn = "id",
        entityColumn = "categoryId"
    )
    val keywords: List<CategoryKeyword>
)
