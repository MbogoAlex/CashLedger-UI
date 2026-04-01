package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ChatMessage
import kotlinx.coroutines.flow.Flow

@Dao
interface ChatMessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: ChatMessage): Long

    @Query("SELECT * FROM chat_message WHERE userId = :userId ORDER BY timestamp ASC")
    fun getMessagesForUser(userId: Int): Flow<List<ChatMessage>>

    @Query("SELECT * FROM chat_message WHERE userId = :userId ORDER BY timestamp ASC")
    suspend fun getMessagesForUserOnce(userId: Int): List<ChatMessage>

    @Query("DELETE FROM chat_message WHERE userId = :userId")
    suspend fun clearChatForUser(userId: Int)

    @Query("DELETE FROM chat_message WHERE id = :id")
    suspend fun deleteMessageById(id: Int)

    @Query("DELETE FROM chat_message")
    suspend fun clearAllMessages()
}
