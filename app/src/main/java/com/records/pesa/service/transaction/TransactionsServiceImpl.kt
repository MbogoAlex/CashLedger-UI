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
import com.records.pesa.service.transaction.function.ReportGeneration
import com.records.pesa.service.transaction.function.TransactionsExtraction
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

class TransactionsServiceImpl(private val transactionsDao: TransactionsDao, private val categoryDao: CategoryDao): TransactionService {
    override fun extractTransactionDetails(
        messageDto: MessageData,
        userAccount: UserAccount,
        categories: List<TransactionCategory>,
    ): Transaction = TransactionsExtraction().extractTransactionDetails(
        messageDto,
        userAccount,
        transactionsDao,
        categories,
        categoryDao
    )

    override fun getTransactionsByEntity(entity: String): Flow<List<Transaction>> = transactionsDao.getTransactionByEntity(entity = entity)

    override fun getTransactionByCode(transactionCode: String): Flow<Transaction> = transactionsDao.getTransactionByCode(transactionCode)
    override fun getTransactionById(transactionId: Int): Flow<TransactionWithCategories> = transactionsDao.getTransactionById(transactionId)

    override fun getAllTransactions(): Flow<List<Transaction>> = transactionsDao.getAllTransactions()

    override fun getTransactionWithCategories(id: Int): Flow<TransactionWithCategories> = transactionsDao.getTransactionWithCategories(id)
    override fun getUserTransactions(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>> = transactionsDao.getUserTransactions(query)
    override fun getSortedTransactions(query: SupportSQLiteQuery): Flow<List<AggregatedTransaction>> = transactionsDao.getSortedTransactions(query)
    override fun getLatestTransactionCode(): Flow<String?> = transactionsDao.getLatestTransactionCode()
    override fun getFirstTransaction(): Flow<Transaction> = transactionsDao.getFirstTransaction()
    override fun createUserTransactionQuery(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery = transactionsDao.createUserTransactionQuery(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        latest = latest,
        moneyDirection = moneyDirection,
        startDate = startDate,
        endDate = endDate
    )

    override fun createSortedTransactionsQuery(
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        moneyIn: Boolean,
        orderByAmount: Boolean,
        ascendingOrder: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery = transactionsDao.createSortedTransactionsQuery(
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

    override suspend fun updateTransaction(transaction: Transaction) = transactionsDao.updateTransaction(transaction)
    override fun generateAllTransactionsReport(
        query: SupportSQLiteQuery,
        userAccount: UserAccount,
        reportType: String,
        startDate: String,
        endDate: String,
        context: Context
    ): ByteArray = ReportGeneration().generateAllTransactionsReport(
        query,
        transactionsDao,
        userAccount,
        reportType,
        startDate,
        endDate,
        context
    )

    override fun generateReportForTransactionsForMultipleCategories(
        query: SupportSQLiteQuery,
        userAccount: UserAccount,
        reportType: String,
        startDate: String,
        endDate: String,
        context: Context
    ): ByteArray = ReportGeneration().generateAllTransactionsReport(
        query,
        transactionsDao,
        userAccount,
        reportType,
        startDate,
        endDate,
        context
    )

    override fun createUserTransactionQueryForMultipleCategories(
        userId: Int,
        entity: String?,
        categoryIds: List<Int>?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery = transactionsDao.createUserTransactionQueryForMultipleCategories(
        userId = userId,
        entity = entity,
        categoryIds = categoryIds,
        budgetId = budgetId,
        transactionType = transactionType,
        latest = latest,
        startDate = startDate,
        endDate = endDate
    )

    override fun getTodayExpenditure(date: LocalDate): Flow<TodayExpenditure> = transactionsDao.getTodayExpenditure(date)
    override fun getCurrentBalance(): Flow<Double> = transactionsDao.getCurrentBalance()
    override fun createUserTransactionQueryByMonthAndYear(
        userId: Int,
        entity: String?,
        categoryId: Int?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        moneyDirection: String?,
        month: String,
        year: Int
    ): SupportSQLiteQuery = transactionsDao.createUserTransactionQueryByMonthAndYear(
        userId = userId,
        entity = entity,
        categoryId = categoryId,
        budgetId = budgetId,
        transactionType = transactionType,
        latest = latest,
        moneyDirection = moneyDirection,
        month = month,
        year = year
    )

    override fun getUserTransactionsFilteredByMonthAndYear(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>> = transactionsDao.getUserTransactionsFilteredByMonthAndYear(query)
    override suspend fun insertTransaction(transaction: Transaction): Long = transactionsDao.insertTransaction(transaction)

}