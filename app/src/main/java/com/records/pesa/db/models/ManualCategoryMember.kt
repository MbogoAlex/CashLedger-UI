package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDateTime

@Entity(tableName = "manual_category_member", indices = [Index(value = ["categoryId"])])
data class ManualCategoryMember(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val categoryId: Int,
    val name: String,
    val createdAt: LocalDateTime,
    val deletedAt: LocalDateTime? = null
)
