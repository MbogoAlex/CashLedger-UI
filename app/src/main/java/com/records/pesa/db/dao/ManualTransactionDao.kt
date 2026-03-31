package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.ManualTransaction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.LocalDateTime

@Dao
interface ManualTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: ManualTransaction): Long

    @Query("SELECT * FROM manual_transaction WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    fun getForCategory(categoryId: Int): Flow<List<ManualTransaction>>

    @Query("SELECT * FROM manual_transaction WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY date DESC, createdAt DESC")
    suspend fun getForCategoryOnce(categoryId: Int): List<ManualTransaction>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM manual_transaction WHERE categoryId = :categoryId AND isOutflow = 1 AND deletedAt IS NULL")
    suspend fun sumOutflowForCategory(categoryId: Int): Double

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM manual_transaction WHERE categoryId = :categoryId AND isOutflow = 1 AND date >= :startDate AND date <= :endDate AND deletedAt IS NULL")
    suspend fun sumOutflowForCategoryInPeriod(categoryId: Int, startDate: java.time.LocalDate, endDate: java.time.LocalDate): Double

    @Query("SELECT * FROM manual_transaction WHERE id = :id AND deletedAt IS NULL")
    fun getById(id: Int): Flow<ManualTransaction>

    @Query("SELECT * FROM manual_transaction WHERE deletedAt IS NULL")
    suspend fun getAllOnce(): List<ManualTransaction>

    @Query("UPDATE manual_transaction SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteById(id: Int, deletedAt: LocalDateTime)

    @Update
    suspend fun update(tx: ManualTransaction)

    @Query("DELETE FROM manual_transaction WHERE categoryId = :categoryId AND memberName = :memberName")
    suspend fun deleteByMemberAndCategory(categoryId: Int, memberName: String)

    @Query("UPDATE manual_transaction SET memberName = :newName WHERE categoryId = :categoryId AND memberName = :oldName")
    suspend fun updateMemberName(categoryId: Int, oldName: String, newName: String)

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM manual_transaction WHERE categoryId = :categoryId AND isOutflow = 1 AND date >= :startDate AND date <= :endDate AND memberName IN (:memberNames) AND deletedAt IS NULL")
    suspend fun sumOutflowForCategoryAndMembers(categoryId: Int, startDate: LocalDate, endDate: LocalDate, memberNames: List<String>): Double
}
