package com.records.pesa.db.models

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "chat_message")
data class ChatMessage(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val userId: Int,
    val role: String, // "user" | "model"
    val content: String,
    val attachmentName: String? = null,
    val attachmentType: String? = null, // "csv" | "pdf" | "image"
    val timestamp: Long = System.currentTimeMillis()
)
