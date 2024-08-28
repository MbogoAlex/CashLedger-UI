package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "transactionCategoryCrossRef",
    primaryKeys = ["transactionId", "categoryId"],
    foreignKeys = [
        ForeignKey(entity = Transaction::class, parentColumns = ["id"], childColumns = ["transactionId"], onDelete = ForeignKey.CASCADE),
        ForeignKey(entity = TransactionCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)
    ],
    indices = [
        Index(value = ["transactionId"]),
        Index(value = ["categoryId"]),
    ]
)
data class TransactionCategoryCrossRef(
    val transactionId: Int,
    val categoryId: Int
)

