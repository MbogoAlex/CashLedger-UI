package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetResponseBody(
    val statusCode: Int,
    val message: String,
    val data: BudgetData
)

@Serializable
data class BudgetData(
    val budget: List<BudgetDt>
)

@Serializable
data class BudgetDt(
    val id: Int,
    val name: String?,
    val active: Boolean,
    val expenditure: Double,
    val budgetLimit: Double,
    val createdAt: String,
    val limitDate: String,
    val limitReached: Boolean,
    val limitReachedAt: String?,
    val exceededBy: Double,
    val category: BudgetCategory,
    val user: BudgetOwner
)

@Serializable
data class BudgetCategory(
    val id: Int,
    val name: String
)

@Serializable
data class BudgetOwner(
    val id: Int,
    val name: String
)
