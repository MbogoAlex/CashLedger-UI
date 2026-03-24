package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.ManualBudgetTransaction
import kotlinx.coroutines.flow.Flow

@Dao
interface ManualBudgetTransactionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tx: ManualBudgetTransaction): Long

    @Query("SELECT * FROM manual_budget_transaction WHERE budgetId = :budgetId ORDER BY date DESC, createdAt DESC")
    fun getForBudget(budgetId: Int): Flow<List<ManualBudgetTransaction>>

    @Query("SELECT COALESCE(SUM(amount), 0.0) FROM manual_budget_transaction WHERE budgetId = :budgetId")
    suspend fun sumForBudget(budgetId: Int): Double

    @Query("SELECT * FROM manual_budget_transaction")
    suspend fun getAllOnce(): List<ManualBudgetTransaction>

    @Query("DELETE FROM manual_budget_transaction WHERE id = :id")
    suspend fun deleteById(id: Int)

    @Query("DELETE FROM manual_budget_transaction WHERE budgetId = :budgetId")
    suspend fun deleteAllForBudget(budgetId: Int)
}
