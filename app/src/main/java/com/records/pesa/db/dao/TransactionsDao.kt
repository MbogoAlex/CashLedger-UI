package com.records.pesa.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.RawQuery
import androidx.room.Update
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.sqlite.db.SupportSQLiteQuery
import com.records.pesa.db.models.AggregatedTransaction
import com.records.pesa.db.models.Transaction
import com.records.pesa.db.models.TransactionCategory
import com.records.pesa.db.models.TransactionCategoryCrossRef
import com.records.pesa.db.models.TransactionWithCategories
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.runBlocking
import java.time.LocalDate

@Dao
interface TransactionsDao {
    @Insert
    suspend fun insertTransaction(transaction: Transaction): Long
    fun insertTransactionBlocking(transaction: Transaction): Long {
        return runBlocking {
            insertTransaction(transaction)
        }
    }

    @Query("select * from `transaction` where id = :id")
    fun getTransactionById(id: Int): Flow<TransactionWithCategories>

    @Query("select * from `transaction` where id = :id")
    fun getStaticTransactionById(id: Int): Transaction

    @Query("select * from `transaction` where transactionCode = :code")
    fun getTransactionByCode(code: String): Flow<Transaction>

    @Query("select * from `transaction`")
    fun getAllTransactions(): Flow<List<Transaction>>

    @Query("select * from `transaction` where entity = :entity")
    fun getTransactionByEntity(entity: String): Flow<List<Transaction>>

    @Query("select * from `transaction` where entity = :entity")
    fun getStaticTransactionByEntity(entity: String): List<Transaction>

    @Insert
    fun insertCategoryTransactionMapping(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long

    fun insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef: TransactionCategoryCrossRef): Long {
        return runBlocking {
            insertCategoryTransactionMappingRunBlocking(transactionCategoryCrossRef)
        }
    }

    @Query("select * from `transaction` where id = :transactionId")
    fun getTransactionWithCategories(transactionId: Int): Flow<TransactionWithCategories>

    @Update
    suspend fun updateTransaction(transaction: Transaction)

    @Query("select transactionCode from `transaction` order by date desc, time desc limit 1")
    fun getLatestTransactionCode(): Flow<String?>

    @Query("select balance from `transaction` order by date desc, time desc limit 1")
    fun getCurrentBalance(): Flow<Double>

