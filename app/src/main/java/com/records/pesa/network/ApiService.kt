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
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("transaction/{userId}")
    suspend fun getTransactions(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("latest") latest: Boolean,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?
    ): Response<TransactionResponseBody>

    @GET("transaction/outandin/{userId}")
    suspend fun getMoneyIn(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("latest") latest: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<TransactionResponseBody>

    @GET("transaction/outandin/{userId}")
    suspend fun getMoneyOut(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("latest") latest: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<TransactionResponseBody>

    @GET("transaction/sorted/{userId}")
    suspend fun getMoneyInSortedTransactions(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("orderByAmount") orderByAmount: Boolean,
        @Query("ascendingOrder") ascendingOrder: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<SortedTransactionsResponseBody>

    @GET("transaction/sorted/{userId}")
    suspend fun getMoneyOutSortedTransactions(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
        @Query("budgetId") budgetId: Int?,
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("orderByAmount") orderByAmount: Boolean,
        @Query("ascendingOrder") ascendingOrder: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<SortedTransactionsResponseBody>

    @GET("transaction/balance/{userId}")
    suspend fun getCurrentBalance(
        @Path("userId") userId: Int
    ): Response<CurrentBalanceResponseBody>

    @GET("category/{userId}")
    suspend fun getUserCategories(
        @Path("userId") userId: Int,
        @Query("name") name: String?,
        @Query("orderBy") orderBy: String?
    ): Response<CategoriesResponseBody>

    @GET("category/details/{categoryId}")
    suspend fun getCategoryDetails(
        @Path("categoryId") categoryId: Int
    ): Response<CategoryResponseBody>

    @POST("category/{userId}")
    suspend fun createCategory(
        @Path("userId") userId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/name/{categoryId}")
    suspend fun updateCategoryName(
        @Path("categoryId") categoryId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/{categoryId}")
    suspend fun addCategoryMembers(
        @Path("categoryId") categoryId: Int,
        @Body category: CategoryEditPayload
    ): Response<CategoryResponseBody>

    @PUT("category/keyword")
    suspend fun updateCategoryKeyword(
        @Body keyword: CategoryKeywordEditPayload
    ): Response<CategoryKeywordEditResponseBody>

    @DELETE("category/keyword/{categoryId}/{keywordId}")
    suspend fun deleteCategoryKeyword(
        @Path("categoryId") categoryId: Int,
        @Path("keywordId") keywordId: Int
    ): Response<CategoryKeywordDeleteResponseBody>

    @DELETE("category/{categoryId}")
    suspend fun deleteCategory(
        @Path("categoryId") categoryId: Int
    ): Response<CategoryDeleteResponseBody>
}