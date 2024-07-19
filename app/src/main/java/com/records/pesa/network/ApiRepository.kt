package com.records.pesa.network

import com.records.pesa.models.CategoriesResponseBody
import com.records.pesa.models.CategoryResponseBody
import com.records.pesa.models.CurrentBalanceResponseBody
import com.records.pesa.models.SortedTransactionsResponseBody
import com.records.pesa.models.TransactionResponseBody
import retrofit2.Response

interface ApiRepository {
    suspend fun getTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, latest: Boolean, startDate: String?, endDate: String?): Response<TransactionResponseBody>
    suspend fun getMoneyIn(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>
    suspend fun getMoneyOut(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>

    suspend fun getMoneyInSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>
    suspend fun getMoneyOutSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getCurrentBalance(userId: Int): Response<CurrentBalanceResponseBody>

    suspend fun getUserCategories(userId: Int, name: String?, orderBy: String?): Response<CategoriesResponseBody>

    suspend fun getCategoryDetails(categoryId: Int): Response<CategoryResponseBody>
}

class ApiRepositoryImpl(private val apiService: ApiService): ApiRepository {
    override suspend fun getTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: String?,
        endDate: String?
    ): Response<TransactionResponseBody> = apiService.getTransactions(userId, entity, categoryId, budgetId, transactionType, latest, startDate, endDate)

    override suspend fun getMoneyIn(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        latest: Boolean,
        startDate: String,
        endDate: String
    ): Response<TransactionResponseBody> = apiService.getMoneyIn(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyOut(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        latest: Boolean,
        startDate: String,
        endDate: String
    ): Response<TransactionResponseBody> = apiService.getMoneyOut(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyInSortedTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getMoneyInSortedTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        orderByAmount = orderByAmount,
        ascendingOrder = ascendingOrder,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getMoneyOutSortedTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: String,
        endDate: String
    ): Response<SortedTransactionsResponseBody> = apiService.getMoneyOutSortedTransactions(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        moneyIn = moneyIn,
        orderByAmount = orderByAmount,
        ascendingOrder = ascendingOrder,
        startDate = startDate,
        endDate = endDate
    )

    override suspend fun getCurrentBalance(userId: Int): Response<CurrentBalanceResponseBody> = apiService.getCurrentBalance(
        userId = userId
    )

    override suspend fun getUserCategories(
        userId: Int,
        name: String?,
        orderBy: String?
    ): Response<CategoriesResponseBody> = apiService.getUserCategories(
        userId = userId,
        name = name,
        orderBy = orderBy
    )

    override suspend fun getCategoryDetails(categoryId: Int): Response<CategoryResponseBody> = apiService.getCategoryDetails(
        categoryId = categoryId
    )

}