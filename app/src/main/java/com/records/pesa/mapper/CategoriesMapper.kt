package com.records.pesa.mapper

import com.records.pesa.db.models.Budget
import com.records.pesa.db.models.CategoryKeyword
import com.records.pesa.db.models.CategoryWithKeywords
import com.records.pesa.db.models.CategoryWithTransactions
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.models.CategoryBudget
import com.records.pesa.models.transaction.TransactionItem
import java.time.LocalDate
import java.time.LocalDateTime

fun CategoryWithKeywords.toTransactionCategory(): TransactionCategory = TransactionCategory(
    id = category.id,
    name = category.name,
    createdAt = category.createdAt,
    updatedAt = category.updatedAt,
    updatedTimes = category.updatedTimes,
    contains = category.contains
)

fun CategoryWithTransactions.toTransactionCategory(): TransactionCategory = TransactionCategory(
    id = category.id,
    name = category.name,
    createdAt = category.createdAt,
    updatedAt = category.updatedAt,
    updatedTimes = category.updatedTimes,
    contains = category.contains
)

fun CategoryWithTransactions.toResponseTransactionCategory(): com.records.pesa.models.TransactionCategory = com.records.pesa.models.TransactionCategory(
    id = category.id,
    name = category.name,
    createdAt = category.createdAt.toString(),
    transactions = transactions.map { it.toTransactionItem() },
    keywords = keyWords.map { it.toResponseCategoryKeyword() },
    budgets = budgets.map { it.toResponseCategoryBudget() }

)

fun CategoryWithKeywords.toResponseTransactionCategory(transactions: List<TransactionItem>): com.records.pesa.models.TransactionCategory = com.records.pesa.models.TransactionCategory(
    id = category.id,
    name = category.name,
    createdAt = category.createdAt.toString(),
    transactions = transactions,
    keywords = keywords.map { it.toResponseCategoryKeyword() },
    budgets = budgets.map { it.toResponseCategoryBudget() }

)

fun CategoryKeyword.toResponseCategoryKeyword(): com.records.pesa.models.CategoryKeyword = com.records.pesa.models.CategoryKeyword(
    id = id,
    keyWord = keyword,
    nickName = nickName
)

fun Budget.toResponseCategoryBudget(): com.records.pesa.models.CategoryBudget = com.records.pesa.models.CategoryBudget(
    id = id,
    name = name,
    budgetLimit = budgetLimit,
    createdAt = createdAt.toString(),
    limitDate = limitDate.toString(),
    limitReached = limitReached,
    exceededBy = exceededBy
)

fun com.records.pesa.models.TransactionCategory.toTransactionCategory(times: Double, contains: List<String>): TransactionCategory = TransactionCategory(
    id = id,
    name = name,
    createdAt = LocalDateTime.parse(createdAt),
    updatedAt = LocalDateTime.now(),
    updatedTimes = times,
    contains = contains

)

fun com.records.pesa.models.CategoryKeyword.toCategoryKeyword(categoryId: Int): CategoryKeyword = CategoryKeyword(
    id = id,
    keyword = keyWord,
    nickName = nickName,
    categoryId = categoryId
)