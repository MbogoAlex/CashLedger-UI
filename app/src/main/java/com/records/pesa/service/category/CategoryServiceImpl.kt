package com.records.pesa.service.category

import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.models.TransactionCategory
import kotlinx.coroutines.flow.Flow

class CategoryServiceImpl(private val categoryDao: CategoryDao): CategoryService {
    override fun getAllCategories(): Flow<List<TransactionCategory>> = categoryDao.getAllCategories()
}