package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.CategoryWithKeywords
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking

@Dao
interface CategoryDao {
    @Insert
    suspend fun insertTransactionCategory(transactionCategory: TransactionCategory): Long

    fun insertCategoryRunBlocking(transactionCategory: TransactionCategory): Long {
        return runBlocking {
            insertTransactionCategory(transactionCategory)
        }
    }

    @Update
    suspend fun updateCategory(transactionCategory: TransactionCategory): Int

    fun updateCategoryRunBlocking(transactionCategory: TransactionCategory): Int {
        return runBlocking {
            updateCategory(transactionCategory)
        }
    }

    @Query("select * from transactionCategory where id = :id")
    fun getCategoryById(id: Int): Flow<TransactionCategory>

    @Query("select * from transactionCategory")
    fun getAllCategories(): Flow<List<TransactionCategory>>

    @Query("select * from categoryKeyword where categoryId = :id")
    fun getStaticCategoryKeywords(id: Int): List<CategoryKeyword>

    @Insert
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long

    fun insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long {
        return runBlocking {
            insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef)
        }
    }
}