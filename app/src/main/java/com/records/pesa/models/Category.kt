package com.records.pesa.models

import androidx.compose.ui.layout.IntrinsicMeasurable
import kotlinx.serialization.Serializable

@Serializable
data class CategoryEditPayload(
    val userId: Int,
    val categoryName: String,
    val keywords: List<String>
)

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

@Serializable
data class CategoryKeywordEditPayload(
    val id: Int,
    val keyWord: String,
    val nickName: String
)

@Serializable
data class CategoryKeywordEditResponseBody(
    val statusCode: Int,
    val message: String,
    val data: CategoryKeywordDt
)

@Serializable
data class CategoryKeywordDt(
    val keyword: KeywordDt
)

@Serializable
data class KeywordDt(
    val id: Int,
    val keyWord: String,
    val nickName: String
)

@Serializable
data class CategoryKeywordDeletePayload(
    val categoryId: Int,
    val keywordId: Int
)

@Serializable
data class CategoryKeywordDeleteResponseBody(
    val statusCode: Int,
    val message: String,
    val data: KeywordDlt
)

@Serializable
data class KeywordDlt(
    val keyword: String
)

@Serializable
data class CategoryDeleteResponseBody(
    val statusCode: Int,
    val message: String,
    val data: CategoryDlt
)

@Serializable
data class CategoryDlt(
    val category: String
)

