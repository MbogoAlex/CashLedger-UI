package com.records.pesa.service.transaction

import androidx.sqlite.db.SupportSQLiteQuery
import com.records.pesa.db.dao.CategoryDao
import com.records.pesa.db.dao.TransactionsDao
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionWithCategories
import com.records.pesa.db.models.UserAccount
import com.records.pesa.models.MessageData
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface TransactionService {
    fun extractTransactionDetails(messageDto: MessageData, userAccount: UserAccount, categories: List<TransactionCategory>): Transaction
    fun getTransactionsByEntity(entity: String): Flow<List<Transaction>>
    fun getTransactionByCode(transactionCode: String): Flow<Transaction>
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
}