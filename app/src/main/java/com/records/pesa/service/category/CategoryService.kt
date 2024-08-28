package com.records.pesa.service.category

import com.records.pesa.db.models.TransactionCategory
import kotlinx.coroutines.flow.Flow

interface CategoryService {
    fun getAllCategories(): Flow<List<TransactionCategory>>
}