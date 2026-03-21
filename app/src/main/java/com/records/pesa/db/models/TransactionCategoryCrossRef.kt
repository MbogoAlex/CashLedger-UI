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
    var id: Int? = 0,
    var transactionId: Int = 0,
    var categoryId: Int = 0
)

