package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.records.pesa.db.models.BudgetMember
import kotlinx.coroutines.flow.Flow

@Dao
interface BudgetMemberDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(member: BudgetMember): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(members: List<BudgetMember>)

    @Query("SELECT * FROM budget_member WHERE budgetId = :budgetId")
    fun getForBudget(budgetId: Int): Flow<List<BudgetMember>>

    @Query("SELECT * FROM budget_member WHERE budgetId = :budgetId")
    suspend fun getForBudgetOnce(budgetId: Int): List<BudgetMember>

    @Query("DELETE FROM budget_member WHERE budgetId = :budgetId")
    suspend fun deleteByBudget(budgetId: Int)

    @Query("SELECT * FROM budget_member")
    suspend fun getAllOnce(): List<BudgetMember>
}
