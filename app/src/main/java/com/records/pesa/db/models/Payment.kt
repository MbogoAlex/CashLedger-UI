package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime
import java.time.Month

@Entity(tableName = "payment", foreignKeys = [
    ForeignKey(entity = UserAccount::class, parentColumns = ["id"], childColumns = ["userId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
], indices = [
    Index(value = ["month"]),
    Index(value = ["paidAt"]),
    Index(value = ["expiredAt"]),
    Index(value = ["expiredAt"]),
    Index(value = ["userId"]),
])
data class Payment(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val month: Month,
    val paidAt: LocalDateTime,
    val expiredAt: LocalDateTime,
    val amount: Double,
    val freeTrialStartedOn: LocalDateTime?,
    val freeTrialEndedOn: LocalDateTime?,
    val userId: Int,
)
