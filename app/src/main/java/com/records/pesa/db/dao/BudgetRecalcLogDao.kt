package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.BudgetRecalcLog
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetRecalcLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: BudgetRecalcLog)

    @Query("SELECT * FROM budget_recalc_log WHERE budgetId = :budgetId ORDER BY timestamp DESC")
    fun getLogsForBudget(budgetId: Int): Flow<List<BudgetRecalcLog>>

    @Query("SELECT * FROM budget_recalc_log ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 50): Flow<List<BudgetRecalcLog>>

    @Query("DELETE FROM budget_recalc_log WHERE budgetId = :budgetId")
    suspend fun deleteLogsForBudget(budgetId: Int)
}