    @Query("select * from `transaction` order by date asc limit 1")
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
    ): SupportSQLiteQuery {
        val orderClause = if (latest) "DESC" else "ASC"

        val query = StringBuilder().apply {
            append("SELECT t.* FROM `transaction` t ")
            append("LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId ")
            append("LEFT JOIN budget b ON b.categoryId = tc.categoryId ")
            append("WHERE t.userId = ? ") // Assuming `userId` is a column in `transaction`
            append("AND (? IS NULL OR ? = '' OR LOWER(t.sender) LIKE '%' || ? || '%' OR LOWER(t.nickName) LIKE '%' || ? || '%' OR LOWER(t.recipient) LIKE '%' || ? || '%') ")
            append("AND (? IS NULL OR tc.categoryId = ?) ")
            append("AND (? IS NULL OR b.id = ?) ")
            append("AND (? IS NULL OR ? = '' OR LOWER(t.transactionType) LIKE '%' || ? || '%') ")
            append("AND (t.date BETWEEN ? AND ?) ")
            if (!moneyDirection.isNullOrEmpty()) {
                if (moneyDirection.equals("in", ignoreCase = true)) {
                    append("AND t.transactionAmount > 0 ")
                } else {
                    append("AND t.transactionAmount <= 0 ")
                }
            }
            append("ORDER BY t.date $orderClause, t.time $orderClause")
        }

        return SimpleSQLiteQuery(
            query.toString(),
            arrayOf(userId, entity, entity, entity, entity, entity, categoryId, categoryId, budgetId, budgetId, transactionType, transactionType, transactionType, startDate.toString(), endDate.toString())
        )
    }


    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getUserTransactions(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>

    fun createUserTransactionQueryForMultipleCategories(
        userId: Int,
        entity: String?,
        categoryIds: List<Int>?,
        budgetId: Int?,
        transactionType: String?,
        latest: Boolean,
        startDate: LocalDate,
        endDate: LocalDate
    ): SupportSQLiteQuery {
        val orderClause = if (latest) "DESC" else "ASC"

        val query = StringBuilder().apply {
            append("SELECT t.* FROM `transaction` t ")
            append("LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId ")
            append("LEFT JOIN budget b ON b.categoryId = tc.categoryId ")
            append("WHERE t.userId = ? ") // Assuming `userId` is a column in `transaction`
            append("AND (? IS NULL OR (? = '' OR LOWER(t.sender) LIKE '%' || ? || '%' OR LOWER(t.nickName) LIKE '%' || ? || '%' OR LOWER(t.recipient) LIKE '%' || ? || '%')) ")

            if (!categoryIds.isNullOrEmpty()) {
                append("AND tc.categoryId IN (${categoryIds.joinToString(", ")}) ")
            }

            append("AND (? IS NULL OR b.id = ?) ")
            append("AND (? IS NULL OR LOWER(t.transactionType) = ?) ")
            append("AND (t.date >= ?) ")
            append("AND (t.date <= ?) ")
            append("ORDER BY t.date $orderClause, t.time $orderClause")
        }

        val argsList = mutableListOf<Any?>()
        argsList.add(userId)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)
        argsList.add(entity)

        if (categoryIds != null && categoryIds.isNotEmpty()) {
            // No need to add category IDs to args list; they are embedded directly in the query.
        }

        argsList.add(budgetId)
        argsList.add(budgetId)
        argsList.add(transactionType)
        argsList.add(transactionType)
        argsList.add(startDate.toString())
        argsList.add(endDate.toString())

        return SimpleSQLiteQuery(query.toString(), argsList.toTypedArray())
    }

    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getUserTransactionsForMultipleCategories(query: SupportSQLiteQuery): Flow<List<TransactionWithCategories>>


    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getStaticUserTransactions(query: SupportSQLiteQuery): List<TransactionWithCategories>

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
    ): SupportSQLiteQuery {
        val orderClause = if (ascendingOrder) "ASC" else "DESC"
        val amountClause = if (orderByAmount) "totalAmount" else "times"

        val query = StringBuilder().apply {
            append("""
            SELECT 
                CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END AS entity,
                t.nickName, 
                t.transactionType, 
                COUNT(ABS(t.transactionAmount)) AS times, 
                SUM(ABS(t.transactionAmount)) AS totalAmount, 
                SUM(ABS(t.transactionCost)) AS totalCost 
            FROM `transaction` t
            LEFT JOIN transactionCategoryCrossRef tc ON t.id = tc.transactionId
            LEFT JOIN budget b ON tc.categoryId = b.categoryId
            WHERE 
                (? IS NULL OR 
                LOWER(CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END) LIKE '%' || LOWER(?) || '%' OR 
                LOWER(t.nickName) LIKE '%' || LOWER(?) || '%') AND
                (? IS NULL OR tc.categoryId = ?) AND
                (? IS NULL OR b.id = ?) AND
                (? IS NULL OR LOWER(t.transactionType) = LOWER(?)) AND
                t.date BETWEEN ? AND ? AND
                ((? AND t.transactionAmount > 0) OR 
                (NOT ? AND t.transactionAmount < 0))
            GROUP BY 
                CASE 
                    WHEN ? THEN t.sender 
                    ELSE t.recipient 
                END,
                t.nickName, 
                t.transactionType
            ORDER BY 
                CASE WHEN ? THEN totalAmount ELSE times END 
                COLLATE NOCASE 
                $orderClause
        """)
        }

        return SimpleSQLiteQuery(
            query.toString(),
            arrayOf(
                moneyIn, moneyIn, entity, entity, entity,
                categoryId, categoryId,
                budgetId, budgetId,
                transactionType, transactionType,
                startDate.toString(), endDate.toString(),
                moneyIn, moneyIn
            )
        )
    }

    @RawQuery(observedEntities = [Transaction::class, TransactionCategory::class])
    fun getSortedTransactions(query: SupportSQLiteQuery): Flow<List<AggregatedTransaction>>


}