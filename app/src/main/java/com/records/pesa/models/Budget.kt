package com.records.pesa.models

import kotlinx.serialization.Serializable

@Serializable
data class BudgetEditPayLoad(
    val name: String,
    val budgetLimit: Double,
    val limitDate: String,
    val limitExceeded: Boolean
)
@Serializable
data class SingleBudgetResponseBody(
    val statusCode: Int,
    val message: String,
    val data: SingleBudgetData
)
@Serializable
data class BudgetResponseBody(
    val statusCode: Int,
    val message: String,
    val data: BudgetData
)
@Serializable
data class SingleBudgetData(
    val budget: BudgetDt
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

@Serializable
data class BudgetDeleteResponseBody(
    val statusCode: Int,
    val message: String,
    val data: BudgetDlt
)

@Serializable
data class BudgetDlt(
    val budget: String
)
