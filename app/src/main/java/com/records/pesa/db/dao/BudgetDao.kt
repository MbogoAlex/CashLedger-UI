package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.Budget
import kotlinx.coroutines.flow.Flow
import java.time.LocalDateTime

@Dao
interface BudgetDao {
    @Query("UPDATE budget SET expenditure = :expenditure, limitReached = :limitReached, exceededBy = :exceededBy WHERE id = :id")
    suspend fun updateBudgetExpenditure(id: Int, expenditure: Double, limitReached: Boolean, exceededBy: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Query("UPDATE budget SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun deleteBudgetById(id: Int, deletedAt: LocalDateTime)

    @Query("UPDATE budget SET deletedAt = :deletedAt WHERE id = :id")
    suspend fun softDeleteBudget(id: Int, deletedAt: LocalDateTime)

    @Query("SELECT * FROM budget WHERE id = :id AND deletedAt IS NULL")
    fun getBudgetById(id: Int): Flow<Budget?>

    // One-shot lookup for restore merge (includes soft-deleted rows intentionally)
    @Query("SELECT * FROM budget WHERE id = :id LIMIT 1")
    suspend fun getBudgetByIdOnce(id: Int): Budget?

    @Query("SELECT * FROM budget WHERE categoryId = :categoryId AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getBudgetsByCategoryId(categoryId: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE active = 1 AND deletedAt IS NULL ORDER BY createdAt DESC")
    fun getActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE deletedAt IS NULL ORDER BY createdAt DESC")
    suspend fun getAllBudgetsOnce(): List<Budget>
}
