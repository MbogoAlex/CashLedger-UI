package com.records.pesa.service.category

import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.CategoryWithKeywords
import com.records.pesa.db.models.CategoryWithTransactions
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import kotlinx.coroutines.flow.Flow

interface CategoryService {
    suspend fun insertTransactionCategory(transactionCategory: TransactionCategory): Long
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long
    suspend fun updateCategoryKeyword(categoryKeyword: CategoryKeyword)
    suspend fun insertCategoryKeyword(categoryKeyword: CategoryKeyword)
    suspend fun updateCategory(transactionCategory: TransactionCategory)
    fun getCategoryById(id: Int): Flow<CategoryWithKeywords>
    fun getAllCategories(): Flow<List<CategoryWithTransactions>>
    fun getRawCategoryById(id: Int): Flow<TransactionCategory>
    fun getCategoryKeyword(id: Int): Flow<CategoryKeyword>
    fun getTransactionCategoryCrossRefs(): Flow<List<TransactionCategoryCrossRef>>
    fun getAllCategoryKeywords(): List<CategoryKeyword>
    suspend fun insertTransactionCategoryCrossRef(transactionCategoryCrossRef: TransactionCategoryCrossRef)
}