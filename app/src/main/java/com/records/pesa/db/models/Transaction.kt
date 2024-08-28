package com.records.pesa.db.models

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate
import java.time.LocalTime

@Entity(tableName = "transaction", foreignKeys = [
    ForeignKey(entity = UserAccount::class, parentColumns = ["id"], childColumns = ["userId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
], indices = [
    Index(value = ["transactionCode"], unique = true),
    Index(value = ["entity"]),
    Index(value = ["transactionType"]),
    Index(value = ["date"]),
    Index(value = ["time"]),
    Index(value = ["sender"]),
    Index(value = ["recipient"]),
    Index(value = ["userId"]),
])
data class Transaction(
    @PrimaryKey(autoGenerate = true)
    @ColumnInfo(name = "id")
    var id: Int = 0,
    var transactionCode: String,
    var transactionType: String,
    var transactionAmount: Double,
    var transactionCost: Double,
    var date: LocalDate,
    var time: LocalTime,
    var sender: String,
    var recipient: String,
    var nickName: String?,
    var comment: String?,
    var balance: Double,
    var entity: String,
    var userId: Int,
)
