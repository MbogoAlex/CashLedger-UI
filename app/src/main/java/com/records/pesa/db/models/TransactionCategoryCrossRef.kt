package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "transaction_category_cross_ref",
    primaryKeys = ["transactionId", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = Transaction::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class TransactionCategoryCrossRef(
    val transactionId: Int,
    val categoryId: Int
)
