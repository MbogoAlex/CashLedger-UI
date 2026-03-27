package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.BudgetCycleLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetCycleLogDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BudgetCycleLog)

    @Query("SELECT * FROM budget_cycle_log WHERE budgetId = :budgetId ORDER BY cycleNumber DESC")
    fun getLogsForBudget(budgetId: Int): Flow<List<BudgetCycleLog>>

    @Query("SELECT * FROM budget_cycle_log WHERE budgetId = :budgetId ORDER BY cycleNumber DESC")
    suspend fun getLogsForBudgetOnce(budgetId: Int): List<BudgetCycleLog>

    @Query("SELECT COUNT(*) FROM budget_cycle_log WHERE budgetId = :budgetId")
    suspend fun countLogsForBudget(budgetId: Int): Int

    @Query("DELETE FROM budget_cycle_log WHERE budgetId = :budgetId")
    suspend fun deleteLogsForBudget(budgetId: Int)

    @Query("SELECT * FROM budget_cycle_log ORDER BY budgetId, cycleNumber DESC")
    suspend fun getAllOnce(): List<BudgetCycleLog>
}
