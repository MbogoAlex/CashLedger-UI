package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class CategoriesResponseBody(
    val statusCode: Int,
    val message: String,
    val data: CategoriesDt
)

@Serializable
data class CategoryResponseBody(
    val statusCode: Int,
    val message: String,
    val data: CategoryDt
)

@Serializable
data class CategoryDt(
    val category: TransactionCategory
)

@Serializable
data class CategoriesDt(
    val category: List<TransactionCategory>
)

@Serializable
data class TransactionCategory(
    val id: Int,
    val name: String,
    val createdAt: String?,
    val transactions: List<TransactionItem>,
    val keywords: List<CategoryKeyword>,
    val budgets: List<CategoryBudget>
)

@Serializable
data class CategoryKeyword(
    val id: Int,
    val keyWord: String,
    val nickName: String
)

@Serializable
data class CategoryBudget(
    val id: Int,
    val name: String?,
    val budgetLimit: Double,
    val createdAt: String,
    val limitDate: String,
    val limitReached: Boolean,
    val exceededBy: Double
)

