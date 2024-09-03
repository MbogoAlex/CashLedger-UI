package com.records.pesa.service.transaction

import android.content.Context
import androidx.sqlite.db.SupportSQLiteQuery
import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.MessageData
import com.records.pesa.models.TodayExpenditure
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionService {
    fun extractTransactionDetails(messageDto: MessageData, userAccount: UserAccount, categories: List<TransactionCategory>): Transaction
    fun getTransactionsByEntity(entity: String): Flow<List<Transaction>>
    fun getTransactionByCode(transactionCode: String): Flow<Transaction>
    fun getTransactionById(transactionId: Int): Flow<TransactionWithCategories>
    fun getAllTransactions(): Flow<List<Transaction>>
    fun getTransactionWithCategories(id: Int): Flow<TransactionWithCategories>
    fun getUserTransactions(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>

    fun getSortedTransactions(query: SupportSQLiteQuery): Flow<List<AggregatedTransaction>>

    fun getLatestTransactionCode(): Flow<String?>

    fun getFirstTransaction(): Flow<Transaction>

    fun createUserTransactionQuery(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery

    fun createSortedTransactionsQuery(
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery

    suspend fun updateTransaction(transaction: Transaction)

    fun generateAllTransactionsReport(query: SupportSQLiteQuery, userAccount: UserAccount, reportType: String, startDate: String, endDate: String, context: Context): ByteArray

    fun generateReportForTransactionsForMultipleCategories(query: SupportSQLiteQuery, userAccount: UserAccount, reportType: String, startDate: String, endDate: String, context: Context): ByteArray
    fun createUserTransactionQueryForMultipleCategories(
        userId: Int,
        entity: String?,
        categoryIds: List<Int>?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery

    fun getTodayExpenditure(date: LocalDate): Flow<TodayExpenditure>
    fun getCurrentBalance(): Flow<Double>
    fun createUserTransactionQueryByMonthAndYear(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        month: String, // The month name (e.g., "January")
        year: Int // The year (e.g., 2024)
    ): SupportSQLiteQuery

    fun getUserTransactionsFilteredByMonthAndYear(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>
}