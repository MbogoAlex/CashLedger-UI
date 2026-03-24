package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ManualTransactionType
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualTransactionTypeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(type: ManualTransactionType): Long

    @Query("SELECT * FROM manual_transaction_type ORDER BY isCustom ASC, name ASC")
    fun getAll(): Flow<List<ManualTransactionType>>

    @Query("SELECT * FROM manual_transaction_type")
    suspend fun getAllOnce(): List<ManualTransactionType>

    @Query("DELETE FROM manual_transaction_type WHERE id = :id AND isCustom = 1")
    suspend fun deleteCustomById(id: Int)
}
