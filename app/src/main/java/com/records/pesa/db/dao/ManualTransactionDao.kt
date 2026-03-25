package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ManualTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: ManualTransaction): Long

    @Query("SELECT * FROM manual_transaction WHERE categoryId = :categoryId ORDER BY date DESC, createdAt DESC")
    fun getForCategory(categoryId: Int): Flow<List<ManualTransaction>>

    @Query("SELECT * FROM manual_transaction WHERE categoryId = :categoryId ORDER BY date DESC, createdAt DESC")
    suspend fun getForCategoryOnce(categoryId: Int): List<ManualTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM manual_transaction WHERE categoryId = :categoryId AND isOutflow = 1")
    suspend fun sumOutflowForCategory(categoryId: Int): Double

    @Query("SELECT * FROM manual_transaction")
    suspend fun getAllOnce(): List<ManualTransaction>

    @Query("DELETE FROM manual_transaction WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM manual_transaction WHERE categoryId = :categoryId AND memberName = :memberName")
    suspend fun deleteByMemberAndCategory(categoryId: Int, memberName: String)

    @Query("UPDATE manual_transaction SET memberName = :newName WHERE categoryId = :categoryId AND memberName = :oldName")
    suspend fun updateMemberName(categoryId: Int, oldName: String, newName: String)
}
