package com.records.pesa.network

import com.records.pesa.models.CategoriesResponseBody
import com.records.pesa.models.CategoryDeleteResponseBody
import com.records.pesa.models.CategoryEditPayload
import com.records.pesa.models.CategoryKeywordDeletePayload
import com.records.pesa.models.CategoryKeywordDeleteResponseBody
import com.records.pesa.models.CategoryKeywordEditPayload
import com.records.pesa.models.CategoryKeywordEditResponseBody
import com.records.pesa.models.CategoryResponseBody
import com.records.pesa.models.CurrentBalanceResponseBody
import com.records.pesa.models.SortedTransactionsResponseBody
import com.records.pesa.models.TransactionResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path

interface ApiRepository {
    suspend fun getTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, latest: Boolean, startDate: String?, endDate: String?): Response<TransactionResponseBody>
    suspend fun getMoneyIn(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>
    suspend fun getMoneyOut(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>

    suspend fun getMoneyInSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>
    suspend fun getMoneyOutSortedTransactions(userId: Int, entity: String?, categoryId: Int?, budgetId: Int?, transactionType: String?, moneyIn: Boolean, orderByAmount: Boolean, ascendingOrder: Boolean, startDate: String, endDate: String): Response<SortedTransactionsResponseBody>

    suspend fun getCurrentBalance(userId: Int): Response<CurrentBalanceResponseBody>

    suspend fun getUserCategories(userId: Int, name: String?, orderBy: String?): Response<CategoriesResponseBody>

    suspend fun getCategoryDetails(categoryId: Int): Response<CategoryResponseBody>
    suspend fun createCategory(userId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryName(categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun addCategoryMembers(categoryId: Int, category: CategoryEditPayload): Response<CategoryResponseBody>

    suspend fun updateCategoryKeyword(keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody>

    suspend fun deleteCategoryKeyword(categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody>

    suspend fun deleteCategory(categoryId: Int): Response<CategoryDeleteResponseBody>
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

    override suspend fun createCategory(
        userId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.createCategory(
        userId = userId,
        category = category
    )

    override suspend fun updateCategoryName(
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.updateCategoryName(
        categoryId = categoryId,
        category = category
    )

    override suspend fun addCategoryMembers(
        categoryId: Int,
        category: CategoryEditPayload
    ): Response<CategoryResponseBody> = apiService.addCategoryMembers(
        categoryId = categoryId,
        category = category
    )

    override suspend fun updateCategoryKeyword(keyword: CategoryKeywordEditPayload): Response<CategoryKeywordEditResponseBody> = apiService.updateCategoryKeyword(
        keyword = keyword
    )

    override suspend fun deleteCategoryKeyword(categoryId: Int, keywordId: Int): Response<CategoryKeywordDeleteResponseBody> = apiService.deleteCategoryKeyword(
        categoryId = categoryId,
        keywordId = keywordId
    )

    override suspend fun deleteCategory(categoryId: Int): Response<CategoryDeleteResponseBody> = apiService.deleteCategory(
        categoryId = categoryId
    )


}