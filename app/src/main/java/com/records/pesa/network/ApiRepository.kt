package com.records.pesa.network

import com.records.pesa.models.TransactionResponseBody
import retrofit2.Response

interface ApiRepository {
    suspend fun getTransactions(userId: Int, entity: String?, categoryId: Int?, transactionType: String?, latest: Boolean, startDate: String, endDate: String): Response<TransactionResponseBody>
}

class ApiRepositoryImpl(private val apiService: ApiService): ApiRepository {
    override suspend fun getTransactions(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: String,
        endDate: String
    ): Response<TransactionResponseBody> = apiService.getTransactions(userId, entity, categoryId, transactionType, latest, startDate, endDate)

}