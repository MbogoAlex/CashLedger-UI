package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "categoryKeyword", foreignKeys = [
    ForeignKey(entity = TransactionCategory::class, parentColumns = ["id"], childColumns = ["categoryId"], onUpdate = ForeignKey.CASCADE, onDelete = ForeignKey.CASCADE),
], indices = [
    Index(value = ["categoryId"])
])
data class CategoryKeyword(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    var keyword: String,
    var nickName: String?,
    var categoryId: Int,
)
