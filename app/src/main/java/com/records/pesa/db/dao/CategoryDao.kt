package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.CategoryWithKeywords
import com.records.pesa.db.models.CategoryWithTransactions
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
    @Insert
    fun insertCategoryKeyword(categoryKeyword: CategoryKeyword)

    fun insertCategoryKeywordRunBlocking(categoryKeyword: CategoryKeyword) {
        return runBlocking {
            insertCategoryKeyword(categoryKeyword)
        }
    }

    @Query("select * from categoryKeyword where id = :id")
    fun getCategoryKeyword(id: Int): Flow<CategoryKeyword>

    @Update
    suspend fun updateCategory(transactionCategory: TransactionCategory)

    @Update
    suspend fun updateCategoryKeyword(categoryKeyword: CategoryKeyword)

    fun updateCategoryRunBlocking(transactionCategory: TransactionCategory) {
        return runBlocking {
            updateCategory(transactionCategory)
        }
    }

    @Query("select * from transactionCategory where id = :id")
    fun getCategoryById(id: Int): Flow<CategoryWithKeywords>


    @Query("select * from transactionCategory where id = :id")
    fun getRawCategoryById(id: Int): Flow<TransactionCategory>

    @Query("select * from transactionCategory")
    fun getAllCategories(): Flow<List<CategoryWithTransactions>>

    @Query("select * from categoryKeyword where categoryId = :id")
    fun getStaticCategoryKeywords(id: Int): List<CategoryKeyword>

    @Insert
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long

    fun insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long {
        return runBlocking {
            insertCategoryTransactionMapping(transactionCategoryCrossRef)
        }
    }

//    @Query("select * from `transaction` where categoryId = :categoryId")
//    fun getTransactionsByCategoryId(categoryId: Int): Flow<Transaction>
}