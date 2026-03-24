package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.Budget
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetDao {
    @Query("UPDATE budget SET expenditure = :expenditure, limitReached = :limitReached, exceededBy = :exceededBy WHERE id = :id")
    suspend fun updateBudgetExpenditure(id: Int, expenditure: Double, limitReached: Boolean, exceededBy: Double)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBudget(budget: Budget): Long

    @Update
    suspend fun updateBudget(budget: Budget)

    @Delete
    suspend fun deleteBudget(budget: Budget)

    @Query("DELETE FROM budget WHERE id = :id")
    suspend fun deleteBudgetById(id: Int)

    @Query("SELECT * FROM budget WHERE id = :id")
    fun getBudgetById(id: Int): Flow<Budget?>

    @Query("SELECT * FROM budget WHERE categoryId = :categoryId ORDER BY createdAt DESC")
    fun getBudgetsByCategoryId(categoryId: Int): Flow<List<Budget>>

    @Query("SELECT * FROM budget WHERE active = 1 ORDER BY createdAt DESC")
    fun getActiveBudgets(): Flow<List<Budget>>

    @Query("SELECT * FROM budget ORDER BY createdAt DESC")
    fun getAllBudgets(): Flow<List<Budget>>
}
