package com.records.pesa.service.category

import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.CategoryWithKeywords
import com.records.pesa.db.models.CategoryWithTransactions
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import kotlinx.coroutines.flow.Flow

class CategoryServiceImpl(private val categoryDao: CategoryDao): CategoryService {
    override suspend fun insertTransactionCategory(transactionCategory: TransactionCategory): Long = categoryDao.insertTransactionCategory(transactionCategory)
    override fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long = categoryDao.insertCategoryTransactionMapping(transactionCategoryCrossRef)
    override suspend fun updateCategoryKeyword(categoryKeyword: CategoryKeyword) = categoryDao.updateCategoryKeyword(categoryKeyword)

    override suspend fun insertCategoryKeyword(categoryKeyword: CategoryKeyword) = categoryDao.insertCategoryKeyword(categoryKeyword)

    override suspend fun updateCategory(transactionCategory: TransactionCategory) = categoryDao.updateCategory(transactionCategory)
    override fun getCategoryById(id: Int): Flow<CategoryWithKeywords> = categoryDao.getCategoryById(id)

    override fun getAllCategories(): Flow<List<CategoryWithTransactions>> = categoryDao.getAllCategories()
    override fun getRawCategoryById(id: Int): Flow<TransactionCategory> = categoryDao.getRawCategoryById(id)
    override fun getCategoryKeyword(id: Int): Flow<CategoryKeyword> = categoryDao.getCategoryKeyword(id)
}