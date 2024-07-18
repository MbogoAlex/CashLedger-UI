package com.records.pesa.network

import com.records.pesa.models.SortedTransactionsResponseBody
import com.records.pesa.models.TransactionResponseBody
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {
    @GET("transaction/{userId}")
    suspend fun getTransactions(
        @Path("userId") userId: Int,
        @Query("entity") entity: String?,
        @Query("categoryId") categoryId: Int?,
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
        @Query("transactionType") transactionType: String?,
        @Query("moneyIn") moneyIn: Boolean,
        @Query("orderByAmount") orderByAmount: Boolean,
        @Query("ascendingOrder") ascendingOrder: Boolean,
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<SortedTransactionsResponseBody>
}