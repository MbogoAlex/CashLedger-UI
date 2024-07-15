package com.records.pesa.network

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
        @Query("startDate") startDate: String,
        @Query("endDate") endDate: String
    ): Response<TransactionResponseBody>
}