package com.records.pesa.db.models

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
    val id: Int = 0,
    val transactionCode: String,
    val transactionType: String,
    val transactionAmount: Double,
    val transactionCost: Double,
    val date: LocalDate,
    val time: LocalTime,
    val sender: String,
    val recipient: String,
    val nickName: String?,
    val comment: String?,
    val balance: Double,
    val entity: String,
    val userId: Int,
)
